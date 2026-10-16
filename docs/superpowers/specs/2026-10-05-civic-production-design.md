# feature/civic_production — Design Spec

**Date:** 2026-10-05
**Owner:** SmartJirani civic platform
**Status:** Draft → awaiting user review

## 1. Overview / Context

`feature/civic_production` is the next graph-engineering step following C1–C10. The civic stack is verified end-to-end against the linked dev DB (`hkpghinqousjboyftjcm`); all 22 acceptance steps passed. This node turns the verified stack into a continuously-running operational system without changing the data model or RPC surface already in production use.

The work is **four independent sub-features** grouped under one spec because they share an integration milestone and one rollout cycle:

1. **Channel gateway multi-provider** — add WhatsApp + SMS alongside the existing in-app transport. Africa's Talking and Twilio are both reachable through the existing `CivicChannelProvider` interface; `LogMockChannelProvider` stays as the staging transport.
2. **pg_cron SLA scanner** — a Postgres-side scheduled job calls `civic_run_sla_scan()` every five minutes so escalation no longer depends on a human pressing "scan now" in the admin console.
3. **Officer console density upgrade** — `MtaaOfficerDashboardScreen` and `DepartmentOfficerConsoleScreen` get a denser layout (single list with filter chips, batched actions, persistent KPI strip).
4. **AI provider swap** — flip the active provider from `KeywordMockCivicAiProvider` to `GeminiCivicAiProvider` via `CIVIC_AI_PROVIDER=gemini`, with transparent fallback to the mock on any Gemini failure.

## 2. Goals & Non-Goals

### Goals
- Production-grade notification fan-out via SMS (round 1) and WhatsApp (round 2, stub this round).
- SLA escalation runs every five minutes without a human trigger.
- Officers can triage a full day's reports from one screen.
- AI triage uses Gemini when configured, falls back to the keyword mock on failure with full audit traceability.

### Non-Goals
- Voice / IVR channels (out of scope; the user can opt into phone-channel in a future node).
- Multi-tenant org isolation changes (the org scoping from C5 stays unchanged).
- Re-architecting the civic state machine or RPC contracts.
- Cleaning the `.civic_tmp\` scratch directory (out of scope per user).
- Replacing the in-app push transport entirely; the existing FCM-disabled transport remains.

## 3. Architecture

```
                     ┌────────────────────────────────────────────────┐
                     │        SmartJirani Postgres (Supabase)         │
                     │                                                │
  Resident app ───┐  │  ┌──────────────────────────────────────────┐  │
                  │  │  │ civic_ai_triage / civic_ai_assistant     │  │
  Officer app ────┤  │  │                                          │  │
                  ├──┼──▶│ Notifications insert (logical event)     │  │
  Admin app ──────┘  │  │   │                                      │  │
                     │  │   ▼                                      │  │
                     │  │ notification_deliveries (per-channel)     │  │
                     │  │   ├── in_app row                         │  │
                     │  │   ├── sms_twilio / sms_africas_talking   │  │
                     │  │   └── whatsapp_noop (this round)         │  │
                     │  └──────────────────────────────────────────┘  │
                     │                                                │
                     │  ┌──────────────────────────────────────────┐  │
                     │  │ pg_cron → civic_run_sla_scan() every 5m  │  │
                     │  └──────────────────────────────────────────┘  │
                     │                                                │
                     │  Gemini 3.0 Flash ◀──┐    ┌── Keyword Mock    │
                     │  (if configured)      │    │  (fallback)       │
                     │                       ▼    ▼                   │
                     │  ┌──────────────────────────────────────────┐  │
                     │  │            civic_ai_audit                │  │
                     │  └──────────────────────────────────────────┘  │
                     └────────────────────────────────────────────────┘
