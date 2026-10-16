import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  if (req.method !== "GET" && req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 405,
    });
  }

  try {
    const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);

    const { data, error } = await supabase
      .from("app_releases")
      .select(
        "version_name, version_code, apk_bucket, apk_path, sha256, size_bytes, min_supported_code, changelog, released_at",
      )
      .eq("is_published", true)
      .order("version_code", { ascending: false })
      .limit(1)
      .maybeSingle();

    if (error) throw error;

    if (!data) {
      return new Response(JSON.stringify({ available: false }), {
        headers: {
          ...corsHeaders,
          "Content-Type": "application/json",
          "Cache-Control": "public, max-age=300",
        },
        status: 200,
      });
    }

    return new Response(
      JSON.stringify({
        available: true,
        version: data.version_name,
        versionCode: data.version_code,
        apkUrl: `${SUPABASE_URL}/storage/v1/object/public/${data.apk_bucket}/${data.apk_path}`,
        sha256: data.sha256,
        sizeMb:
          data.size_bytes != null
            ? Math.round((data.size_bytes / (1024 * 1024)) * 10) / 10
            : undefined,
        releasedAt: data.released_at,
        changelog: Array.isArray(data.changelog) ? data.changelog : [],
        minSupportedCode: data.min_supported_code,
      }),
      {
        headers: {
          ...corsHeaders,
          "Content-Type": "application/json",
          "Cache-Control": "public, max-age=300",
        },
        status: 200,
      },
    );
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});
