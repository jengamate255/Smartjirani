import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient, SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const GROQ_API_KEY = Deno.env.get("GROQ_API_KEY") ?? "";
const GROQ_MODEL = Deno.env.get("GROQ_MODEL") ?? "openai/gpt-oss-120b";
const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY") ?? "";
const GEMINI_MODEL = Deno.env.get("GEMINI_MODEL") ?? "gemini-2.0-flash";
const ASSISTANT_DAILY_LIMIT = 50;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
    status,
  });
}

// ------------------------------------------------------------
// Record an AI metric via the ai_record_metric RPC.
// Fire-and-forget: never throws, never blocks the response.
// ------------------------------------------------------------
function recordMetric(
  service: ReturnType<typeof createClient>,
  params: {
    feature: string;
    provider: string;
    model: string;
    mock: boolean;
    latencyMs: number;
    success: boolean;
    errorMessage?: string;
    errorCode?: string;
    userId: string;
    organizationId?: string | null;
    reportId?: string | null;
    metadata?: Record<string, unknown>;
    action?: string | null;
  },
) {
  service
    .rpc("ai_record_metric", {
      p_feature: params.feature,
      p_provider: params.provider,
      p_model: params.model,
      p_mock: params.mock,
      p_latency_ms: params.latencyMs,
      p_success: params.success,
      p_error_message: params.errorMessage ?? null,
      p_error_code: params.errorCode ?? null,
      p_user_id: params.userId,
      p_organization_id: params.organizationId ?? null,
      p_report_id: params.reportId ?? null,
      p_metadata: params.metadata ?? {},
      p_action: params.action ?? null,
    })
    .then(() => {})
    .catch(() => {});
}

// ------------------------------------------------------------
// Gemini REST call. Throws on failure so callers can fall back.
// ------------------------------------------------------------
async function gemini(prompt: string, system: string): Promise<string> {
  const res = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${GEMINI_API_KEY}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: system }] },
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.2, maxOutputTokens: 600 },
      }),
    },
  );
  if (!res.ok) throw new Error(`gemini_http_${res.status}`);
  const data = await res.json();
  const parts = data?.candidates?.[0]?.content?.parts ?? [];
  const text = parts.map((p: { text?: string }) => p.text ?? "").join("").trim();
  if (!text) throw new Error("gemini_empty");
  return text;
}

// ------------------------------------------------------------
// Gemini embedding — returns a 1536-dim float array.
// Used by the dedup action to generate embeddings on the fly.
// ------------------------------------------------------------
async function geminiEmbedding(text: string): Promise<number[]> {
  const res = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=${GEMINI_API_KEY}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        model: "models/text-embedding-004",
        content: { parts: [{ text }] },
      }),
    },
  );
  if (!res.ok) throw new Error(`gemini_embed_http_${res.status}`);
  const data = await res.json();
  const values = data?.embedding?.values;
  if (!Array.isArray(values)) throw new Error("gemini_embed_empty");
  return values;
}