```

**Layer rule (unchanged):** SmartJirani DB is the single source of truth. AI is an intelligence layer; channels are transports; pg_cron is a clock. None of them is authoritative on its own — every state transition still flows through SECURITY DEFINER RPCs that write `audit_logs`.

## 4. Sub-feature 1 — Channel Gateway Multi-Provider

### 4.1 Provider Interface

The existing interface (C6) is extended with the SMS + WhatsApp channel names. No code change to consumers.

```kotlin
interface CivicChannelProvider {
    val name: String  // "in_app" | "sms_twilio" | "sms_africas_talking" | "whatsapp_twilio" | "whatsapp_africas_talking" | "whatsapp_noop"
    suspend fun deliver(payload: CivicNotificationPayload): ChannelResult
}
```

### 4.2 Implementations Shipped This Round

| Provider impl | Behavior this round |
|---|---|
| `InAppChannelProvider` | Existing; unchanged. Writes the in-app realtime event + `notifications.is_read=false` row. |
| `LogMockChannelProvider` | Existing; remains the default active transport. Marks a property of log entries as the audit artifact for staging. |
| `SmsTwilioProvider` | **New.** Live this round. Reads `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`. Calls Twilio REST. |
| `SmsAfricasTalkingProvider` | **New.** Live this round. Reads `AFRICASTALKING_USERNAME`, `AFRICASTALKING_API_KEY`, `AFRICASTALKING_FROM`. Calls AT `/v1/messaging`. |
| `WhatsAppNoOpProvider` | **New.** Stub this round. Writes a `notification_deliveries` row with `provider='whatsapp_noop'` and `status='noop'`. |
| `WhatsAppTwilioProvider` | **New.** Implemented but **disabled** by config this round. Code path is identical to SMS via Twilio but uses the WhatsApp sender; flip on when WhatsApp credentials land. |
| `WhatsAppAfricasTalkingProvider` | **New.** Implemented but **disabled** by config this round. Same as the SMS AT provider with WhatsApp product. |

### 4.3 Resident Channel Preference

- New column `profiles.civic_channel_pref text not null default 'in_app'` — values: `in_app`, `sms`, `whatsapp`, `sms,whatsapp`.
- Set in the existing profile editor; defaults to `in_app` for all existing residents.
- A backfill migration ensures no NULL values: `alter table profiles alter column civic_channel_pref set default 'in_app'; update profiles set civic_channel_pref='in_app' where civic_channel_pref is null;`.

### 4.4 Fan-out Rule

- When a notification is emitted, the channel gateway reads the recipient's `civic_channel_pref` and emits one `notification_deliveries` row per active channel.
- Multiple channels = multiple rows. The base `notifications` row remains the logical event; `notification_deliveries` is the per-channel attempt.
- If a recipient has `civic_channel_pref='sms,whatsapp'` and WhatsApp is disabled at config level, only the SMS row is written — never a NoOp row for WhatsApp in that case (no audit noise).
- If SMS is disabled but the resident prefers SMS, the gateway writes a single `notification_deliveries` row with `status='skipped_no_provider'`. The recipient is shown a banner in the app: "Enable WhatsApp or in-app to receive updates."

## 5. Sub-feature 2 — pg_cron SLA Scanner

### 5.1 Migration

`supabase/migrations/20261005150000_civic_pg_cron.sql`:
- `create extension if not exists pg_cron;`
- `grant usage on schema cron to postgres;`
- `select cron.schedule('civic_run_sla_scan', '*/5 * * * *', $$ select public.civic_run_sla_scan(); $$);`
- Schedule a separate cleanup job for old audit rows (not in this spec — `audit_logs` already has a TTL in C9).

### 5.2 Why pg_cron

- Lives inside the database transaction boundary — failure to invoke cannot outrun a row state.
- No extra infra. No clock-skew concerns (Supabase Edge Function clocks can drift; pg_cron is the DB clock).
- One migration; no Edge Function deploy.
- Auditable via `cron.job_run_history` for ops dashboards.

### 5.3 Failure Handling

- `civic_run_sla_scan()` already wraps the scan in a transaction; failed runs log to `audit_logs` with `entity_type='cron'` and the next run continues from where the previous left off.
- pg_cron records run history in `cron.job_run_details`. A daily view in the admin console surfaces failures.

## 6. Sub-feature 3 — Officer Console Density

### 6.1 Layout

Single scroll surface for `MtaaOfficerDashboardScreen` and `DepartmentOfficerConsoleScreen`:

```
┌──────────────────────────────────────────────┐
│  KPI strip (4 cards): Open / Overdue /       │
│   SLA at risk / Resolved today                │
├──────────────────────────────────────────────┤
│  Filter chips:  ALL | AWAITING_VERIFICATION  │
│   OVERDUE | MY_ASSIGNED | NEW_TODAY          │
├──────────────────────────────────────────────┤
│  Selected rows count  [Batch: Assign /       │
│   Verify / Add note]                          │
├──────────────────────────────────────────────┤
│  Row 1: ilala-mc-2026-00026  Taka  ⏱ 3h LEFT  │
│  Row 2: ilala-mc-2026-00027  Maji  ⚠ OVERDUE  │
│  ...                                          │
└──────────────────────────────────────────────┘
```

- Filter chips: pill-shaped, single-select.
- Batch actions: enabled when ≥1 row is selected. Disallowed batch actions for the actor's role are not shown.
- KPI strip: top of screen, pulls from `civic_analytics` for the officer's `civic_unit_scope`.
- Row tap: navigates to the existing report detail screen with role-appropriate actions.
- Long-press / check-box: enters batch-select mode.

### 6.2 Data Source

- `feature/civic_official/api/CivicOfficialFeed.kt` adds a new `listFeed(filter, scope)` method using a single RPC `civic_official_feed(p_filter, p_scope_unit)` that returns `(report_id, title, ref, severity, status, due_at_minutes, assigned_unit_name, category_en, category_sw)` with one round-trip and a server-side filter.
- RPC `civic_official_feed` is `SECURITY INVOKER`, so the existing RLS keeps it tight.

### 6.3 Real-Time Refresh

- Realtime listener on `civic_reports` filtered to `civic_unit_scope` of the logged-in officer. New rows auto-prepend; status changes auto-update.
- Debounced refresh of KPI strip every 30 seconds.

## 7. Sub-feature 4 — AI Provider Swap

### 7.1 Configuration

`.env`:
```
CIVIC_AI_PROVIDER=gemini   # or "mock"
GEMINI_API_KEY=...          # required when provider=gemini
GEMINI_MODEL=gemini-1.5-flash
```

### 7.2 Provider Resolution

```kotlin
val provider = when (civicConfig.aiProvider) {
    "gemini" -> if (civicConfig.geminiApiKey.isNotBlank())
                    GeminiCivicAiProvider(...) else KeywordMockCivicAiProvider(...)
    else     -> KeywordMockCivicAiProvider(...)
}
```

The fallback to mock when `CIVIC_AI_PROVIDER=gemini` but the key is blank is a graceful local config check; an `civic_ai_audit` row records the swap reason.

### 7.3 Runtime Failure Path

When Gemini returns non-2xx, throws, or exceeds a 5-second timeout:
1. Catch in `GeminiCivicAiProvider`.
2. Write `civic_ai_audit` row with `status='fallback'`, `provider='gemini'`, `latency_ms`, error detail.
3. Delegate to `KeywordMockCivicAiProvider`.
4. The mock's audit row is also written (`status='success'`, `provider='mock_fallback'`).
5. The resident sees a triage result within 5 seconds; the audit log captures both attempts.

This is **not** silent degradation — the audit row is the paper trail.

### 7.4 Assistant Path

`civic_ai_assistant` uses the same provider resolution. Assistant sessions that start on Gemini and degrade to mock mid-session are flagged in `civic_ai_sessions` (`provider_changes jsonb` column, added this round).

## 8. Data Model Changes

### 8.1 New Table: `notification_deliveries`

```sql
create table public.notification_deliveries (
    id uuid primary key default gen_random_uuid(),
    notification_id uuid not null references public.notifications(id) on delete cascade,
    channel text not null check (channel in ('in_app','sms','whatsapp')),
    provider text not null,        -- sms_twilio, sms_africas_talking, whatsapp_noop, in_app, etc.
    recipient_user_id uuid not null references public.profiles(id),
    payload jsonb not null,
    status text not null check (status in ('queued','sent','delivered','failed','skipped_no_provider','noop')),
    provider_response jsonb,
    error text,
    attempted_at timestamptz not null default now(),
    delivered_at timestamptz
);
create index on public.notification_deliveries (notification_id);
create index on public.notification_deliveries (recipient_user_id, attempted_at desc);
create index on public.notification_deliveries (status) where status in ('failed','skipped_no_provider');
alter table public.notification_deliveries enable row level security;
```

**RLS:**
- Recipient can SELECT own rows.
- Officials can SELECT rows for their `civic_unit_scope` recipients.
- COUNCIL_ADMIN / SYSTEM_ADMIN can SELECT all.
- No client INSERT — server-side only via the channel gateway.

### 8.2 New Column: `profiles.civic_channel_pref`

```sql
alter table public.profiles
    add column civic_channel_pref text default 'in_app'
    check (civic_channel_pref in ('in_app','sms','whatsapp','sms,whatsapp'));
update public.profiles set civic_channel_pref = 'in_app' where civic_channel_pref is null;
```

### 8.3 New Column: `civic_ai_sessions.provider_changes`

```sql
alter table public.civic_ai_sessions
    add column provider_changes jsonb default '[]'::jsonb;
```

Append-on-degrade: `[{"from":"gemini","to":"mock_fallback","at":"2026-10-05T..."}]`.

## 9. RPC Changes

### 9.1 New RPC `civic_official_feed`

```sql
create or replace function public.civic_official_feed(
    p_filter text,           -- 'all' | 'awaiting_verification' | 'overdue' | 'mine' | 'new_today'
    p_scope_unit uuid default null
)
returns table (
    report_id uuid, ref text, title text, category_sw text, category_en text,
    severity text, status text, due_at_minutes int, assigned_unit_name text,
    created_at timestamptz, neighborhood_name text
)
language sql stable security invoker set search_path = public as $$
    select r.id, r.reference_number, r.title, c.name_sw, c.name_en,
           r.severity::text, r.status::text,
           (extract(epoch from (r.due_at - now())) / 60)::int as due_at_minutes,
           u.name as assigned_unit_name,
    from civic_reports r
    join civic_categories c on c.id = r.category_id
    left join org_units u on u.id = r.assigned_unit_id
    left join neighborhoods n on n.id = r.neighborhood_id
    where civic_has_report_scope(auth.uid(), r.organization_id, r.unit_id, r.assigned_unit_id)
      and case p_filter
            when 'awaiting_verification' then r.status = 'AWAITING_VERIFICATION'
            when 'overdue' then r.due_at < now() and r.status in ('ASSIGNED','IN_PROGRESS','WAITING_EXTERNAL_ACTION')
            when 'mine' then exists (select 1 from user_roles ur where ur.user_id = auth.uid()
                                       and ur.org_unit_id = r.assigned_unit_id and ur.is_active)
            when 'new_today' then r.created_at::date = current_date
            else true
          end
    order by r.created_at desc
    limit 200;