// ------------------------------------------------------------
// Groq REST call (OpenAI-compatible). Throws on failure.
// ------------------------------------------------------------
async function groq(prompt: string, system: string): Promise<string> {
  const res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${GROQ_API_KEY}`,
    },
    body: JSON.stringify({
      model: GROQ_MODEL,
      messages: [
        { role: "system", content: system },
        { role: "user", content: prompt },
      ],
      temperature: 0.2,
      max_tokens: 600,
    }),
  });
  if (!res.ok) throw new Error(`groq_http_${res.status}`);
  const data = await res.json();
  const text = data?.choices?.[0]?.message?.content ?? "";
  if (!text.trim()) throw new Error("groq_empty");
  return text.trim();
}

// ------------------------------------------------------------
// Groq Whisper transcription. Accepts audio bytes, returns text.
// ------------------------------------------------------------
async function groqTranscribe(audioBytes: Uint8Array, filename: string): Promise<string> {
  const blob = new Blob([audioBytes], { type: "audio/mpeg" });
  const form = new FormData();
  form.append("file", blob, filename);
  form.append("model", "whisper-large-v3-turbo");
  form.append("response_format", "text");

  const res = await fetch("https://api.groq.com/openai/v1/audio/transcriptions", {
    method: "POST",
    headers: { Authorization: `Bearer ${GROQ_API_KEY}` },
    body: form,
  });
  if (!res.ok) throw new Error(`whisper_http_${res.status}`);
  const text = await res.text();
  if (!text.trim()) throw new Error("whisper_empty");
  return text.trim();
}

// ------------------------------------------------------------
// Offline keyword mock — Swahili-first. Used whenever GEMINI_API_KEY
// is absent or the live call fails, so the feature never hard-fails.
// ------------------------------------------------------------
const MOCK_RULES: Array<{ keys: string[]; en: string }> = [
  { keys: ["taka", "takataka", "vuede", "garbage", "waste", "trash"], en: "Waste" },
  { keys: ["mfereji", "majarazi", "maji yakuzama", "drain", "flood"], en: "Drainage" },
  { keys: ["maji", "bombani", "water", "hakuna maji"], en: "Water" },
  { keys: ["umeme", "stima", "electric", "power", "kibeni"], en: "Electricity" },
  { keys: ["taa", "giza", "street light", "mwanga"], en: "Street Lighting" },
  { keys: ["wizi", "uhalifu", "usalama", "crime", "security"], en: "Public Safety" },
  { keys: ["miti", "mazingira", "environment", "uchafu"], en: "Environment" },
  { keys: ["kelele", "sauti", "noise", "muziki"], en: "Noise" },
  { keys: ["daraja", "kioo", "public facilit", "kitengo cha umma"], en: "Public Facilities" },
  { keys: ["foleni", "traffic", "tepshi", "daladala", "magari"], en: "Traffic" },
  { keys: ["barabara", "shimo", "mashimo", "road", "kabrasha"], en: "Roads" },
  { keys: ["afya", "hospitali", "health", "kliniki"], en: "Health" },
  { keys: ["shule", "elimu", "school", "education"], en: "Education" },
  { keys: ["sherehe", "maonyesho", "event", "community"], en: "Community Events" },
];

const CRITICAL_KEYS = ["moto", "fire", "ajali", "kufa", "dharura", "mlipuko"];
const HIGH_KEYS = ["hatari", "tishio", "haraka", "urgent", "usalama", "majeruhi"];

function mockTriage(title: string, description: string, categories: Array<{ id: string; en: string; sw: string }>) {
  const text = `${title} ${description}`.toLowerCase();
  let matched: { id: string; en: string; sw: string } | null = null;
  for (const rule of MOCK_RULES) {
    if (rule.keys.some((k) => text.includes(k))) {
      const cat = categories.find((c) => c.en.toLowerCase() === rule.en.toLowerCase());
      if (cat) {
        matched = cat;
        break;
      }
    }
  }
  matched ??= categories.find((c) => c.en.toLowerCase() === "other") ?? categories[0] ?? null;

  let severity = "MEDIUM";
  if (CRITICAL_KEYS.some((k) => text.includes(k))) severity = "CRITICAL";
  else if (HIGH_KEYS.some((k) => text.includes(k))) severity = "HIGH";

  const confidence = matched ? 0.35 : 0.1;
  const summary = matched
    ? `AI (hali ya majaribio): tatizo linaainishwa chini ya "${matched.sw}" na umuhimu "${severity}".`
    : "AI (hali ya majaribio): haiwezi kubaini kipengee kwa uhakika.";

  return { categoryId: matched?.id ?? null, severity, confidence, summary };
}

const SELF_ID =
  "Mimi ni Msaidizi wa AI wa SmartJirani — si mtu, wala mfanyikazi wa manispaa.";

function mockAssistant(
  message: string,
  context: {
    is_official?: boolean;
    roles?: Array<{ role: string }>;
    my_recent_reports?: Array<{ reference: string; status: string; title: string }>;
    org_stats?: Array<{ open_reports: number; overdue: number }>;
  },
  searchResults: Array<{ reference: string; status: string; title: string | null }>,
): string {
  const m = message.toLowerCase();
  const lines: string[] = [];

  if (/(habari|jambo|shikamoo|salama|hello|hi\b|mambo)/.test(m)) {
    lines.push(`Habari! ${SELF_ID}`);
    lines.push(
      "Naweza kukusaidia: kutafuta hali ya ripoti zako, kukueleza jinsi ya kuripoti tatizo, na (kwa viongozi) kutoa muhtasari wa ripoti zilizo wazi.",
    );
    return lines.join("\n");
  }

  if (searchResults.length > 0) {
    lines.push(`${SELF_ID} Hii hapa niliyoiona kuhusu ulizo lako:`);
    for (const r of searchResults.slice(0, 3)) {
      lines.push(`• ${r.reference} — hali: ${r.status}${r.title ? ` — "${r.title}"` : ""}`);
    }
    return lines.join("\n");
  }

  if (/(ripoti|taarifa|report)/.test(m) && /(vipi|namna|jinsi|how|nitete|nawezaje)/.test(m)) {
    lines.push(`${SELF_ID}`);
    lines.push(
      "Kuripoti tatizo: fungua kichupo cha 'Civic' → Bonyeza 'Ripoti Tatizo' → chagua kipengee, andika maelezo, ongeza picha na mahali. Ripoti yako itathibitishwa na viongozi wa mtaa kisha itapeanwa idara husika.",
    );
    return lines.join("\n");
  }

  if (/(ripoti yangu|hali ya|status)/.test(m)) {
    const mine = context.my_recent_reports ?? [];
    if (mine.length > 0) {
      lines.push(`${SELF_ID} Hali ya ripoti zako za hivi karibuni:`);
      for (const r of mine.slice(0, 3)) {
        lines.push(`• ${r.reference} — ${r.status}${r.title ? ` — "${r.title}"` : ""}`);
      }
    } else {
      lines.push(`${SELF_ID} Hauna ripoti yoyote kwa sasa.`);
    }
    return lines.join("\n");
  }

  if (context.is_official && context.org_stats && context.org_stats.length > 0) {
    const stat = context.org_stats[0];
    lines.push(`${SELF_ID} Muhtasari wa eneo lako: ripoti ${stat.open_reports} zilizo wazi, ${stat.overdue} zimevuka muda.`);
    lines.push("Tumia dashibodi ya viongozi kuona mfululizo kamili.");
    return lines.join("\n");
  }

  lines.push(`${SELF_ID}`);
  lines.push(
    "Naweza kukusaidia kutafuta hali ya ripoti zako au kukueleza jinsi ya kuripoti tatizo. Jaribu: 'Hali ya ripoti yangu' au 'Jinsi ya kuripoti'.",
  );
  return lines.join("\n");
}

function mockDedup(title: string, description: string, categoryEn: string | null) {
  return {
    duplicates: [] as Array<{ report_id: string; title: string; similarity: number }>,
    method: "keyword-mock" as const,
    message: "Hakuna njia ya embedding iliyosanidiwa. Ripoti zinahitaji embeddings kwa utambuzi wa nakala.",
    categoryFilter: categoryEn,
  };
}

function mockAnalyze(title: string, description: string) {
  const text = `${title} ${description}`.toLowerCase();
  let severity = "LOW";
  if (CRITICAL_KEYS.some((k) => text.includes(k))) severity = "CRITICAL";
  else if (HIGH_KEYS.some((k) => text.includes(k))) severity = "HIGH";
  else if (["haraka", "muda mrefu", "zaidi ya wiki"].some((k) => text.includes(k))) severity = "MEDIUM";

  return {
    affectedArea: "Eneo la mtaa (haijabainishwa)",
    populationImpact: "Haijakadiriwa",
    suggestedActions: ["Kagua eneo", "Thibitisha ripoti", "Panga zamu ya ukarabati"],
    urgencyFactors: severity === "CRITICAL" ? ["Dharura — hatari ya maisha"] : ["Ripoti ya kawaida"],
    infrastructureType: "Miundombinu ya umma",
    severityEstimate: severity,
  };
}

function mockRoute(categoryEn: string | null, severity: string | null) {
  const deptMap: Record<string, string> = {
    Waste: "Idara ya Usafi na Mazingira",
    Drainage: "Idara ya Maji na Maji ya Taka",
    Water: "Idara ya Maji",
    Electricity: "Idara ya Umeme",
    "Street Lighting": "Idara ya Umeme",
    "Public Safety": "Idara ya Usalama",
    Environment: "Idara ya Mazingira",
    Noise: "Idara ya Mazingira",
    "Public Facilities": "Idara ya Miundombinu",
    Traffic: "Idara ya Uchukuzi",
    Roads: "Idara ya Uhandisi wa Barabara",
    Health: "Idara ya Afya",
    Education: "Idara ya Elimu",
    "Community Events": "Idara ya Jamii",
  };
  const dept = categoryEn ? deptMap[categoryEn] ?? "Idara ya Huduma za Jamii" : "Idara ya Huduma za Jamii";
  return {
    suggestedDepartment: dept,
    routingReason: `Kipengee: ${categoryEn ?? "haijulikani"}. Njia ya majaribio.`,
    priorityRecommendation: severity ?? "MEDIUM",
    slaHours: severity === "CRITICAL" ? 4 : severity === "HIGH" ? 24 : severity === "LOW" ? 168 : 72,
  };
}

const ASSISTANT_SYSTEM = `Wewe ni "Msaidizi wa SmartJirani" — msaidizi wa AI wa mfumo wa huduma za manispaa (civic) wa SmartJirani.
Kanuni za lazima:
1. Jibu kwa Kiswahili kwanza; tuweke Kiingereza ukiombwa.
2. Wewe ni AI, si mtu wala mfanyikazi wa manispaa. Ukiulizwa, tambua wazi kuwa wewe ni msaidizi wa AI.
3. Usiwahi kutaja au kubuni taarifa za watumiaji wengine. Tumia TOLEO la muktadha uliotolewapi pekee.
4. Usiwahi ahidi utendaji (mf. "nitarekebisha barabara"). Eleza tu hali ya mambo na hatua zinazofuata.
5. Kama taarifa haipo kwenye muktadha, sema wazi kuwa hujui na muongoze mtumiaji kwa hatua sahihi.`;

const ANALYZE_SYSTEM = `Wewe ni mchambuzi wa ripoti za manispaa Tanzania.
Jibu JSON tupu wenye funguo hizi:
- affectedArea (text): eneo linaloathirika
- populationImpact (text): makadirio ya watu walioathirika
- suggestedActions (array ya text): hatua zilizopendekezwa
- urgencyFactors (array ya text): sababu za dharura
- infrastructureType (text): aina ya miundombinu
- severityEstimate (LOW|MEDIUM|HIGH|CRITICAL): makadirio ya umuhimu`;

const ROUTE_SYSTEM = `Wewe ni mbinu wa uelekezaji wa ripoti za manispaa Tanzania.
Jibu JSON tupu wenye funguo hizi:
- suggestedDepartment (text): jina la idara inayopendekezwa kwa Kiswahili
- routingReason (text): sababu ya kuelekeza hapa
- priorityRecommendation (LOW|MEDIUM|HIGH|CRITICAL): umuhimu uliopendekezwa
- slaHours (number): muda wa SLA kwa masaa`;

function extractJson(text: string): Record<string, unknown> | null {
  const fenced = text.replace(/```json/gi, "").replace(/```/g, "").trim();
  const start = fenced.indexOf("{");
  const end = fenced.lastIndexOf("}");
  if (start === -1 || end === -1) return null;
  try {
    return JSON.parse(fenced.slice(start, end + 1));
  } catch {
    return null;
  }
}

function extractJsonArray(text: string): unknown[] | null {
  const fenced = text.replace(/```json/gi, "").replace(/```/g, "").trim();
  const start = fenced.indexOf("[");
  const end = fenced.lastIndexOf("]");
  if (start === -1 || end === -1) return null;
  try {
    return JSON.parse(fenced.slice(start, end + 1));
  } catch {
    return null;
  }
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const started = Date.now();
  const service = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
    auth: { persistSession: false },
  });

  const authHeader = req.headers.get("Authorization") ?? "";
  if (!authHeader.toLowerCase().startsWith("bearer ")) {
    return json({ error: "unauthorized" }, 401);
  }
  const userClient: SupabaseClient = createClient(SUPABASE_URL, ANON_KEY, {
    auth: { persistSession: false },
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userErr } = await userClient.auth.getUser();
  const user = userData?.user;
  if (userErr || !user) return json({ error: "unauthorized" }, 401);

  let body: {
    action?: string;
    reportId?: string;
    message?: string;
    organizationId?: string;
    weekStart?: string;
    audioBase64?: string;
    audioFilename?: string;
    threshold?: number;
  };
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid_json" }, 400);
  }
  const action = body.action;
  const validActions = ["triage", "assistant", "summarize", "dedup", "analyze", "transcribe", "insights", "route"];
  if (!action || !validActions.includes(action)) {
    return json({ error: "unknown_action" }, 400);
  }

  try {
    // ----------------------------------------------------------
    // TRIAGE — classify a report (category, severity, summary),
    // persist ai fields, then attempt NEW -> AI_TRIAGED as SYSTEM.
    // ----------------------------------------------------------
    if (action === "triage") {
      const reportId = body.reportId;
      if (!reportId) return json({ error: "missing_reportId" }, 400);

      const { data: rep, error: repErr } = await service
        .from("civic_reports")
        .select("id, organization_id, title, description, status, ai_summary, public_reference")
        .eq("id", reportId)
        .maybeSingle();
      if (repErr) throw repErr;
      if (!rep) return json({ error: "report_not_found" }, 404);

      const { data: isMine, error: mineErr } = await userClient
        .from("civic_reports")
        .select("id")
        .eq("id", reportId)
        .eq("reporter_id", user.id)
        .maybeSingle();
      if (mineErr) throw mineErr;
      let permitted = Boolean(isMine);
      if (!permitted) {
        const { data: canManage, error: cmErr } = await userClient.rpc(
          "civic_can_manage_report",
          { p_report_id: reportId },
        );
        if (cmErr) throw cmErr;
        permitted = canManage === true;
      }
      if (!permitted) return json({ error: "forbidden" }, 403);

      const { data: cats, error: catErr } = await service
        .from("civic_categories")
        .select("id, name_en, name_sw")
        .eq("organization_id", rep.organization_id)
        .eq("is_active", true)
        .order("sort_order");
      if (catErr) throw catErr;

      const catList = (cats ?? []).map((c: { id: string; name_en: string; name_sw: string }) => ({
        id: c.id,
        en: c.name_en,
        sw: c.name_sw,
      }));

      let provider = "groq";
      let mock = false;
      let classification: { categoryId: string | null; severity: string; confidence: number; summary: string };

      const catNames = catList.map((c) => `${c.en} / ${c.sw}`).join("; ");
      const triagePrompt =
        `Andika majibu kama JSON tupu wenye funguo hizi tu: category (moja kati ya: ${catNames}), ` +
        `severity (moja ya LOW|MEDIUM|HIGH|CRITICAL), confidence (namba 0.0-1.0), summary (sentensi moja Kiswahili).\n\n` +
        `Kichwa: ${rep.title}\nMaelezo: ${rep.description}`;
      const triageSystem = "Wewe ni mbinu wa uainishaji wa malalamiko ya manispaa Tanzania. Jibu JSON tupu tu.";

      if (GROQ_API_KEY && catList.length > 0) {
        try {
          const raw = await groq(triagePrompt, triageSystem);
          const parsed = extractJson(raw);
          if (!parsed) throw new Error("groq_parse");
          const catText = String(parsed.category ?? "");
          const cat = catList.find(
            (c) => c.en.toLowerCase() === catText.toLowerCase() || c.sw.toLowerCase() === catText.toLowerCase(),
          );
          const sev = String(parsed.severity ?? "MEDIUM").toUpperCase();
          classification = {
            categoryId: cat?.id ?? null,
            severity: ["LOW", "MEDIUM", "HIGH", "CRITICAL"].includes(sev) ? sev : "MEDIUM",
            confidence: Math.max(0, Math.min(1, Number(parsed.confidence ?? 0.3))),
            summary: String(parsed.summary ?? "").slice(0, 500) || "Hakuna muhtasari.",
          };
        } catch {
          if (GEMINI_API_KEY) {
            try {
              provider = "gemini";
              const raw = await gemini(triagePrompt, triageSystem);
              const parsed = extractJson(raw);
              if (!parsed) throw new Error("gemini_parse");
              const catText = String(parsed.category ?? "");
              const cat = catList.find(
                (c) => c.en.toLowerCase() === catText.toLowerCase() || c.sw.toLowerCase() === catText.toLowerCase(),
              );
              const sev = String(parsed.severity ?? "MEDIUM").toUpperCase();
              classification = {
                categoryId: cat?.id ?? null,
                severity: ["LOW", "MEDIUM", "HIGH", "CRITICAL"].includes(sev) ? sev : "MEDIUM",
                confidence: Math.max(0, Math.min(1, Number(parsed.confidence ?? 0.3))),
                summary: String(parsed.summary ?? "").slice(0, 500) || "Hakuna muhtasari.",
              };
            } catch {
              provider = "mock";
              mock = true;
              classification = mockTriage(rep.title, rep.description, catList);
            }
          } else {
            provider = "mock";
            mock = true;
            classification = mockTriage(rep.title, rep.description, catList);
          }
        }
      } else if (GEMINI_API_KEY && catList.length > 0) {
        try {
          provider = "gemini";
          const raw = await gemini(triagePrompt, triageSystem);
          const parsed = extractJson(raw);
          if (!parsed) throw new Error("gemini_parse");
          const catText = String(parsed.category ?? "");
          const cat = catList.find(
            (c) => c.en.toLowerCase() === catText.toLowerCase() || c.sw.toLowerCase() === catText.toLowerCase(),
          );
          const sev = String(parsed.severity ?? "MEDIUM").toUpperCase();
          classification = {
            categoryId: cat?.id ?? null,
            severity: ["LOW", "MEDIUM", "HIGH", "CRITICAL"].includes(sev) ? sev : "MEDIUM",
            confidence: Math.max(0, Math.min(1, Number(parsed.confidence ?? 0.3))),
            summary: String(parsed.summary ?? "").slice(0, 500) || "Hakuna muhtasari.",
          };
        } catch {
          provider = "mock";
          mock = true;
          classification = mockTriage(rep.title, rep.description, catList);
        }
      } else {
        provider = "mock";
        mock = true;
        classification = mockTriage(rep.title, rep.description, catList);
      }

      const updates: Record<string, unknown> = {
        ai_summary: classification.summary,
        ai_confidence: classification.confidence,
      };
      const { error: updErr } = await service.from("civic_reports").update(updates).eq("id", reportId);
      if (updErr) throw updErr;

      let transitioned = false;
      if (rep.status === "NEW" && classification.categoryId) {
        const { data: tr, error: trErr } = await service.rpc("transition_report", {
          p_report_id: reportId,
          p_to_status: "AI_TRIAGED",
          p_reason: "AI triage (kiotomatiki)",
          p_note: { provider, mock, confidence: classification.confidence },
        });
        transitioned = !trErr;
      }

      const usedModel = mock ? "keyword-mock" : provider === "groq" ? GROQ_MODEL : GEMINI_MODEL;
      await service.from("ai_comms").insert({
        user_id: user.id,
        organization_id: rep.organization_id,
        kind: "TRIAGE",
        prompt_summary: rep.title.slice(0, 200),
        response_summary: classification.summary,
        provider,
        model: usedModel,
        mock,
        latency_ms: Date.now() - started,
      });

      recordMetric(service, {
        feature: "triage",
        provider,
        model: usedModel,
        mock,
        latencyMs: Date.now() - started,
        success: true,
        userId: user.id,
        organizationId: rep.organization_id,
        reportId,
        metadata: { confidence: classification.confidence, transitioned },
      });

      return json({
        ok: true,
        action: "triage",
        provider,
        mock,
        model: usedModel,
        categoryId: classification.categoryId,
        severity: classification.severity,
        confidence: classification.confidence,
        summary: classification.summary,
        transitioned,
        latencyMs: Date.now() - started,
      });
    }

    // ----------------------------------------------------------
    // ASSISTANT — user-scoped context + search via the user's JWT,
    // then a Swahili-first reply. Read-only by design.
    // ----------------------------------------------------------
    if (action === "assistant") {
      const message = (body.message ?? "").trim();
      if (message.length === 0) return json({ error: "missing_message" }, 400);
      if (message.length > 2000) return json({ error: "message_too_long" }, 400);

      const dayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
      const { count, error: cntErr } = await service
        .from("ai_comms")
        .select("id", { count: "exact", head: true })
        .eq("user_id", user.id)
        .eq("kind", "ASSISTANT")
        .gte("created_at", dayAgo);
      if (cntErr) throw cntErr;
      if ((count ?? 0) >= ASSISTANT_DAILY_LIMIT) {
        return json({ error: "assistant_rate_limited", limit: ASSISTANT_DAILY_LIMIT }, 429);
      }

      const { data: context, error: ctxErr } = await userClient.rpc("civic_assistant_context");
      if (ctxErr) throw ctxErr;

      const { data: searchData, error: searchErr } = await userClient.rpc("civic_assistant_search", {
        p_query: message,
      });
      if (searchErr) throw searchErr;
      const searchResults: Array<{ reference: string; status: string; title: string | null }> =
        (searchData?.results as typeof searchResults | undefined) ?? [];

      let provider = "groq";
      let mock = false;
      let reply: string;

      const contextBlock = JSON.stringify({ context, search: searchResults });
      const assistantPrompt = `Muktadha wa mtumiaji (pekee unaruhusiwa kutumia):\n${contextBlock}\n\nUjumbe wa mtumiaji: ${message}`;

      if (GROQ_API_KEY) {
        try {
          reply = await groq(assistantPrompt, ASSISTANT_SYSTEM);
        } catch {
          if (GEMINI_API_KEY) {
            try {
              provider = "gemini";
              reply = await gemini(assistantPrompt, ASSISTANT_SYSTEM);
            } catch {
              provider = "mock";
              mock = true;
              reply = mockAssistant(message, context ?? {}, searchResults);
            }
          } else {
            provider = "mock";
            mock = true;
            reply = mockAssistant(message, context ?? {}, searchResults);
          }
        }
      } else if (GEMINI_API_KEY) {
        try {
          provider = "gemini";
          reply = await gemini(assistantPrompt, ASSISTANT_SYSTEM);
        } catch {
          provider = "mock";
          mock = true;
          reply = mockAssistant(message, context ?? {}, searchResults);
        }
      } else {
        provider = "mock";
        mock = true;
        reply = mockAssistant(message, context ?? {}, searchResults);
      }

      const roles = (context?.roles as Array<{ organization_id: string }> | undefined) ?? [];
      const usedModel = mock ? "keyword-mock" : provider === "groq" ? GROQ_MODEL : GEMINI_MODEL;
      await service.from("ai_comms").insert({
        user_id: user.id,
        organization_id: roles[0]?.organization_id ?? null,
        kind: "ASSISTANT",
        prompt_summary: message.slice(0, 200),
        response_summary: reply.slice(0, 500),
        provider,
        model: usedModel,
        mock,
        latency_ms: Date.now() - started,
      });

      recordMetric(service, {
        feature: "assistant",
        provider,
        model: usedModel,
        mock,
        latencyMs: Date.now() - started,
        success: true,
        userId: user.id,
        organizationId: roles[0]?.organization_id ?? null,
        metadata: { messageLength: message.length },
      });

      return json({
        ok: true,
        action: "assistant",
        provider,
        mock,
        model: usedModel,
        reply,
        latencyMs: Date.now() - started,
      });
    }

    // ----------------------------------------------------------
    // DEDUP — find semantic duplicates using pgvector embeddings.
    // If the report lacks embeddings, tries to generate via Gemini.
    // Falls back to keyword-based mock when no embedding API exists.
    // ----------------------------------------------------------
    if (action === "dedup") {
      const reportId = body.reportId;
      if (!reportId) return json({ error: "missing_reportId" }, 400);
      const threshold = body.threshold ?? 0.85;

      const { data: rep, error: repErr } = await service
        .from("civic_reports")
        .select("id, organization_id, title, description, status, category_id, title_embedding, public_reference")
        .eq("id", reportId)
        .maybeSingle();
      if (repErr) throw repErr;
      if (!rep) return json({ error: "report_not_found" }, 404);

      const { data: isMine, error: mineErr } = await userClient
        .from("civic_reports")
        .select("id")
        .eq("id", reportId)
        .eq("reporter_id", user.id)
        .maybeSingle();
      if (mineErr) throw mineErr;
      let permitted = Boolean(isMine);
      if (!permitted) {
        const { data: canManage, error: cmErr } = await userClient.rpc(
          "civic_can_manage_report",
          { p_report_id: reportId },
        );
        if (cmErr) throw cmErr;
        permitted = canManage === true;
      }
      if (!permitted) return json({ error: "forbidden" }, 403);

      let provider = "mock";
      let mock = true;
      let duplicates: Array<{ report_id: string; title: string; similarity: number; status: string }> = [];

      if (rep.title_embedding) {
        const { data: similar, error: simErr } = await service.rpc("civic_find_similar_reports", {
          p_report_id: reportId,
          p_threshold: threshold,
          p_limit: 10,
        });
        if (!simErr && similar) {
          duplicates = (similar as Array<{ report_id: string; title: string; similarity: number; status: string }>).map(
            (s) => ({ report_id: s.report_id, title: s.title, similarity: Math.round(s.similarity * 100) / 100, status: s.status }),
          );
          provider = "pgvector";
          mock = false;
        }
      } else if (GEMINI_API_KEY) {
        try {
          const embedText = `${rep.title} ${rep.description ?? ""}`.slice(0, 2000);
          const embedding = await geminiEmbedding(embedText);
          const vecStr = `[${embedding.join(",")}]`;

          await service
            .from("civic_reports")
            .update({ title_embedding: vecStr, embedding_generated_at: new Date().toISOString() })
            .eq("id", reportId);

          const { data: similar, error: simErr } = await service.rpc("civic_find_similar_reports", {
            p_report_id: reportId,
            p_threshold: threshold,
            p_limit: 10,
          });
          if (!simErr && similar) {
            duplicates = (similar as Array<{ report_id: string; title: string; similarity: number; status: string }>).map(
              (s) => ({ report_id: s.report_id, title: s.title, similarity: Math.round(s.similarity * 100) / 100, status: s.status }),
            );
          }
          provider = "gemini";
          mock = false;
        } catch {
          const catEn = rep.category_id ? null : null;
          const mockResult = mockDedup(rep.title, rep.description ?? "", catEn);
          duplicates = [];
          provider = mockResult.method;
          mock = true;
        }
      } else {
        const mockResult = mockDedup(rep.title, rep.description ?? "", null);
        duplicates = [];
        provider = mockResult.method;
        mock = true;
      }

      const usedModel = mock ? "keyword-mock" : provider === "gemini" ? "text-embedding-004" : "pgvector";
      await service.from("ai_comms").insert({
        user_id: user.id,
        organization_id: rep.organization_id,
        kind: "DEDUP",
        prompt_summary: rep.public_reference ?? rep.title.slice(0, 200),
        response_summary: `${duplicates.length} duplicates found`,
        provider,
        model: usedModel,
        mock,
        latency_ms: Date.now() - started,
      });

      recordMetric(service, {
        feature: "dedup",
        provider,
        model: usedModel,
        mock,
        latencyMs: Date.now() - started,
        success: true,
        userId: user.id,
        organizationId: rep.organization_id,
        reportId,
        metadata: { duplicatesFound: duplicates.length, threshold },
      });

      return json({
        ok: true,
        action: "dedup",
        provider,
        mock,
        model: usedModel,
        duplicates,
        duplicateCount: duplicates.length,
        threshold,
        latencyMs: Date.now() - started,
      });
    }

    // ----------------------------------------------------------
    // ANALYZE — deep content analysis of a report. Returns
    // structured insights: affected area, population, actions.
    // ----------------------------------------------------------
    if (action === "analyze") {
      const reportId = body.reportId;
      if (!reportId) return json({ error: "missing_reportId" }, 400);

      const { data: rep, error: repErr } = await service
        .from("civic_reports")
        .select("id, organization_id, title, description, status, severity, public_reference")
        .eq("id", reportId)
        .maybeSingle();
      if (repErr) throw repErr;
      if (!rep) return json({ error: "report_not_found" }, 404);

      const { data: isMine, error: mineErr } = await userClient
        .from("civic_reports")
        .select("id")
        .eq("id", reportId)
        .eq("reporter_id", user.id)
        .maybeSingle();
      if (mineErr) throw mineErr;
      let permitted = Boolean(isMine);
      if (!permitted) {
        const { data: canManage, error: cmErr } = await userClient.rpc(
          "civic_can_manage_report",
          { p_report_id: reportId },
        );
        if (cmErr) throw cmErr;
        permitted = canManage === true;
      }
      if (!permitted) return json({ error: "forbidden" }, 403);

      let provider = "groq";
      let mock = false;
      let analysis: Record<string, unknown>;

      const analyzePrompt =
        `Chambua ripoti hii ya manispaa na utoe JSON:\n\n` +
        `Kichwa: ${rep.title}\nMaelezo: ${rep.description ?? "(hakuna)"}\n` +
        `Hali: ${rep.status}, Umuhimu: ${rep.severity ?? "haijabainishwa"}`;

      if (GROQ_API_KEY) {
        try {
          const raw = await groq(analyzePrompt, ANALYZE_SYSTEM);
          const parsed = extractJson(raw);
          if (!parsed) throw new Error("groq_parse");
          analysis = parsed;
        } catch {
          if (GEMINI_API_KEY) {
            try {
              provider = "gemini";
              const raw = await gemini(analyzePrompt, ANALYZE_SYSTEM);
              const parsed = extractJson(raw);
              if (!parsed) throw new Error("gemini_parse");
              analysis = parsed;
            } catch {
              provider = "mock";
              mock = true;
              analysis = mockAnalyze(rep.title, rep.description ?? "");
            }
          } else {
            provider = "mock";
            mock = true;
            analysis = mockAnalyze(rep.title, rep.description ?? "");
          }
        }
      } else if (GEMINI_API_KEY) {
        try {
          provider = "gemini";
          const raw = await gemini(analyzePrompt, ANALYZE_SYSTEM);
          const parsed = extractJson(raw);
          if (!parsed) throw new Error("gemini_parse");
          analysis = parsed;
        } catch {
          provider = "mock";
          mock = true;
          analysis = mockAnalyze(rep.title, rep.description ?? "");
        }
      } else {
        provider = "mock";
        mock = true;
        analysis = mockAnalyze(rep.title, rep.description ?? "");
      }

      const usedModel = mock ? "keyword-mock" : provider === "groq" ? GROQ_MODEL : GEMINI_MODEL;
      await service.from("ai_comms").insert({
        user_id: user.id,
        organization_id: rep.organization_id,
        kind: "ANALYZE",
        prompt_summary: rep.public_reference ?? rep.title.slice(0, 200),
        response_summary: JSON.stringify(analysis).slice(0, 500),
        provider,
        model: usedModel,
        mock,
        latency_ms: Date.now() - started,
      });

      recordMetric(service, {
        feature: "analyze",
        provider,
        model: usedModel,
        mock,
        latencyMs: Date.now() - started,
        success: true,
        userId: user.id,
        organizationId: rep.organization_id,
        reportId,
      });

      return json({
        ok: true,
        action: "analyze",
        provider,
        mock,
        model: usedModel,
        analysis,
        latencyMs: Date.now() - started,
      });
    }

    // ----------------------------------------------------------
    // TRANSCRIBE — convert audio to text via Groq Whisper.
    // Accepts base64-encoded audio in the request body.
    // ----------------------------------------------------------
    if (action === "transcribe") {
      const audioBase64 = body.audioBase64;
      if (!audioBase64) return json({ error: "missing_audioBase64" }, 400);
      if (audioBase64.length > 5 * 1024 * 1024) return json({ error: "audio_too_large", maxBytes: 5 * 1024 * 1024 }, 413);

      const filename = body.audioFilename ?? "audio.mp3";
      let provider = "groq";
      let mock = false;
      let transcript: string;

      if (GROQ_API_KEY) {
        try {
          const audioBytes = Uint8Array.from(atob(audioBase64), (c) => c.charCodeAt(0));
          transcript = await groqTranscribe(audioBytes, filename);
        } catch (err) {
          provider = "mock";
          mock = true;
          transcript = `[Transcription haijafanikiwa: ${err instanceof Error ? err.message : "error"}]`;
        }
      } else {
        provider = "mock";
        mock = true;
        transcript = "[Hakuna API ya transcription iliyosanidiwa. Sanidi GROQ_API_KEY kwa Whisper.]";
      }

      const usedModel = mock ? "mock" : "whisper-large-v3-turbo";
      await service.from("ai_comms").insert({
        user_id: user.id,
        organization_id: null,
        kind: "TRANSCRIBE",
        prompt_summary: filename.slice(0, 200),
        response_summary: transcript.slice(0, 500),
        provider,
        model: usedModel,
        mock,
        latency_ms: Date.now() - started,
      });

      recordMetric(service, {
        feature: "transcribe",
        provider,
        model: usedModel,
        mock,
        latencyMs: Date.now() - started,
        success: !mock,
        errorMessage: mock ? "No Groq API key or transcription failed" : null,
        userId: user.id,
        metadata: { filename, audioSizeBytes: audioBase64.length },
      });

      return json({
        ok: true,
        action: "transcribe",
        provider,
        mock,
        model: usedModel,
        transcript,
        latencyMs: Date.now() - started,
      });
    }

    // ----------------------------------------------------------
    // INSIGHTS — generate weekly AI insights for officials.
    // Uses civic_weekly_insights table + AI to summarize trends.
    // ----------------------------------------------------------
    if (action === "insights") {
      const organizationId = body.organizationId;
      if (!organizationId) return json({ error: "missing_organizationId" }, 400);

      const weekStart = body.weekStart;
      if (!weekStart) return json({ error: "missing_weekStart" }, 400);

      const { data: context, error: ctxErr } = await userClient.rpc("civic_assistant_context");
      if (ctxErr) throw ctxErr;
      const isOfficial = (context as { is_official?: boolean })?.is_official === true;
      if (!isOfficial) return json({ error: "forbidden" }, 403);

      const { data: insightId, error: genErr } = await service.rpc("civic_generate_weekly_insights", {
        p_organization_id: organizationId,
        p_week_start: weekStart,
      });
      if (genErr) throw genErr;

      const weekEndDate = new Date(weekStart);
      weekEndDate.setDate(weekEndDate.getDate() + 6);
      const weekEndStr = weekEndDate.toISOString().split("T")[0];

      const { data: recentReports, error: repErr } = await service
        .from("civic_reports")
        .select("id, title, description, status, severity, created_at, public_reference")
        .eq("organization_id", organizationId)
        .gte("created_at", weekStart)
        .lte("created_at", weekEndStr + "T23:59:59")
        .order("created_at", { ascending: false })
        .limit(50);
      if (repErr) throw repErr;

      const reports = recentReports ?? [];
      const totalReports = reports.length;
      const resolvedReports = reports.filter((r: { status: string }) =>
        ["RESOLVED", "CLOSED"].includes(r.status)
      ).length;
      const avgResolutionHours = resolvedReports > 0
        ? Math.round((reports.filter((r: { status: string }) => ["RESOLVED", "CLOSED"].includes(r.status)).length / totalReports) * 48 * 100) / 100
        : null;
      const resolutionRate = totalReports > 0
        ? Math.round((resolvedReports / totalReports) * 1000) / 1000
        : null;

      let provider = "groq";
      let mock = false;
      let summaryText: string;
      let topConcerns: unknown[] = [];
      let emergingIssues: unknown[] = [];

      const reportSummaries = reports
        .slice(0, 20)
        .map(
          (r: { public_reference: string | null; title: string; status: string; severity: string | null }) =>
            `${r.public_reference ?? "?"} "${r.title}" — ${r.status}${r.severity ? ` (${r.severity})` : ""}`,
        )
        .join("\n");

      const insightsPrompt =
        `Ripoti ${totalReports} za wiki ${weekStart} hadi ${weekEndStr}. Zilizotatuliwa: ${resolvedReports}.\n\n` +
        `Ripoti:\n${reportSummaries || "(hakuna)"}\n\n` +
        `Andika JSON wenye funguo:\n` +
        `- summary (text): muhtasari wa wiki kwa Kiswahili (sentensi 3-5)\n` +
        `- topConcerns (array): [{category, count, trend}] — mambo 3 ya juu\n` +
        `- emergingIssues (array): [{description, severity}] — matatizo mapya yanayoonekana`;
      const insightsSystem = "Wewe ni mchambuzi wa data za manispaa Tanzania. Jibu JSON tupu tu.";

      if (GROQ_API_KEY && reports.length > 0) {
        try {
          const raw = await groq(insightsPrompt, insightsSystem);
          const parsed = extractJson(raw);
          if (!parsed) throw new Error("groq_parse");
          summaryText = String(parsed.summary ?? `Wiki ya ${weekStart}: ripoti ${totalReports}, ${resolvedReports} zimetatuliwa.`);
          topConcerns = (parsed.topConcerns as unknown[]) ?? [];
          emergingIssues = (parsed.emergingIssues as unknown[]) ?? [];
        } catch {
          if (GEMINI_API_KEY) {
            try {
              provider = "gemini";
              const raw = await gemini(insightsPrompt, insightsSystem);
              const parsed = extractJson(raw);
              if (!parsed) throw new Error("gemini_parse");
              summaryText = String(parsed.summary ?? `Wiki ya ${weekStart}: ripoti ${totalReports}, ${resolvedReports} zimetatuliwa.`);
              topConcerns = (parsed.topConcerns as unknown[]) ?? [];
              emergingIssues = (parsed.emergingIssues as unknown[]) ?? [];
            } catch {
              provider = "mock";
              mock = true;
              summaryText = `Muhtasari (hali ya majaribio): Wiki ya ${weekStart} — ripoti ${totalReports}, ${resolvedReports} zimetatuliwa.`;
            }
          } else {
            provider = "mock";
            mock = true;
            summaryText = `Muhtasari (hali ya majaribio): Wiki ya ${weekStart} — ripoti ${totalReports}, ${resolvedReports} zimetatuliwa.`;
          }
        }
      } else if (GEMINI_API_KEY && reports.length > 0) {
        try {
          provider = "gemini";
          const raw = await gemini(insightsPrompt, insightsSystem);
          const parsed = extractJson(raw);
          if (!parsed) throw new Error("gemini_parse");
          summaryText = String(parsed.summary ?? `Wiki ya ${weekStart}: ripoti ${totalReports}, ${resolvedReports} zimetatuliwa.`);
          topConcerns = (parsed.topConcerns as unknown[]) ?? [];
          emergingIssues = (parsed.emergingIssues as unknown[]) ?? [];
        } catch {
          provider = "mock";
          mock = true;
          summaryText = `Muhtasari (hali ya majaribio): Wiki ya ${weekStart} — ripoti ${totalReports}, ${resolvedReports} zimetatuliwa.`;
        }
      } else {
        provider = "mock";
        mock = true;
        summaryText = `Muhtasari (hali ya majaribio): Wiki ya ${weekStart} — ripoti ${totalReports}, ${resolvedReports} zimetatuliwa. Hakuna data ya kutosha kwa uchambuzi wa kina.`;
      }

      if (insightId) {
        await service.rpc("civic_update_weekly_insights", {
          p_insight_id: insightId,
          p_summary_text: summaryText,
          p_top_concerns: topConcerns,
          p_emerging_issues: emergingIssues,
          p_hotspots: [],
          p_total_reports: totalReports,
          p_resolved_reports: resolvedReports,
          p_avg_resolution_hours: avgResolutionHours,
          p_resolution_rate: resolutionRate,
          p_provider: provider,
          p_model: mock ? "keyword-mock" : provider === "groq" ? GROQ_MODEL : GEMINI_MODEL,
          p_latency_ms: Date.now() - started,
          p_input_tokens: null,
          p_output_tokens: null,
        });
      }

      const usedModel = mock ? "keyword-mock" : provider === "groq" ? GROQ_MODEL : GEMINI_MODEL;
      await service.from("ai_comms").insert({
        user_id: user.id,
        organization_id: organizationId,
        kind: "INSIGHTS",
        prompt_summary: `week:${weekStart}`,
        response_summary: summaryText.slice(0, 500),
        provider,
        model: usedModel,
        mock,
        latency_ms: Date.now() - started,
      });

      recordMetric(service, {
        feature: "insights",
        provider,
        model: usedModel,
        mock,
        latencyMs: Date.now() - started,
        success: true,
        userId: user.id,
        organizationId,
        metadata: { weekStart, totalReports, resolvedReports, insightId },
      });

      return json({
        ok: true,
        action: "insights",
        provider,
        mock,
        model: usedModel,
        insightId,
        summary: summaryText,
        topConcerns,
        emergingIssues,
        stats: { totalReports, resolvedReports, avgResolutionHours, resolutionRate },
        latencyMs: Date.now() - started,
      });
    }

    // ----------------------------------------------------------
    // ROUTE — smart report routing to the appropriate department.
    // Uses AI to match report content to department responsibility.
    // ----------------------------------------------------------
    if (action === "route") {
      const reportId = body.reportId;
      if (!reportId) return json({ error: "missing_reportId" }, 400);

      const { data: rep, error: repErr } = await service
        .from("civic_reports")
        .select("id, organization_id, title, description, status, severity, category_id, public_reference")
        .eq("id", reportId)
        .maybeSingle();
      if (repErr) throw repErr;
      if (!rep) return json({ error: "report_not_found" }, 404);

      const { data: canManage, error: cmErr } = await userClient.rpc("civic_can_manage_report", {
        p_report_id: reportId,
      });
      if (cmErr) throw cmErr;
      if (canManage !== true) return json({ error: "forbidden" }, 403);

      let categoryEn: string | null = null;
      if (rep.category_id) {
        const { data: cat, error: catErr } = await service
          .from("civic_categories")
          .select("name_en")
          .eq("id", rep.category_id)
          .maybeSingle();
        if (!catErr && cat) categoryEn = cat.name_en;
      }

      let provider = "groq";
      let mock = false;
      let routing: Record<string, unknown>;

      const routePrompt =
        `Elekeza ripoti hii kwa idara sahihi:\n\n` +
        `Kichwa: ${rep.title}\nMaelezo: ${rep.description ?? "(hakuna)"}\n` +
        `Kipengee: ${categoryEn ?? "haijabainishwa"}\nUmuhimu: ${rep.severity ?? "haijabainishwa"}`;

      if (GROQ_API_KEY) {
        try {
          const raw = await groq(routePrompt, ROUTE_SYSTEM);
          const parsed = extractJson(raw);
          if (!parsed) throw new Error("groq_parse");
          routing = parsed;
        } catch {
          if (GEMINI_API_KEY) {
            try {
              provider = "gemini";
              const raw = await gemini(routePrompt, ROUTE_SYSTEM);
              const parsed = extractJson(raw);
              if (!parsed) throw new Error("gemini_parse");
              routing = parsed;
            } catch {
              provider = "mock";
              mock = true;
              routing = mockRoute(categoryEn, rep.severity);
            }
          } else {
            provider = "mock";
            mock = true;
            routing = mockRoute(categoryEn, rep.severity);
          }
        }
      } else if (GEMINI_API_KEY) {
        try {
          provider = "gemini";
          const raw = await gemini(routePrompt, ROUTE_SYSTEM);
          const parsed = extractJson(raw);
          if (!parsed) throw new Error("gemini_parse");
          routing = parsed;
        } catch {
          provider = "mock";
          mock = true;
          routing = mockRoute(categoryEn, rep.severity);
        }
      } else {
        provider = "mock";
        mock = true;
        routing = mockRoute(categoryEn, rep.severity);
      }

      const usedModel = mock ? "keyword-mock" : provider === "groq" ? GROQ_MODEL : GEMINI_MODEL;
      await service.from("ai_comms").insert({
        user_id: user.id,
        organization_id: rep.organization_id,
        kind: "ROUTE",
        prompt_summary: rep.public_reference ?? rep.title.slice(0, 200),
        response_summary: JSON.stringify(routing).slice(0, 500),
        provider,
        model: usedModel,
        mock,
        latency_ms: Date.now() - started,
      });

      recordMetric(service, {
        feature: "route",
        provider,
        model: usedModel,
        mock,
        latencyMs: Date.now() - started,
        success: true,
        userId: user.id,
        organizationId: rep.organization_id,
        reportId,
        metadata: { categoryEn, suggestedDepartment: routing.suggestedDepartment },
      });

      return json({
        ok: true,
        action: "route",
        provider,
        mock,
        model: usedModel,
        routing,
        latencyMs: Date.now() - started,
      });
    }

    // ----------------------------------------------------------
    // SUMMARIZE — officials in scope only. Produces a Swahili
    // timeline summary from the immutable status history.
    // ----------------------------------------------------------
    const reportId = body.reportId;
    if (!reportId) return json({ error: "missing_reportId" }, 400);

    const { data: canManage, error: cmErr } = await userClient.rpc("civic_can_manage_report", {
      p_report_id: reportId,
    });
    if (cmErr) throw cmErr;
    if (canManage !== true) return json({ error: "forbidden" }, 403);

    const { data: rep, error: repErr } = await service
      .from("civic_reports")
      .select(
        "id, organization_id, public_reference, title, description, status, severity, priority, due_at, escalation_level, report_count",
      )
      .eq("id", reportId)
      .maybeSingle();
    if (repErr) throw repErr;
    if (!rep) return json({ error: "report_not_found" }, 404);

    const { data: history, error: histErr } = await service
      .from("civic_report_status_history")
      .select("from_status, to_status, changed_by_role, reason, created_at")
      .eq("report_id", reportId)
      .order("created_at", { ascending: true });
    if (histErr) throw histErr;

    let provider = "groq";
    let mock = false;
    let summary: string;

    const histText = (history ?? [])
      .map(
        (h: { from_status: string | null; to_status: string; changed_by_role: string | null; created_at: string }) =>
          `${h.created_at}: ${h.from_status ?? "-"} -> ${h.to_status}${h.changed_by_role ? ` (${h.changed_by_role})` : ""}`,
      )
      .join("\n");

    const summarizePrompt =
      `Ripoti ${rep.public_reference} "${rep.title}" — hali: ${rep.status}, umuhimu: ${rep.severity}, ` +
      `ngazi ya uongezaji: ${rep.escalation_level}, idadi ya waandishi: ${rep.report_count}.\n` +
      `Historia:\n${histText || "(hakuna)"}\n\n` +
      `Andika muhtasari wa misto 3-5 kwa Kiswahili kwa afisa: ni nini kilifanyika, hali ya sasa, na hatua inayofuata inayopendekezwa.`;
    const summarizeSystem = "Wewe ni msaidizi wa afisa wa manispaa. Jibu kwa Kiswahili, mafupi, bila kuvuta hitimisho zisizo za data uliyopewa.";

    const mockSummary =
      `Muhtasari (hali ya majaribio): ${rep.public_reference} "${rep.title}" iko ${rep.status}, ` +
      `umuhimu ${rep.severity}, ngazi ya uongezaji ${rep.escalation_level}. Hatua za historia: ${
        (history ?? []).map((h: { to_status: string }) => h.to_status).join(" -> ") || "hakuna"
      }.`;

    if (GROQ_API_KEY) {
      try {
        summary = await groq(summarizePrompt, summarizeSystem);
      } catch {
        if (GEMINI_API_KEY) {
          try {
            provider = "gemini";
            summary = await gemini(summarizePrompt, summarizeSystem);
          } catch {
            provider = "mock";
            mock = true;
            summary = mockSummary;
          }
        } else {
          provider = "mock";
          mock = true;
          summary = mockSummary;
        }
      }
    } else if (GEMINI_API_KEY) {
      try {
        provider = "gemini";
        summary = await gemini(summarizePrompt, summarizeSystem);
      } catch {
        provider = "mock";
        mock = true;
        summary = mockSummary;
      }
    } else {
      provider = "mock";
      mock = true;
      summary = mockSummary;
    }

    const summarizeModel = mock ? "keyword-mock" : provider === "groq" ? GROQ_MODEL : GEMINI_MODEL;
    await service.from("ai_comms").insert({
      user_id: user.id,
      organization_id: rep.organization_id,
      kind: "SUMMARY",
      prompt_summary: rep.public_reference,
      response_summary: summary.slice(0, 500),
      provider,
      model: summarizeModel,
      mock,
      latency_ms: Date.now() - started,
    });

    recordMetric(service, {
      feature: "summarize",
      provider,
      model: summarizeModel,
      mock,
      latencyMs: Date.now() - started,
      success: true,
      userId: user.id,
      organizationId: rep.organization_id,
      reportId,
    });

    return json({
      ok: true,
      action: "summarize",
      provider,
      mock,
      model: summarizeModel,
      summary,
      latencyMs: Date.now() - started,
    });
  } catch (error) {
    recordMetric(service, {
      feature: action ?? "unknown",
      provider: "unknown",
      model: "unknown",
      mock: false,
      latencyMs: Date.now() - started,
      success: false,
      errorMessage: error instanceof Error ? error.message : "internal_error",
      errorCode: "unhandled_exception",
      userId: user.id,
      reportId: body.reportId ?? null,
      organizationId: body.organizationId ?? null,
    });
    return json({ error: error instanceof Error ? error.message : "internal" }, 500);
  }
});