$$;
```

### 9.2 New RPC `civic_record_delivery`

Insert into `notification_deliveries`. Called from the channel gateway. Server-side only — `security definer`, callable by:
- `service_role` (cron / system actor — used by the pg_cron job and backfill scripts)
- `authenticated` whose `auth.uid()` equals `recipient_user_id` (so the app can mark "delivered" when FCM ack returns)
- `authenticated` with role `SYSTEM_ADMIN` (for ops backfill only)

### 9.3 New RPC `civic_set_channel_pref`

```sql
create or replace function public.civic_set_channel_pref(p_pref text)
returns void language sql security definer set search_path = public as $$
    update profiles set civic_channel_pref = p_pref where id = auth.uid();
$$;
```

## 10. Android Module Changes

### 10.1 New Module: `feature/civic_channels`

- `CivicChannelRegistry.kt` — registers all providers, picks active based on config.
- `CivicChannelRouter.kt` — given a notification, reads recipient `civic_channel_pref`, fans out.
- `CivicChannelConfig.kt` — typed access to the env vars.

### 10.2 New Module: `feature/civic_official` (extension)

- `CivicOfficialFeed.kt` — exposes `listFeed(filter, scope)`.
- `MtaaOfficerDashboardScreen.kt` and `DepartmentOfficerConsoleScreen.kt` get the layout in §6.1.
- `CivicOfficialFilterChips.kt` — reusable component.

### 10.3 Module: `feature/civic` (extension)

- `ProfileChannelPrefEditor.kt` — UI to set `civic_channel_pref`.

### 10.4 Module: `core-ai` (extension)

- `GeminiCivicAiProvider.kt` — real implementation (already exists; verify retry/fallback behavior).
- `AiProviderResolver.kt` — config-driven resolution per §7.2.

## 11. Configuration & Secrets

| Key | Required when | Source |
| --- | --- | --- |
| `TWILIO_ACCOUNT_SID` | SMS-Twilio active | `.env` |
| `TWILIO_AUTH_TOKEN` | SMS-Twilio active | `.env` |
| `TWILIO_FROM_NUMBER` | SMS-Twilio active | `.env` |
| `AFRICASTALKING_USERNAME` | SMS-AT active | `.env` |
| `AFRICASTALKING_API_KEY` | SMS-AT active | `.env` |
| `AFRICASTALKING_FROM` | SMS-AT active | `.env` |
| `CIVIC_AI_PROVIDER` | always | `.env` (default `mock`) |
| `GEMINI_API_KEY` | `CIVIC_AI_PROVIDER=gemini` | `.env` (blank key auto-falls-back to mock per §7.2) |
| `GEMINI_MODEL` | optional | `.env` (default `gemini-1.5-flash`) |
| `CIVIC_SMS_PROVIDER` | SMS active | `.env` (`twilio` \| `africas_talking`, default `twilio`; only the selected provider's credentials are read) |
| `CIVIC_WHATSAPP_PROVIDER` | WhatsApp active | `.env` (default `noop` this round) |

**Security constraint preserved:** `.env` deliberately contains no `service_role` key. `TWILIO_AUTH_TOKEN` and `AFRICASTALKING_API_KEY` are client-side secrets used only in the Edge Function layer (or the DB equivalent for cron). They are NOT `NEXT_PUBLIC_*`. Android does not receive them — the channel gateway runs server-side.

## 12. Error Handling

| Failure | Behavior |
| --- | --- |
| Twilio 4xx/5xx | `notification_deliveries.status='failed'`, `error` populated. Retry on next emission; no automatic retry loop this round. |
| AT 4xx/5xx | Same as Twilio. |
| Gemini timeout (5s) | Fallback to mock. Audit row captures the swap. |
| Gemini 429 | Fallback to mock. Audit row captures the swap. |
| Gemini 4xx (malformed request) | Fallback to mock — no retry, would just fail again. |
| pg_cron job failure | Logged in `audit_logs` (entity_type='cron'). Next run continues. Daily admin view surfaces failures. |
| Resident has no active channel | `notification_deliveries.status='skipped_no_provider'`. Banner in app. |
| Channel provider disabled at config but recipient prefers it | `notification_deliveries.status='skipped_no_provider'`. |
| Realtime listener disconnects | RealtimeService existing reconnect-with-backoff (no change). |

## 13. Testing Plan

### 13.1 Unit

- `SmsTwilioProviderTest` — mock HTTP, assert payload shape, handle 4xx/5xx.
- `SmsAfricasTalkingProviderTest` — mock HTTP, assert payload shape, handle 4xx/5xx.
- `WhatsAppNoOpProviderTest` — assert NoOp row written.
- `CivicChannelRouterTest` — given `civic_channel_pref`, route to correct providers.
- `GeminiCivicAiProviderFallbackTest` — force failure, assert mock fallback and audit row.
- `civic_official_feed` filter test — each filter returns expected rows.

### 13.2 Migration Verification (linked DB)

- Apply migration; verify `notification_deliveries` table created with RLS.
- Verify `pg_cron` extension enabled; verify `cron.job_run_details` after first 5 minutes.
- Verify `profiles.civic_channel_pref` backfilled.

### 13.3 E2E Acceptance — 7 new steps added to the existing 22

23. Resident sets `civic_channel_pref='sms'` in profile.
24. Resident submits garbage report; AI triage runs via Gemini; mock fallback path verified by stubbing GEMINI_API_KEY with an invalid value once.
26. Status changes; SMS row written to `notification_deliveries` with `provider='sms_twilio'`, `status='sent'`.
27. WhatsApp row written with `provider='whatsapp_noop'`, `status='noop'` for residents with `webcivic_channel_pref='sms,whatsapp'`.
28. SLA breach occurs; pg_cron fires within 5 minutes; escalation records new `escalation_level`.
29. Officer opens new `MtaaOfficerDashboardScreen`; KPI strip reflects the latest analytics; filter chip "OVERDUE" returns the right rows.
30. Officer batch-selects 3 rows; batch-verify fires 3 transitions.

### 13.4 Negative Path Tests

- All SMS providers disabled; resident with `civic_channel_pref='sms'`; verify `skipped_no_provider` row + app banner.
- Gemini configured but key invalid; verify fallback.
- Resident submits 50 reports in <30 min; verify anti-abuse rate limit fires (from C7).

## 14. Rollout / Phasing

| Phase | Surface | Risk |
| --- | --- | --- |
| **P0 (immediately)** | Apply migration `20261005150000_civic_pg_cron.sql`; pg_cron job active. `notification_deliveries` table created. No Android changes needed. | Low — additive only. |
| **P1** | Ship SMS providers (Twilio + AT) behind `CIVIC_SMS_PROVIDER`. Ship `CivicChannelRouter` server-side. Default channel still `in_app`. Residents opt into SMS by editing profile. | Low — opt-in. |
| **P2** | Ship AI provider swap. `CIVIC_AI_PROVIDER=gemini` when key configured. Default stays `mock`. | Low — fallback ensures no resident-visible regression. |
| **P3** | Ship officer console density upgrade. | Medium — UI change; needs a UI review. |
| **P4** | WhatsApp live (this round = NoOp stub). | Low — stub-only this round. |

All five phases ship in one release cycle per the user's batch-implementation preference. Each phase has its own rollback path (config flip + migration repair).

## 15. Risks & Limitations

- **pg_cron requires superuser.** Supabase grants `pg_cron` to `postgres`; the migration is run as a `postgres` user via `supabase db push`. If the project ever moves away from Supabase-managed pg_cron, this needs revisiting.
- **Gemini rate limits in production are not free.** The fallback path is the cost guard — if Gemini 429s under load, residents still get a category. Costs need monitoring.
- **SMS provider switch is config-driven, not UI-driven.** A future iteration could expose provider choice in the admin console.
- **WhatsAppNoOp is real but does nothing.** It's there to keep the channel fan-out predictable and the audit trail clean. Replace it with a real provider in P4 of a future round.
- **`notification_deliveries` adds storage. With ~hundreds of reports/day and ~3 deliveries each, that's ~thousands of rows/day. Index on `(recipient_user_id, attempted_at desc)` covers the read path; consider partitioning if volume crosses 10k/day.**
- **AI assistant mid-session provider swap** may produce inconsistent style across turns. This is documented in `civic_ai_sessions.provider_changes` so reviewers can spot it.
- **Officer console batch actions** are powerful; an accidental "batch-verify" of 10 misclassified reports would be visible but already reversible via `transition_civic_report`. No destructive operations in the batch menu this round.

---

**End of spec.** Awaiting user review.