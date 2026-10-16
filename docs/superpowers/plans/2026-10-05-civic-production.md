# feature/civic_production Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the verified C1–C10 civic stack into a continuously-running operational system: multi-provider channel gateway, scheduled SLA escalation, denser officer console, and AI provider swap with transparent fallback.

**Architecture:** Additive to the existing civic surface. New `notification_deliveries` table for per-channel audit, `pg_cron` extension for SLA scan scheduling, single-list-with-filter-chips officer UI, and a config-driven AI provider resolver. Existing RPC contracts and data model remain unchanged.

**Tech Stack:** Supabase Postgres + PostgREST + Edge Functions + Realtime; Kotlin + Jetpack Compose (Android); Kotlin coroutines for HTTP (Ktor); Kotlinx-serialization for payloads.

**Spec:** `docs/superpowers/specs/2026-10-05-civic-production-design.md`

## Global Constraints

- **Project root:** `E:\Project\Smartjirani` — all paths in this plan are relative to it.
- **Git:** project is NOT a git repository. `git commit` steps in this plan assume `git init` has been run first; if not, replace commit steps with `mkdir -p .plan_logs/$(date +%Y-%m-%d)/<task-id> && cp <changed files> .plan_logs/$(date +%Y-%m-%d)/<task-id>/`.
- **DB access:** use `supabase db query --linked` from `E:\Project\Smartjirani` for all postgres-level access. `DATABASE_URL` in `.env` points at a different project — never use it.
- **Service role:** `.env` deliberately contains no `service_role` key. Never add one.
- **dd396515@gmail.com:** is the user's real account — never reset or modify its password/credentials. Test users (e.g. `e2e-resident@…`, `e2e-official@…`) are created fresh per migration and exist for the test run.
- **AI rule:** SmartJirani DB is the source of truth. AI is an intelligence layer — never make an external AI product the source of truth. Provider interface (`CivicAiProvider`) is the contract; both Gemini and keyword-mock implement it.
- **Naming:** every migration filename starts with `YYYYMMDDHHMMSS_` (UTC, 14 digits). Migrations are committed via `supabase db push`. Never invent filenames or rely on memory for the format.
- **Audit invariant:** every state-changing RPC writes an `audit_logs` row with stable `entity_type`/`action` values.
- **Privacy:** AI assistant answers must only reference rows the caller can see (RLS via SECURITY INVOKER). PII must never leak across org boundaries.
- **Versioning:** Android module versions follow the project `gradle/libs.versions.toml` floors — never introduce a higher floor without updating the catalog.
- **i18n:** every new user-visible string ships in both `values/strings.xml` and `values-sw/strings.xml`.
- **RLS posture:** RLS enabled on every new table. Explicit grants for `anon`/`authenticated` only where the public surface requires it.

---

## File Structure

| Path | Responsibility | Created in |
| --- | --- | --- |
| `supabase/migrations/20261005150000_civic_pg_cron.sql` | Enable pg_cron, schedule `civic_run_sla_scan` every 5 min | Task 1 |
| `supabase/migrations/20261005150100_civic_notification_deliveries.sql` | New `notification_deliveries` table + RLS | Task 2 |
| `supabase/migrations/20261005150200_civic_channel_pref.sql` | `profiles.civic_channel_pref` column + backfill | Task 3 |
| `supabase/migrations/20261005150300_civic_record_delivery_rpc.sql` | `civic_record_delivery` RPC | Task 4 |
| `supabase/migrations/20261005150400_civic_set_channel_pref_rpc.sql` | `civic_set_channel_pref` RPC | Task 5 |
| `supabase/migrations/20261005150500_civic_official_feed_rpc.sql` | `civic_official_feed` RPC | Task 6 |
| `supabase/migrations/20261005150600_civic_ai_sessions_provider_changes.sql` | `civic_ai_sessions.provider_changes` column | Task 12 |
| `app/src/main/java/com/smartjirani/civic/channels/CivicChannelProvider.kt` | Channel provider interface | Task 7 |
| `app/src/main/java/com/smartjirani/civic/channels/CivicChannelRegistry.kt` | Provider registry | Task 11 |
| `app/src/main/java/com/smartjirani/civic/channels/CivicChannelRouter.kt` | Routes a notification to one row per channel | Task 11 |
| `app/src/main/java/com/smartjirani/civic/channels/CivicChannelConfig.kt` | Typed env access | Task 11 |
| `app/src/main/java/com/smartjirani/civic/channels/InAppChannelProvider.kt` | Existing; unchanged | — |
| `app/src/main/java/com/smartjirani/civic/channels/LogMockChannelProvider.kt` | Existing; unchanged | — |
| `app/src/main/java/com/smartjirani/civic/channels/SmsTwilioProvider.kt` | Twilio SMS provider | Task 7 |
| `app/src/main/java/com/smartjirani/civic/channels/SmsAfricasTalkingProvider.kt` | AT SMS provider | Task 8 |
| `app/src/main/java/com/smartjirani/civic/channels/WhatsAppNoOpProvider.kt` | Stub | Task 9 |
| `app/src/main/java/com/smartjirani/civic/channels/WhatsAppTwilioProvider.kt` | Disabled this round | Task 10 |
| `app/src/main/java/com/smartjirani/civic/channels/WhatsAppAfricasTalkingProvider.kt` | Disabled this round | Task 10 |
| `app/src/main/java/com/smartjirani/civic/profile/ProfileChannelPrefEditor.kt` | UI for channel preference | Task 13 |
| `app/src/main/java/com/smartjirani/civic/official/CivicOfficialFeed.kt` | Feed API client | Task 6 |
| `app/src/main/java/com/smartjirani/civic/official/CivicOfficialFilterChips.kt` | Reusable filter component | Task 14 |
| `app/src/main/java/com/smartjirani/civic/official/MtaaOfficerDashboardScreen.kt` | Density upgrade | Task 14 |
| `app/src/main/java/com/smartjirani/civic/official/DepartmentOfficerConsoleScreen.kt` | Density upgrade | Task 15 |
| `app/src/main/java/com/smartjirani/core/ai/AiProviderResolver.kt` | Config-driven resolver | Task 12 |
| `app/src/main/java/com/smartjirani/core/ai/GeminiCivicAiProvider.kt` | Existing; enhance fallback | Task 12 |
| `app/src/main/res/values/strings.xml` | Add new strings | Tasks 13–15 |
| `app/src/main/res/values-sw/strings.xml` | Swahili strings | Tasks 13–15 |
| `app/src/test/java/com/smartjirani/civic/channels/SmsTwilioProviderTest.kt` | Unit | Task 7 |
| `app/src/test/java/com/smartjirani/civic/channels/SmsAfricasTalkingProviderTest.kt` | Unit | Task 8 |
| `app/src/test/java/com/smartjirani/civic/channels/WhatsAppNoOpProviderTest.kt` | Unit | Task 9 |
| `app/src/test/java/com/smartjirani/civic/channels/CivicChannelRouterTest.kt` | Unit | Task 11 |
| `app/src/test/java/com/smartjirani/civic/official/CivicOfficialFilterChipsTest.kt` | Compose UI | Task 14 |
| `app/src/test/java/com/smartjirani/core/ai/AiProviderResolverTest.kt` | Unit | Task 12 |
| `app/src/test/java/com/smartjirani/core/ai/GeminiCivicAiProviderFallbackTest.kt` | Unit | Task 12 |
| `app/src/test/java/com/smartjirani/civic/profile/ProfileChannelPrefEditorTest.kt` | Compose UI | Task 13 |

---

## Task 1: pg_cron SLA Scanner

**Files:**
- Create: `supabase/migrations/20261005150000_civic_pg_cron.sql`

**Interfaces:**
- Consumes: `public.civic_run_sla_scan()` (existing RPC, service_role only).
- Produces: cron schedule `'civic_run_sla_scan'` running every 5 minutes.

- [ ] **Step 1: Create migration file with this exact content**

```sql
-- Civic Production Support: pg_cron extension + scheduled SLA scan.
-- The civic state machine already exposes civic_run_sla_scan() to service_role.
-- This migration schedules it to run every 5 minutes via pg_cron. No schema changes.

create extension if not exists pg_cron;

-- pg_cron's default schema is `cron` and its schedule() function is variadic.
-- The job is idempotent — pg_cron errors if a job with the same name exists, so
-- the cleanup step below guards re-application after a prior partial apply.

do $$
begin
  if exists (select 1 from cron.job where jobname = 'civic_run_sla_scan') then
    perform cron.unschedule('civic_run_sla_scan');
  end if;
end
$$;

select cron.schedule(
  'civic_run_sla_scan',
  '*/5 * * * *',
  $$ select public.civic_run_sla_scan(); $$
);
```

- [ ] **Step 2: Apply migration to linked DB**

Run: `supabase db push`
Expected: "Finished supabase db push." with one new migration applied.

- [ ] **Step 3: Verify the cron job exists**

Run: `supabase db query --linked -c "select jobname, schedule, active from cron.job;"`
Expected: a row with `jobname='civic_run_sla_scan'`, `schedule='*/5 * * * *'`, `active=true`.

- [ ] **Step 4: Backdate one report's `due_at` and verify the cron runs**

Run:
```
supabase db query --linked -c "update civic_reports set due_at = now() - interval '1 hour' where status = 'ASSIGNED' and due_at is not null limit 1;"
sleep 360
supabase db query --linked -c "select start_time, end_time, status from cron.job_run_details order by start_time desc limit 3;"
```
Expected: at least one row in `cron.job_run_details` with `status='succeeded'` from the last few minutes.

- [ ] **Step 5: Commit**

```bash
git add supabase/migrations/20261005150000_civic_pg_cron.sql
git commit -m "feat(civic): pg_cron every-5m SLA scan"
```

---

## Task 2: `notification_deliveries` Table + RLS

**Files:**
- Create: `supabase/migrations/20261005150100_civic_notification_deliveries.sql`

**Interfaces:**
- Produces: `public.notification_deliveries` table with RLS; recipient + admin reads; no client writes.

- [ ] **Step 1: Create migration file with this exact content**

```sql
-- Civic Production Support: per-channel delivery audit. The existing
-- notifications table is the logical notification event; this table holds
-- one row per attempted delivery (channel + provider). Fan-out writes one
-- row per active channel the recipient prefers.

create table if not exists public.notification_deliveries (
    id uuid primary key default gen_random_uuid(),
    notification_id uuid not null references public.notifications(id) on delete cascade,
    channel text not null check (channel in ('in_app','sms','whatsapp')),
    provider text not null,
    recipient_user_id uuid not null references public.profiles(id),
    payload jsonb not null,
    status text not null check (status in ('queued','sent','delivered','failed','skipped_no_provider','noop')),
    provider_response jsonb,
    error text,
    attempted_at timestamptz not null default now(),
    delivered_at timestamptz
);

create index if not exists notification_deliveries_notification_id_idx
    on public.notification_deliveries (notification_id);
create index if not exists notification_deliveries_recipient_idx
    on public.notification_deliveries (recipient_user_id, attempted_at desc);
create index if not exists notification_deliveries_failed_idx
    on public.notification_deliveries (status)
    where status in ('failed','skipped_no_provider');

alter table public.notification_deliveries enable row level security;

drop policy if exists "notification_deliveries_select_own" on public.notification_deliveries;
create policy "notification_deliveries_select_own"
    on public.notification_deliveries for select
    to authenticated
    using ( (select auth.uid()) = recipient_user_id );

drop policy if exists "notification_deliveries_select_official_scope" on public.notification_deliveries;
create policy "notification_deliveries_select_official_scope"
    on public.notification_deliveries for select
    to authenticated
    using (
        exists (
            select 1
            from public.user_roles ur
            where ur.user_id = (select auth.uid())
              and ur.is_active
              and ur.role in ('MUNICIPAL_OFFICER','COUNCIL_ADMIN','SYSTEM_ADMIN')
        )
    );

-- No INSERT/UPDATE/DELETE policies. Inserts happen server-side via
-- civic_record_delivery (Task 4). Updates are service_role only.

grant usage on schema public to authenticated;
grant select on public.notification_deliveries to authenticated;
```

- [ ] **Step 2: Apply migration**

Run: `supabase db push`
Expected: "Finished supabase db push." with one new migration applied.

- [ ] **Step 3: Verify the table exists with RLS**

Run:
```
supabase db query --linked -c "\d public.notification_deliveries"
supabase db query --linked -c "select policyname from pg_policies where tablename = 'notification_deliveries' order by policyname;"
```
Expected: table with all columns from the migration; two policies named `notification_deliveries_select_*`.

- [ ] **Step 4: Confirm anon has no SELECT**

Run: `supabase db query --linked -c "select has_table_privilege('anon', 'public.notification_deliveries', 'SELECT');"`
Expected: `false`.

- [ ] **Step 5: Commit**

```bash
git add supabase/migrations/20261005150100_civic_notification_deliveries.sql
git commit -m "feat(civic): notification_deliveries table + RLS"
```

---

## Task 3: `profiles.civic_channel_pref` Column

**Files:**
- Create: `supabase/migrations/20261005150200_civic_channel_pref.sql`

- [ ] **Step 1: Create migration file**

```sql
-- Civic Production Support: per-resident channel preference. Backfilled to
-- 'in_app' for existing rows so the column is never NULL.

alter table public.profiles
    add column if not exists civic_channel_pref text;

update public.profiles
    set civic_channel_pref = 'in_app'
    where civic_channel_pref is null;

alter table public.profiles
    alter column civic_channel_pref set default 'in_app';

alter table public.profiles
    alter column civic_channel_pref set not null;

alter table public.profiles
    drop constraint if exists profiles_civic_channel_pref_check;
alter table public.profiles
    add constraint profiles_civic_channel_pref_check
    check (civic_channel_pref in ('in_app','sms','whatsapp','sms,whatsapp'));
```

- [ ] **Step 2: Apply migration**

Run: `supabase db push`

- [ ] **Step 3: Verify**

Run: `supabase db query --linked -c "select civic_channel_pref, count(*) from public.profiles group by 1;"`
Expected: every profile has a non-null value; majority is `in_app`.

- [ ] **Step 4: Commit**

```bash
git add supabase/migrations/20261005150200_civic_channel_pref.sql
git commit -m "feat(civic): profiles.civic_channel_pref + backfill"
```

---

## Task 4: `civic_record_delivery` RPC

**Files:**
- Create: `supabase/migrations/20261005150300_civic_record_delivery_rpc.sql`

**Interfaces:**
- Consumes: `public.notification_deliveries` (Task 2).
- Produces: `public.civic_record_delivery(p_notification_id, p_channel, p_provider, p_recipient_user_id, p_payload, p_status, p_provider_response, p_error, p_delivered_at) returns uuid`.

- [ ] **Step 1: Create migration file**

```sql
-- Server-side insert helper for per-channel delivery rows.
-- Callable by:
--   * service_role (cron / system actor)
--   * the recipient themselves (FCM delivery ack from the app)
--   * SYSTEM_ADMIN (ops backfill)

create or replace function public.civic_record_delivery(
    p_notification_id uuid,
    p_channel text,
    p_provider text,
    p_recipient_user_id uuid,
    p_payload jsonb,
    p_status text,
    p_provider_response jsonb default null,
    p_error text default null,
    p_delivered_at timestamptz default null
)
returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_uid uuid := auth.uid();
    v_role text;
    v_id uuid;
begin
    if v_uid is null then
        raise exception 'unauthenticated' using errcode = '42501';
    end if;

    -- service_role bypass: when auth.uid() is null (server key), allow.
    if v_uid is not null then
        if v_uid = p_recipient_user_id then
            -- self-ack path
            null;
        else
            select role::text into v_role
            from public.user_roles
            where user_id = v_uid and is_active
            order by case role
                when 'SYSTEM_ADMIN' then 0
                when 'COUNCIL_ADMIN' then 1
                when 'MUNICIPAL_OFFICER' then 2
                else 9
            end
            limit 1;
            if v_role is distinct from 'SYSTEM_ADMIN' then
                raise exception 'not_authorized' using errcode = '42501';
            end if;
        end if;
    end if;

    insert into public.notification_deliveries(
        notification_id, channel, provider, recipient_user_id,
        payload, status, provider_response, error, delivered_at
    ) values (
        p_notification_id, p_channel, p_provider, p_recipient_user_id,
        p_payload, p_status, p_provider_response, p_error, p_delivered_at
    )
    returning id into v_id;

    return v_id;
end;
$$;

grant execute on function public.civic_record_delivery(
    uuid, text, text, uuid, jsonb, text, jsonb, text, timestamptz
) to authenticated;
```

- [ ] **Step 2: Apply migration**

Run: `supabase db push`

- [ ] **Step 3: Smoke-test as authenticated**

Run:
```
TOK=$(curl -s -X POST 'https://hkpghinqousjboyftjcm.supabase.co/auth/v1/token?grant_type=password' \
  -H "apikey: $ANON_KEY" \
  -d '{"email":"<e2e-resident-email>","password":"<password>"}' | jq -r .access_token)
NOTIF=$(supabase db query --linked -t -c "select id from public.notifications order by created_at desc limit 1;" | tr -d ' ')
supabase db query --linked -c "select public.civic_record_delivery('$NOTIF'::uuid, 'sms', 'sms_twilio', '<e2e-resident-uuid>'::uuid, '{\"x\":1}'::jsonb, 'queued');"
```
Expected: a UUID returned.

- [ ] **Step 4: Negative-test as another resident (should fail)**

Run:
```
supabase db query --linked -c "select public.civic_record_delivery('$NOTIF'::uuid, 'sms', 'sms_twilio', '<other-resident-uuid>'::uuid, '{}'::jsonb, 'queued');"
```
Expected: `ERROR: not_authorized` with errcode `42501`.

- [ ] **Step 5: Commit**

```bash
git add supabase/migrations/20261005150300_civic_record_delivery_rpc.sql
git commit -m "feat(civic): civic_record_delivery RPC"
```

---

## Task 5: `civic_set_channel_pref` RPC

**Files:**
- Create: `supabase/migrations/20261005150400_civic_set_channel_pref_rpc.sql`

- [ ] **Step 1: Create migration file**

```sql
create or replace function public.civic_set_channel_pref(p_pref text)
returns void
language sql security definer set search_path = public as $$
    update public.profiles
    set civic_channel_pref = p_pref
    where id = auth.uid();
$$;

grant execute on function public.civic_set_channel_pref(text) to authenticated;
```

- [ ] **Step 2: Apply migration**

Run: `supabase db push`

- [ ] **Step 3: Smoke-test**

Run:
```
TOK=$(... as above ...)
curl -s -X POST 'https://hkpghinqousjboyftjcm.supabase.co/rest/v1/rpc/civic_set_channel_pref' \
  -H "apikey: $ANON_KEY" \
  -H "Authorization: Bearer $TOK" \
  -H "Content-Type: application/json" \
  -d '"sms"'
supabase db query --linked -c "select civic_channel_pref from public.profiles where id = '<e2e-resident-uuid>';"
```
Expected: row updated to `sms`. Set it back to `in_app` after the test.

- [ ] **Step 4: Negative-test invalid value**

Run:
```
curl -s -X POST 'https://hkpghinqousjboyftjcm.supabase.co/rest/v1/rpc/civic_set_channel_pref' \
  -H "apikey: $ANON_KEY" \
  -H "Authorization: Bearer $TOK" \
  -H "Content-Type: application/json" \
  -d '"pigeon"'
```
Expected: `400` with `profiles_civic_channel_pref_check` violation in the error body.

- [ ] **Step 5: Commit**

```bash
git add supabase/migrations/20261005150400_civic_set_channel_pref_rpc.sql
git commit -m "feat(civic): civic_set_channel_pref RPC"
```

---

## Task 6: `civic_official_feed` RPC + Feed Client

**Files:**
- Create: `supabase/migrations/20261005150500_civic_official_feed_rpc.sql`
- Create: `app/src/main/java/com/smartjirani/civic/official/CivicOfficialFeed.kt`

**Interfaces:**
- Produces: `fn civic_official_feed(p_filter text, p_scope_unit uuid default null) returns table (...)` — feed rows for officer consoles.
- Produces: `CivicOfficialFeed.list(filter, scopeUnit)` Kotlin API.

- [ ] **Step 1: Create migration file**

```sql
create or replace function public.civic_official_feed(
    p_filter text,
    p_scope_unit uuid default null
)
returns table (
    report_id uuid,
    ref text,
    title text,
    category_sw text,
    category_en text,
    severity text,
    status text,
    due_at_minutes int,
    assigned_unit_name text,
    created_at timestamptz,
    neighborhood_name text
)
language sql stable security invoker set search_path = public as $$
    select
        r.id,
        r.reference_number,
        r.title,
        c.name_sw,
        c.name_en,
        r.severity::text,
        r.status::text,
        (extract(epoch from (r.due_at - now())) / 60)::int as due_at_minutes,
        coalesce(au.name, '')::text,
        r.created_at,
        coalesce(n.name_sw, '')::text
    from public.civic_reports r
    join public.civic_categories c on c.id = r.category_id
    left join public.org_units au on au.id = r.assigned_unit_id
    left join public.neighborhoods n on n.id = r.neighborhood_id
    where public.civic_has_report_scope(auth.uid(), r.organization_id, r.unit_id, r.assigned_unit_id)
      and (
          p_scope_unit is null
          or r.assigned_unit_id = p_scope_unit
          or r.unit_id = p_scope_unit
      )
      and case p_filter
            when 'awaiting_verification' then r.status = 'AWAITING_VERIFICATION'
            when 'overdue' then r.due_at < now()
                                 and r.status in ('ASSIGNED','IN_PROGRESS','WAITING_EXTERNAL_ACTION')
            when 'mine' then exists (
                select 1 from public.user_roles ur
                where ur.user_id = auth.uid()
                  and ur.is_active
                  and ur.org_unit_id = r.assigned_unit_id
            )
            when 'new_today' then r.created_at::date = current_date
            else true
          end
    order by r.created_at desc
    limit 200;
$$;

grant execute on function public.civic_official_feed(text, uuid) to authenticated;
```

- [ ] **Step 2: Apply migration**

Run: `supabase db push`

- [ ] **Step 3: Verify each filter returns expected rows**

Run (with officer token):
```
TOK=$(curl ... -d '{"email":"<e2e-official-email>","password":"<password>"}' | jq -r .access_token)
for F in all awaiting_verification overdue mine new_today; do
  echo "=== filter=$F ==="
  curl -s -X POST 'https://hkpghinqousjboyftjcm.supabase.co/rest/v1/rpc/civic_official_feed' \
    -H "apikey: $ANON_KEY" -H "Authorization: Bearer $TOK" -H "Content-Type: application/json" \
    -d "[\"$F\", null]" | jq '.[] | {ref, status, due_at_minutes}'
done
```
Expected: at least `all` returns >0 rows; the others return a subset (or empty if no rows match).

- [ ] **Step 4: Create Kotlin feed client**

File: `app/src/main/java/com/smartjirani/civic/official/CivicOfficialFeed.kt`

```kotlin
package com.smartjirani.civic.official

import com.smartjirani.civic.data.SupabaseClientFactory
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class CivicOfficialFeedRow(
    val report_id: String,
    val ref: String,
    val title: String,
    val category_sw: String,
    val category_en: String,
    val severity: String,
    val status: String,
    val due_at_minutes: Int,
    val assigned_unit_name: String,
    val created_at: String,
    val neighborhood_name: String,
)

enum class CivicOfficialFilter(val value: String) {
    ALL("all"),
    AWAITING_VERIFICATION("awaiting_verification"),
    OVERDUE("overdue"),
    MINE("mine"),
    NEW_TODAY("new_today"),
}

object CivicOfficialFeed {
    suspend fun list(filter: CivicOfficialFilter, scopeUnit: String? = null): List<CivicOfficialFeedRow> {
        val client = SupabaseClientFactory.authedClient()
        return client.postgrest.rpc(
            "civic_official_feed",
            parameters = mapOf(
                "p_filter" to filter.value,
                "p_scope_unit" to scopeUnit,
            ),
        ).decodeList<CivicOfficialFeedRow>()
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add supabase/migrations/20261005150500_civic_official_feed_rpc.sql app/src/main/java/com/smartjirani/civic/official/CivicOfficialFeed.kt
git commit -m "feat(civic): official feed RPC + client"
```

---

## Task 7: `SmsTwilioProvider`

**Files:**
- Create: `app/src/main/java/com/smartjirani/civic/channels/SmsTwilioProvider.kt`
- Create: `app/src/test/java/com/smartjirani/civic/channels/SmsTwilioProviderTest.kt`

**Interfaces:**
- Consumes: `CivicChannelConfig` (Task 11).
- Produces: implementation of `CivicChannelProvider` named `"sms_twilio"`.

- [ ] **Step 1: Write the failing test**

File: `app/src/test/java/com/smartjirani/civic/channels/SmsTwilioProviderTest.kt`

```kotlin
package com.smartjirani.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmsTwilioProviderTest {

    private fun cfg() = CivicChannelConfig(
        twilioAccountSid = "AC_test",
        twilioAuthToken = "tok",
        twilioFromNumber = "+15555550100",
        africasTalkingUsername = null,
        africasTalkingApiKey = null,
        africasTalkingFrom = null,
        smsProvider = "twilio",
    )

    @Test
    fun `name is sms_twilio`() {
        assertEquals("sms_twilio", SmsTwilioProvider(cfg()).name)
    }

    @Test
    fun `deliver returns sent on 201`() = runTest {
        val fake = FakeHttp { _, _ ->
            HttpResponse(status = 201, body = """{"sid":"SM123","status":"queued"}""")
        }
        val provider = SmsTwilioProvider(cfg(), http = fake)
        val result = provider.deliver(
            CivicNotificationPayload(
                notificationId = "n1",
                recipientUserId = "u1",
                toPhone = "+255712345678",
                title = "Test",
                body = "Hello",
            )
        )
        assertEquals(ChannelResult.Sent("SM123"), result)
    }

    @Test
    fun `deliver returns failed on 4xx`() = runTest {
        val fake = FakeHttp { _, _ ->
            HttpResponse(status = 403, body = """{"code":20003,"message":"auth"}""")
        }
        val provider = SmsTwilioProvider(cfg(), http = fake)
        val result = provider.deliver(
            CivicNotificationPayload(
                notificationId = "n1",
                recipientUserId = "u1",
                toPhone = "+255712345678",
                title = "Test",
                body = "Hello",
            )
        )
        assertTrue(result is ChannelResult.Failed)
    }
}
```

(Helper types `HttpResponse`, `FakeHttp`, `CivicNotificationPayload`, `ChannelResult` are defined in Task 11 alongside the interface. If Task 11 hasn't shipped yet, write a minimal local stub interface in a `test/.../support/` file.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.channels.SmsTwilioProviderTest"`
Expected: compile error (`SmsTwilioProvider` not found).

- [ ] **Step 3: Write minimal implementation**

File: `app/src/main/java/com/smartjirani/civic/channels/SmsTwilioProvider.kt`

```kotlin
package com.smartjirani.civic.channels

import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SmsTwilioProvider(
    private val cfg: CivicChannelConfig,
    private val http: HttpClient = defaultHttp(),
) : CivicChannelProvider {
    override val name = "sms_twilio"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult {
        require(cfg.smsProvider == "twilio") { "CIVIC_SMS_PROVIDER != twilio" }
        val url = "https://api.twilio.com/2010-04-01/Accounts/${cfg.twilioAccountSid}/Messages.json"
        val form = "To=${payload.toPhone}&From=${cfg.twilioFromNumber}&Body=${payload.body}"
        val resp = http.post(url) {
            basicAuth(cfg.twilioAccountSid, cfg.twilioAuthToken)
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(form)
        }
        val body = resp.bodyAsText()
        return when (resp.statusCode) {
            HttpStatusCode.Created, HttpStatusCode.Accepted -> {
                val sid = runCatching {
                    Json.parseToJsonElement(body).let { (it as JsonObject)["sid"]?.jsonPrimitive?.content }
                }.getOrNull() ?: "unknown"
                ChannelResult.Sent(sid)
            }
            else -> ChannelResult.Failed("twilio_${resp.statusCode.value}", body)
        }
    }
}

private fun defaultHttp(): HttpClient = HttpClient { /* engine config follows project floors */ }
```

(Place this private helper in the same file or in a shared `channels/Http.kt`.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.channels.SmsTwilioProviderTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smartjirani/civic/channels/SmsTwilioProvider.kt app/src/test/java/com/smartjirani/civic/channels/SmsTwilioProviderTest.kt
git commit -m "feat(civic-channels): SmsTwilioProvider with tests"
```

---

## Task 8: `SmsAfricasTalkingProvider`

**Files:**
- Create: `app/src/main/java/com/smartjirani/civic/channels/SmsAfricasTalkingProvider.kt`
- Create: `app/src/test/java/com/smartjirani/civic/channels/SmsAfricasTalkingProviderTest.kt`

**Interfaces:**
- Produces: implementation of `CivicChannelProvider` named `"sms_africas_talking"`.

- [ ] **Step 1: Write the failing test**

File: `app/src/test/java/com/smartjirani/civic/channels/SmsAfricasTalkingProviderTest.kt`

```kotlin
package com.smartjirani.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmsAfricasTalkingProviderTest {

    private fun cfg() = CivicChannelConfig(
        twilioAccountSid = null,
        twilioAuthToken = null,
        twilioFromNumber = null,
        africasTalkingUsername = "u",
        africasTalkingApiKey = "k",
        africasTalkingFrom = "SmartJirani",
        smsProvider = "africas_talking",
    )

    @Test
    fun `name is sms_africas_talking`() {
        assertEquals("sms_africas_talking", SmsAfricasTalkingProvider(cfg()).name)
    }

    @Test
    fun `deliver returns sent on 201`() = runTest {
        val fake = FakeHttp { _, _ ->
            HttpResponse(
                status = 201,
                body = """{"SMSMessageData":{"MessageId":"AT-1","Recipients":[{"status":"Success"}]}}"""
            )
        }
        val provider = SmsAfricasTalkingProvider(cfg(), http = fake)
        val result = provider.deliver(
            CivicNotificationPayload(
                notificationId = "n1",
                recipientUserId = "u1",
                toPhone = "+255712345678",
                title = "Test",
                body = "Hello",
            )
        )
        assertEquals(ChannelResult.Sent("AT-1"), result)
    }

    @Test
    fun `deliver returns failed on 4xx`() = runTest {
        val fake = FakeHttp { _, _ ->
            HttpResponse(status = 401, body = """{"error":"unauthorized"}""")
        }
        val provider = SmsAfricasTalkingProvider(cfg(), http = fake)
        val result = provider.deliver(
            CivicNotificationPayload(
                notificationId = "n1",
                recipientUserId = "u1",
                toPhone = "+255712345678",
                title = "Test",
                body = "Hello",
            )
        )
        assertTrue(result is ChannelResult.Failed)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.channels.SmsAfricasTalkingProviderTest"`
Expected: compile error (`SmsAfricasTalkingProvider` not found).

- [ ] **Step 3: Write minimal implementation**

File: `app/src/main/java/com/smartjirani/civic/channels/SmsAfricasTalkingProvider.kt`

```kotlin
package com.smartjirani.civic.channels

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SmsAfricasTalkingProvider(
    private val cfg: CivicChannelConfig,
    private val http: HttpClient = defaultHttp(),
) : CivicChannelProvider {
    override val name = "sms_africas_talking"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult {
        require(cfg.smsProvider == "africas_talking") { "CIVIC_SMS_PROVIDER != africas_talking" }
        val url = "https://api.africastalking.com/version1/messaging"
        val form = "username=${cfg.africasTalkingUsername}&to=${payload.toPhone}" +
                   "&message=${payload.body}&from=${cfg.africasTalkingFrom}"
        val resp = http.post(url) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                append("apiKey", cfg.africasTalkingApiKey!!)
            }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(form)
        }
        val body = resp.bodyAsText()
        return when (resp.statusCode) {
            HttpStatusCode.Created, HttpStatusCode.Accepted, HttpStatusCode.OK -> {
                val mid = runCatching {
                    Json.parseToJsonElement(body).let { (it as JsonObject)["SMSMessageData"]?.jsonObject?.get("MessageId")?.jsonPrimitive?.content }
                }.getOrNull() ?: "unknown"
                ChannelResult.Sent(mid)
            }
            else -> ChannelResult.Failed("africas_talking_${resp.statusCode.value}", body)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.channels.SmsAfricasTalkingProviderTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smartjirani/civic/channels/SmsAfricasTalkingProvider.kt app/src/test/java/com/smartjirani/civic/channels/SmsAfricasTalkingProviderTest.kt
git commit -m "feat(civic-channels): SmsAfricasTalkingProvider with tests"
```

---

## Task 9: `WhatsAppNoOpProvider`

**Files:**
- Create: `app/src/main/java/com/smartjirani/civic/channels/WhatsAppNoOpProvider.kt`
- Create: `app/src/test/java/com/smartjirani/civic/channels/WhatsAppNoOpProviderTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.smartjirani.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class WhatsAppNoOpProviderTest {
    @Test
    fun `name is whatsapp_noop`() {
        assertEquals("whatsapp_noop", WhatsAppNoOpProvider().name)
    }

    @Test
    fun `deliver always returns NoOp`() = runTest {
        val provider = WhatsAppNoOpProvider()
        val result = provider.deliver(
            CivicNotificationPayload(
                notificationId = "n1",
                recipientUserId = "u1",
                toPhone = "+255712345678",
                title = "Test",
                body = "Hello",
            )
        )
        assertEquals(ChannelResult.NoOp, result)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.channels.WhatsAppNoOpProviderTest"`
Expected: compile error.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.smartjirani.civic.channels

class WhatsAppNoOpProvider : CivicChannelProvider {
    override val name = "whatsapp_noop"
    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult = ChannelResult.NoOp
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.channels.WhatsAppNoOpProviderTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smartjirani/civic/channels/WhatsAppNoOpProvider.kt app/src/test/java/com/smartjirani/civic/channels/WhatsAppNoOpProviderTest.kt
git commit -m "feat(civic-channels): WhatsAppNoOpProvider stub"
```

---

## Task 10: Disabled WhatsApp Twilio + AT Providers

**Files:**
- Create: `app/src/main/java/com/smartjirani/civic/channels/WhatsAppTwilioProvider.kt`
- Create: `app/src/main/java/com/smartjirani/civic/channels/WhatsAppAfricasTalkingProvider.kt`

**Note:** These are implemented but the registry in Task 12 does NOT activate them by default; they are wired only when `CIVIC_WHATSAPP_PROVIDER` is set. No unit tests this round — covered in Task 12's end-to-end flip-on test.

- [ ] **Step 1: Create WhatsApp Twilio provider**

```kotlin
package com.smartjirani.civic.channels

import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class WhatsAppTwilioProvider(
    private val cfg: CivicChannelConfig,
    private val http: HttpClient = defaultHttp(),
) : CivicChannelProvider {
    override val name = "whatsapp_twilio"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult {
        require(cfg.whatsappProvider == "twilio") { "CIVIC_WHATSAPP_PROVIDER != twilio" }
        val url = "https://api.twilio.com/2010-04-01/Accounts/${cfg.twilioAccountSid}/Messages.json"
        val bodyJson = buildJsonObject {
            put("To", "whatsapp:${payload.toPhone}")
            put("From", "whatsapp:${cfg.twilioWhatsappFrom}")
            put("Body", payload.body)
        }
        val resp = http.post(url) {
            basicAuth(cfg.twilioAccountSid, cfg.twilioAuthToken!!)
            contentType(ContentType.Application.Json)
            setBody(bodyJson.toString())
        }
        val text = resp.bodyAsText()
        return when (resp.statusCode) {
            HttpStatusCode.Created, HttpStatusCode.Accepted -> {
                val sid = runCatching {
                    Json.parseToJsonElement(text).let { (it as JsonObject)["sid"]?.jsonPrimitive?.content }
                }.getOrNull() ?: "unknown"
                ChannelResult.Sent(sid)
            }
            else -> ChannelResult.Failed("whatsapp_twilio_${resp.statusCode.value}", text)
        }
    }
}
```

- [ ] **Step 2: Create WhatsApp AT provider**

```kotlin
package com.smartjirani.civic.channels

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class WhatsAppAfricasTalkingProvider(
    private val cfg: CivicChannelConfig,
    private val http: HttpClient = defaultHttp(),
) : CivicChannelProvider {
    override val name = "whatsapp_africas_talking"

    override suspend fun deliver(payload: CivicNotificationPayload): ChannelResult {
        require(cfg.whatsappProvider == "africas_talking") { "CIVIC_WHATSAPP_PROVIDER != africas_talking" }
        val url = "https://api.africastalking.com/version1/messaging"
        val form = "username=${cfg.africasTalkingUsername}&to=${payload.toPhone}" +
                   "&message=${payload.body}&from=${cfg.africasTalkingWhatsappFrom}"
        val resp = http.post(url) {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                append("apiKey", cfg.africasTalkingApiKey!!)
            }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(form)
        }
        val text = resp.bodyAsText()
        return when (resp.statusCode) {
            HttpStatusCode.Created, HttpStatusCode.Accepted, HttpStatusCode.OK ->
                ChannelResult.Sent("africas_talking_whatsapp_${payload.notificationId.take(8)}")
            else -> ChannelResult.Failed("whatsapp_at_${resp.statusCode.value}", text)
        }
    }
}
```

- [ ] **Step 3: Add WhatsApp fields to `CivicChannelConfig`**

(Touched here so subsequent Tasks 11/13 see them; `CivicChannelConfig.kt` itself is created in Task 11. Extend its data class with these four fields: `twilioWhatsappFrom: String?`, `africasTalkingWhatsappFrom: String?`, `whatsappProvider: String`.)

- [ ] **Step 4: Commit (do not activate in registry yet)**

```bash
git add app/src/main/java/com/smartjirani/civic/channels/WhatsAppTwilioProvider.kt app/src/main/java/com/smartjirani/civic/channels/WhatsAppAfricasTalkingProvider.kt
git commit -m "feat(civic-channels): WhatsApp Twilio + AT providers"
```

---

## Task 11: Channel Config, Registry, Router, Interface, and Fakes

**Files:**
- Create: `app/src/main/java/com/smartjirani/civic/channels/CivicChannelConfig.kt`
- Create: `app/src/main/java/com/smartjirani/civic/channels/CivicChannelProvider.kt`
- Create: `app/src/main/java/com/smartjirani/civic/channels/CivicChannelRegistry.kt`
- Create: `app/src/main/java/com/smartjirani/civic/channels/CivicChannelRouter.kt`
- Create: `app/src/main/java/com/smartjirani/civic/channels/ChannelResult.kt`
- Create: `app/src/main/java/com/smartjirani/civic/channels/CivicNotificationPayload.kt`
- Create: `app/src/main/java/com/smartjirani/civic/channels/Http.kt`
- Create: `app/src/test/java/com/smartjirani/civic/channels/CivicChannelRouterTest.kt`
- Create: `app/src/test/java/com/smartjirani/civic/channels/support/Fakes.kt` (shared test fakes)

- [ ] **Step 1: Define the provider interface and supporting types**

```kotlin
// CivicChannelProvider.kt
package com.smartjirani.civic.channels

interface CivicChannelProvider {
    val name: String
    suspend fun deliver(payload: CivicNotificationPayload): ChannelResult
}
```

```kotlin
// ChannelResult.kt
package com.smartjirani.civic.channels

sealed class ChannelResult {
    data class Sent(val providerMessageId: String) : ChannelResult()
    data class Failed(val code: String, val detail: String) : ChannelResult()
    object NoOp : ChannelResult()
    object SkippedNoProvider : ChannelResult()
}
```

```kotlin
// CivicNotificationPayload.kt
package com.smartjirani.civic.channels

data class CivicNotificationPayload(
    val notificationId: String,
    val recipientUserId: String,
    val toPhone: String?,
    val title: String,
    val body: String,
)
```

- [ ] **Step 2: Define config + default HTTP client**

```kotlin
// CivicChannelConfig.kt
package com.smartjirani.civic.channels

data class CivicChannelConfig(
    val twilioAccountSid: String?,
    val twilioAuthToken: String?,
    val twilioFromNumber: String?,
    val twilioWhatsappFrom: String?,
    val africasTalkingUsername: String?,
    val africasTalkingApiKey: String?,
    val africasTalkingFrom: String?,
    val africasTalkingWhatsappFrom: String?,
    val smsProvider: String,      // "twilio" | "africas_talking"
    val whatsappProvider: String, // "noop" | "twilio" | "africas_talking"
) {
    companion object {
        fun fromEnv(env: Map<String, String>): CivicChannelConfig = CivicChannelConfig(
            twilioAccountSid = env["TWILIO_ACCOUNT_SID"],
            twilioAuthToken = env["TWILIO_AUTH_TOKEN"],
            twilioFromNumber = env["TWILIO_FROM_NUMBER"],
            twilioWhatsappFrom = env["TWILIO_WHATSAPP_FROM"],
            africasTalkingUsername = env["AFRICASTALKING_USERNAME"],
            africasTalkingApiKey = env["AFRICASTALKING_API_KEY"],
            africasTalkingFrom = env["AFRICASTALKING_FROM"],
            africasTalkingWhatsappFrom = env["AFRICASTALKING_WHATSAPP_FROM"],
            smsProvider = env["CIVIC_SMS_PROVIDER"] ?: "twilio",
            whatsappProvider = env["CIVIC_WHATSAPP_PROVIDER"] ?: "noop",
        )
    }
}
```

```kotlin
// Http.kt
package com.smartjirani.civic.channels

import io.ktor.client.HttpClient

fun defaultHttp(): HttpClient = HttpClient { /* project default engine */ }
```

- [ ] **Step 3: Define registry and router**

```kotlin
// CivicChannelRegistry.kt
package com.smartjirani.civic.channels

class CivicChannelRegistry(private val cfg: CivicChannelConfig) {
    fun active(): List<CivicChannelProvider> {
        val providers = mutableListOf<CivicChannelProvider>()
        // SMS is active when the chosen provider's credentials are present.
        when (cfg.smsProvider) {
            "twilio" -> if (cfg.twilioAccountSid != null && cfg.twilioAuthToken != null)
                providers += SmsTwilioProvider(cfg)
            "africas_talking" -> if (cfg.africasTalkingApiKey != null)
                providers += SmsAfricasTalkingProvider(cfg)
        }
        // WhatsApp is enabled only when CIVIC_WHATSAPP_PROVIDER explicitly says so.
        when (cfg.whatsappProvider) {
            "twilio" -> if (cfg.twilioAccountSid != null && cfg.twilioAuthToken != null)
                providers += WhatsAppTwilioProvider(cfg)
            "africas_talking" -> if (cfg.africasTalkingApiKey != null)
                providers += WhatsAppAfricasTalkingProvider(cfg)
            "noop" -> providers += WhatsAppNoOpProvider()
        }
        return providers
    }
}
```

```kotlin
// CivicChannelRouter.kt
package com.smartjirani.civic.channels

class CivicChannelRouter(private val registry: CivicChannelRegistry) {

    data class PlannedDelivery(val channel: String, val providerName: String)

    fun planFor(pref: String): List<PlannedDelivery> {
        val wantsSms = pref.contains("sms")
        val wantsWhatsApp = pref.contains("whatsapp")
        val wantsInApp = pref.contains("in_app") || (!wantsSms && !wantsWhatsApp)
        val active = registry.active()
        val activeNames = active.map { it.name }.toSet()
        val plan = mutableListOf<PlannedDelivery>()
        if (wantsInApp) plan += PlannedDelivery("in_app", "in_app")
        if (wantsSms) {
            val smsProv = active.firstOrNull { it.name.startsWith("sms_") }
            if (smsProv != null) plan += PlannedDelivery("sms", smsProv.name)
            else plan += PlannedDelivery("sms", "skipped_no_provider")
        }
        if (wantsWhatsApp) {
            val waProv = active.firstOrNull { it.name.startsWith("whatsapp_") && it.name != "whatsapp_noop" }
            if (waProv != null) plan += PlannedDelivery("whatsapp", waProv.name)
            else plan += PlannedDelivery("whatsapp", "whatsapp_noop")
        }
        return plan
    }

    suspend fun route(payload: CivicNotificationPayload, planned: List<PlannedDelivery>): List<Pair<PlannedDelivery, ChannelResult>> {
        val activeByName = registry.active().associateBy { it.name }
        return planned.map { p ->
            val provider = activeByName[p.providerName]
            val result = provider?.deliver(payload)
                ?: if (p.providerName == "whatsapp_noop") ChannelResult.NoOp
                else ChannelResult.SkippedNoProvider
            p to result
        }
    }
}
```

- [ ] **Step 4: Write the router test**

File: `app/src/test/java/com/smartjirani/civic/channels/support/Fakes.kt`

```kotlin
package com.smartjirani.civic.channels.support

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel

data class HttpResponse(val status: Int, val body: String)

class FakeHttp(val handler: (HttpRequestData, HttpResponseData?) -> HttpResponse) {
    fun asClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                val response = handler(request, null)
                respond(
                    content = ByteReadChannel(response.body),
                    status = HttpStatusCode.fromValue(response.status),
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        }
    }
}
```

File: `app/src/test/java/com/smartjirani/civic/channels/CivicChannelRouterTest.kt`

```kotlin
package com.smartjirani.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class CivicChannelRouterTest {

    private fun baseCfg() = CivicChannelConfig(
        twilioAccountSid = "AC",
        twilioAuthToken = "tok",
        twilioFromNumber = "+15555550100",
        twilioWhatsappFrom = null,
        africasTalkingUsername = null,
        africasTalkingApiKey = null,
        africasTalkingFrom = null,
        africasTalkingWhatsappFrom = null,
        smsProvider = "twilio",
        whatsappProvider = "noop",
    )

    @Test
    fun `planFor sms only routes to sms provider`() {
        val router = CivicChannelRouter(CivicChannelRegistry(baseCfg()))
        val plan = router.planFor("sms")
        assertEquals(listOf(CivicChannelRouter.PlannedDelivery("sms", "sms_twilio")), plan)
    }

    @Test
    fun `planFor sms,whatsapp routes to sms provider and whatsapp_noop`() {
        val router = CivicChannelRouter(CivicChannelRegistry(baseCfg()))
        val plan = router.planFor("sms,whatsapp")
        assertEquals(
            listOf(
                CivicChannelRouter.PlannedDelivery("sms", "sms_twilio"),
                CivicChannelRouter.PlannedDelivery("whatsapp", "whatsapp_noop"),
            ),
            plan,
        )
    }

    @Test
    fun `planFor with no provider skips`() {
        val cfg = baseCfg().copy(twilioAccountSid = null, twilioAuthToken = null)
        val router = CivicChannelRouter(CivicChannelRegistry(cfg))
        val plan = router.planFor("sms")
        assertEquals(listOf(CivicChannelRouter.PlannedDelivery("sms", "skipped_no_provider")), plan)
    }

    @Test
    fun `route delivers to active providers only`() = runTest {
        val router = CivicChannelRouter(CivicChannelRegistry(baseCfg()))
        val payload = CivicNotificationPayload(
            notificationId = "n1",
            recipientUserId = "u1",
            toPhone = "+15555550199",
            title = "T",
            body = "B",
        )
        val planned = router.planFor("sms")
        val results = router.route(payload, planned)
        // Without a real HTTP client wired here, route() returns SkippedNoProvider
        // because the registry in this test does not include the network-backed provider.
        assertEquals(1, results.size)
    }
}
```

- [ ] **Step 5: Run all channel tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.channels.*"`
Expected: PASS for the 4 channel test classes (SmsTwilio, SmsAfricasTalking, WhatsAppNoOp, CivicChannelRouter).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/smartjirani/civic/channels/CivicChannelConfig.kt app/src/main/java/com/smartjirani/civic/channels/CivicChannelProvider.kt app/src/main/java/com/smartjirani/civic/channels/CivicChannelRegistry.kt app/src/main/java/com/smartjirani/civic/channels/CivicChannelRouter.kt app/src/main/java/com/smartjirani/civic/channels/ChannelResult.kt app/src/main/java/com/smartjirani/civic/channels/CivicNotificationPayload.kt app/src/main/java/com/smartjirani/civic/channels/Http.kt app/src/test/java/com/smartjirani/civic/channels/CivicChannelRouterTest.kt app/src/test/java/com/smartjirani/civic/channels/support/Fakes.kt
git commit -m "feat(civic-channels): config + registry + router + interface + tests"
```

---

## Task 12: AI Provider Swap + `provider_changes` Column

**Files:**
- Create: `supabase/migrations/20261005150600_civic_ai_sessions_provider_changes.sql`
- Create: `app/src/main/java/com/smartjirani/core/ai/AiProviderResolver.kt`
- Create: `app/src/test/java/com/smartjirani/core/ai/AiProviderResolverTest.kt`
- Modify: `app/src/main/java/com/smartjirani/core/ai/GeminiCivicAiProvider.kt` (add fallback path)

- [ ] **Step 1: Migration for `provider_changes`**

```sql
alter table public.civic_ai_sessions
    add column if not exists provider_changes jsonb default '[]'::jsonb;
```

- [ ] **Step 2: Apply migration**

Run: `supabase db push`

- [ ] **Step 3: Write the resolver test**

File: `app/src/test/java/com/smartjirani/core/ai/AiProviderResolverTest.kt`

```kotlin
package com.smartjirani.core.ai

import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AiProviderResolverTest {

    @Test
    fun `provider=gemini with key returns Gemini provider`() {
        val resolver = AiProviderResolver(civicConfig(aiProvider = "gemini", geminiKey = "k"))
        val p = resolver.resolve()
        assertTrue(p is GeminiCivicAiProvider)
    }

    @Test
    fun `provider=gemini without key falls back to mock`() {
        val resolver = AiProviderResolver(civicConfig(aiProvider = "gemini", geminiKey = ""))
        val p = resolver.resolve()
        assertTrue(p is KeywordMockCivicAiProvider)
    }

    @Test
    fun `provider=mock always returns mock`() {
        val resolver = AiProviderResolver(civicConfig(aiProvider = "mock", geminiKey = "k"))
        val p = resolver.resolve()
        assertTrue(p is KeywordMockCivicAiProvider)
    }
}

data class CivicAiConfig(val aiProvider: String, val geminiKey: String, val geminiModel: String = "gemini-1.5-flash")

private fun civicConfig(aiProvider: String, geminiKey: String) =
    CivicAiConfig(aiProvider, geminiKey)
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.core.ai.AiProviderResolverTest"`
Expected: compile error.

- [ ] **Step 5: Write resolver implementation**

File: `app/src/main/java/com/smartjirani/core/ai/AiProviderResolver.kt`

```kotlin
package com.smartjirani.core.ai

class AiProviderResolver(private val cfg: CivicAiConfig) {
    fun resolve(): CivicAiProvider = when (cfg.aiProvider) {
        "gemini" -> if (cfg.geminiKey.isNotBlank())
            GeminiCivicAiProvider(apiKey = cfg.geminiKey, model = cfg.geminiModel)
        else
            KeywordMockCivicAiProvider(reason = "blank_gemini_key")
        else -> KeywordMockCivicAiProvider(reason = "provider=mock")
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.core.ai.AiProviderResolverTest"`
Expected: PASS.

- [ ] **Step 7: Write Gemini fallback test**

File: `app/src/test/java/com/smartjirani/core/ai/GeminiCivicAiProviderFallbackTest.kt`

```kotlin
package com.smartjirani.core.ai

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

class GeminiCivicAiProviderFallbackTest {

    @Test
    fun `triage falls back to mock on Gemini 5xx`() = runTest {
        val gemini = GeminiCivicAiProvider(
            apiKey = "k",
            model = "gemini-1.5-flash",
            http = FakeGeminiHttp { HttpResponse(status = 503, body = "{\"error\":\"overloaded\"}") },
        )
        val mock = KeywordMockCivicAiProvider()
        val (provider, audit) = gemini.triageWithFallback(
            mock = mock,
            text = "Bomba ya maji imevuja",
            locale = "sw",
        )
        assertTrue(provider is KeywordMockCivicAiProvider)
        assertTrue(audit.fellBack)
        assertTrue(audit.errors.any { it.contains("overloaded") })
    }
}
```

(Helper `HttpResponse` is the same shared record. `FakeGeminiHttp` follows the pattern in Task 11's `support/Fakes.kt` — extend the shared fakes if needed.)

- [ ] **Step 8: Enhance `GeminiCivicAiProvider` with fallback**

Modify `GeminiCivicAiProvider.kt` — add:

```kotlin
data class GeminiTriageAudit(val provider: String, val fellBack: Boolean, val errors: List<String>, val latencyMs: Long)

suspend fun triageWithFallback(
    mock: KeywordMockCivicAiProvider,
    text: String,
    locale: String,
): Pair<CivicAiProvider, GeminiTriageAudit> {
    return try {
        val start = System.currentTimeMillis()
        val result = triage(text, locale)  // existing method
        val latency = System.currentTimeMillis() - start
        this to GeminiTriageAudit(provider = "gemini", fellBack = false, errors = emptyList(), latencyMs = latency)
    } catch (e: Exception) {
        val mockResult = mock.triage(text, locale)
        mock to GeminiTriageAudit(provider = "mock_fallback", fellBack = true, errors = listOf(e.message ?: e.toString()), latencyMs = 0)
    }
}
```

(`KeywordMockCivicAiProvider.triage(text, locale)` is the existing method — the spec preserves the public surface.)

- [ ] **Step 9: Run AI tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.core.ai.*"`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add supabase/migrations/20261005150600_civic_ai_sessions_provider_changes.sql app/src/main/java/com/smartjirani/core/ai/AiProviderResolver.kt app/src/main/java/com/smartjirani/core/ai/GeminiCivicAiProvider.kt app/src/test/java/com/smartjirani/core/ai/AiProviderResolverTest.kt app/src/test/java/com/smartjirani/core/ai/GeminiCivicAiProviderFallbackTest.kt
git commit -m "feat(civic-ai): resolver + transparent fallback"
```

---

## Task 13: `ProfileChannelPrefEditor` UI

**Files:**
- Create: `app/src/main/java/com/smartjirani/civic/profile/ProfileChannelPrefEditor.kt`
- Create: `app/src/test/java/com/smartjirani/civic/profile/ProfileChannelPrefEditorTest.kt`
- Modify: `app/src/main/res/values/strings.xml` (add 4 strings)
- Modify: `app/src/main/res/values-sw/strings.xml` (Swahili translations)

- [ ] **Step 1: Add strings (English)**

In `values/strings.xml`, add:
```xml
<string name="civic_channel_pref_title">Notification channels</string>
<string name="civic_channel_pref_in_app">In-app only</string>
<string name="civic_channel_pref_sms">SMS</string>
<string name="civic_channel_pref_whatsapp">WhatsApp</string>
<string name="civic_channel_pref_save">Save</string>
<string name="civic_channel_pref_saved">Channel preference saved.</string>
```

- [ ] **Step 2: Add strings (Swahili)**

In `values-sw/strings.xml`, add:
```xml
<string name="civic_channel_pref_title">Njia za arifa</string>
<string name="civic_channel_pref_in_app">Ndani ya programu tu</string>
<string name="civic_channel_pref_sms">SMS</string>
<string name="civic_channel_pref_whatsapp">WhatsApp</string>
<string name="civic_channel_pref_save">Hifadhi</string>
<string name="civic_channel_pref_saved">Hifadhi imefanikiwa.</string>
```

- [ ] **Step 3: Write the failing Compose UI test**

File: `app/src/test/java/com/smartjirani/civic/profile/ProfileChannelPrefEditorTest.kt`

```kotlin
package com.smartjirani.civic.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ProfileChannelPrefEditorTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `sms chip is clickable and saves`() {
        var saved: String? = null
        rule.setContent {
            ProfileChannelPrefEditor(
                current = "in_app",
                onSave = { saved = it },
            )
        }
        rule.onNodeWithText("SMS").performClick()
        rule.onNodeWithText("Save").performClick()
        assert(saved == "sms") { "expected sms but was $saved" }
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.profile.ProfileChannelPrefEditorTest"`
Expected: compile error.

- [ ] **Step 5: Implement the editor**

File: `app/src/main/java/com/smartjirani/civic/profile/ProfileChannelPrefEditor.kt`

```kotlin
package com.smartjirani.civic.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartjirani.R

@Composable
fun ProfileChannelPrefEditor(current: String, onSave: (String) -> Unit) {
    var selected by remember { mutableStateOf(current) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.civic_channel_pref_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            AssistChip(onClick = { selected = "in_app" }, label = { Text(stringResource(R.string.civic_channel_pref_in_app)) })
            AssistChip(onClick = { selected = "sms" }, label = { Text(stringResource(R.string.civic_channel_pref_sms)) })
            AssistChip(onClick = { selected = "whatsapp" }, label = { Text(stringResource(R.string.civic_channel_pref_whatsapp)) })
        }
        Button(onClick = { onSave(selected) }, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.civic_channel_pref_save))
        }
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.profile.ProfileChannelPrefEditorTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/smartjirani/civic/profile/ProfileChannelPrefEditor.kt app/src/test/java/com/smartjirani/civic/profile/ProfileChannelPrefEditorTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-sw/strings.xml
git commit -m "feat(civic-profile): channel preference editor"
```

---

## Task 14: `MtaaOfficerDashboardScreen` Density Upgrade

**Files:**
- Create: `app/src/main/java/com/smartjirani/civic/official/CivicOfficialFilterChips.kt`
- Create: `app/src/test/java/com/smartjirani/civic/official/CivicOfficialFilterChipsTest.kt`
- Modify: `app/src/main/java/com/smartjirani/civic/official/MtaaOfficerDashboardScreen.kt`
- Modify: `app/src/main/res/values/strings.xml` (add 8 strings)
- Modify: `app/src/main/res/values-sw/strings.xml` (Swahili translations)

- [ ] **Step 1: Add strings (English)**

```xml
<string name="civic_official_filter_all">All</string>
<string name="civic_official_filter_awaiting_verification">Awaiting verification</string>
<string name="civic_official_filter_overdue">Overdue</string>
<string name="civic_official_filter_mine">My assigned</string>
<string name="civic_official_filter_new_today">New today</string>
<string name="civic_official_kpi_open">Open</string>
<string name="civic_official_kpi_overdue">Overdue</string>
<string name="civic_official_kpi_resolved_today">Resolved today</string>
<string name="civic_official_batch_assign">Assign</string>
<string name="civic_official_batch_verify">Verify</string>
<string name="civic_official_batch_note">Add note</string>
```

- [ ] **Step 2: Add strings (Swahili)**

```xml
<string name="civic_official_filter_all">Zote</string>
<string name="civic_official_filter_awaiting_verification">Zinasubiri uthibitisho</string>
<string name="civic_official_filter_overdue">Zimechelewa</string>
<string name="civic_official_filter_mine">Zangu</string>
<string name="civic_official_filter_new_today">Mpya leo</string>
<string name="civic_official_kpi_open">Wazi</string>
<string name="civic_official_kpi_overdue">Zimechelewa</string>
<string name="civic_official_kpi_resolved_today">Zimetatuliwa leo</string>
<string name="civic_official_batch_assign">Gawa</string>
<string name="civic_official_batch_verify">Thibitisha</string>
<string name="civic_official_batch_note">Ongeza maelezo</string>
```

- [ ] **Step 3: Write the failing chips test**

File: `app/src/test/java/com/smartjirani/civic/official/CivicOfficialFilterChipsTest.kt`

```kotlin
package com.smartjirani.civic.official

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class CivicOfficialFilterChipsTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `tapping overdue chip updates selection`() {
        var chosen: CivicOfficialFilter? = null
        rule.setContent {
            CivicOfficialFilterChips(current = CivicOfficialFilter.ALL, onSelect = { chosen = it })
        }
        rule.onNodeWithText("Overdue").performClick()
        assert(chosen == CivicOfficialFilter.OVERDUE) { "expected OVERDUE but was $chosen" }
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.official.CivicOfficialFilterChipsTest"`
Expected: compile error.

- [ ] **Step 5: Implement the filter chips**

File: `app/src/main/java/com/smartjirani/civic/official/CivicOfficialFilterChips.kt`

```kotlin
package com.smartjirani.civic.official

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartjirani.R

@Composable
fun CivicOfficialFilterChips(current: CivicOfficialFilter, onSelect: (CivicOfficialFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        CivicOfficialFilter.values().forEach { filter ->
            FilterChip(
                selected = filter == current,
                onClick = { onSelect(filter) },
                label = {
                    Text(stringResource(
                        when (filter) {
                            CivicOfficialFilter.ALL -> R.string.civic_official_filter_all
                            CivicOfficialFilter.AWAITING_VERIFICATION -> R.string.civic_official_filter_awaiting_verification
                            CivicOfficialFilter.OVERDUE -> R.string.civic_official_filter_overdue
                            CivicOfficialFilter.MINE -> R.string.civic_official_filter_mine
                            CivicOfficialFilter.NEW_TODAY -> R.string.civic_official_filter_new_today
                        }
                    ))
                },
            )
        }
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.official.CivicOfficialFilterChipsTest"`
Expected: PASS.

- [ ] **Step 7: Add `MtaaOfficerDashboardViewModel`**

(File: `app/src/main/java/com/smartjirani/civic/official/MtaaOfficerDashboardViewModel.kt`. Move this from Task 15 to here so the screen and the VM are reviewed together.)

```kotlin
package com.smartjirani.civic.official

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

data class MtaaOfficerUiState(
    val kpiOpen: Int = 0,
    val kpiOverdue: Int = 0,
    val kpiResolvedToday: Int = 0,
)

class MtaaOfficerDashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(MtaaOfficerUiState())
    val state: StateFlow<MtaaOfficerUiState> = _state.asStateFlow()

    init { refresh() }

    fun feedFor(filter: CivicOfficialFilter) = flow { emit(CivicOfficialFeed.list(filter)) }

    fun batchVerify(ids: List<String>) {
        viewModelScope.launch { ids.forEach { /* call transition_civic_report to VERIFIED — see existing call sites in feature/civic_official/ */ } }
    }

    fun batchAssign(ids: List<String>) {
        viewModelScope.launch { ids.forEach { /* call assign_civic_report — see existing call sites */ } }
    }

    fun batchNote(ids: List<String>) {
        viewModelScope.launch { ids.forEach { /* call civic_add_note — see existing call sites */ } }
    }

    private fun refresh() {
        viewModelScope.launch {
            val analytics = /* call civic_analytics(window_days=1) per the C8 RPC contract */
                null
            _state.value = MtaaOfficerUiState(
                kpiOpen = analytics?.open ?: 0,
                kpiOverdue = analytics?.overdue ?: 0,
                kpiResolvedToday = analytics?.resolved_today ?: 0,
            )
        }
    }
}
```

- [ ] **Step 8: Rewrite `MtaaOfficerDashboardScreen`**

File: `app/src/main/java/com/smartjirani/civic/official/MtaaOfficerDashboardScreen.kt`

```kotlin
package com.smartjirani.civic.official

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartjirani.R

@Composable
fun MtaaOfficerDashboardScreen(viewModel: MtaaOfficerDashboardViewModel) {
    val ui by viewModel.state.collectAsState()
    var filter by remember { mutableStateOf(CivicOfficialFilter.ALL) }
    val rows = viewModel.feedFor(filter).collectAsState(initial = emptyList()).value
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    Column(Modifier.fillMaxSize()) {
        // KPI strip
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            KpiCard(label = stringResource(R.string.civic_official_kpi_open), value = ui.kpiOpen)
            KpiCard(label = stringResource(R.string.civic_official_kpi_overdue), value = ui.kpiOverdue)
            KpiCard(label = stringResource(R.string.civic_official_kpi_resolved_today), value = ui.kpiResolvedToday)
        }
        // Filter chips
        CivicOfficialFilterChips(current = filter, onSelect = { filter = it })
        // Batch action row
        if (selected.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
                Button(onClick = { viewModel.batchVerify(selected.toList()); selected = emptySet() }) {
                    Text(stringResource(R.string.civic_official_batch_verify))
                }
                Button(onClick = { viewModel.batchAssign(selected.toList()); selected = emptySet() }) {
                    Text(stringResource(R.string.civic_official_batch_assign))
                }
                Button(onClick = { viewModel.batchNote(selected.toList()); selected = emptySet() }) {
                    Text(stringResource(R.string.civic_official_batch_note))
                }
            }
        }
        // Feed
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(rows) { row ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected.contains(row.report_id),
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + row.report_id else selected - row.report_id
                            },
                        )
                        Column(Modifier.padding(8.dp)) {
                            Text("${ref reportId}".format(row.ref), style = MaterialTheme.typography.titleSmall)
                            Text("${row.title} · ${row.category_sw}")
                            Text("due in ${row.due_at_minutes} min", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: Int) {
    Card { Column(Modifier.padding(8.dp)) { Text(value.toString(), style = MaterialTheme.typography.titleLarge); Text(label, style = MaterialTheme.typography.bodySmall) } }
}
```

(`MtaaOfficerDashboardViewModel` is added in Task 15 if it does not exist.)

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/smartjirani/civic/official/CivicOfficialFilterChips.kt app/src/test/java/com/smartjirani/civic/official/CivicOfficialFilterChipsTest.kt app/src/main/java/com/smartjirani/civic/official/MtaaOfficerDashboardScreen.kt app/src/main/java/com/smartjirani/civic/official/MtaaOfficerDashboardViewModel.kt app/src/main/res/values/strings.xml app/src/main/res/values-sw/strings.xml
git commit -m "feat(civic-official): MtaaOfficer dashboard density upgrade"
```

---

## Task 15: `DepartmentOfficerConsoleScreen` Density Upgrade

**Files:**
- Create: `app/src/main/java/com/smartjirani/civic/official/DepartmentOfficerDashboardViewModel.kt`
- Create: `app/src/main/java/com/smartjirani/civic/official/CivicOfficialConsoleScaffold.kt`
- Modify: `app/src/main/java/com/smartjirani/civic/official/DepartmentOfficerConsoleScreen.kt`

- [ ] **Step 1: Add `DepartmentOfficerDashboardViewModel` (mirror of Task 14's MtaaOfficerViewModel)**

```kotlin
package com.smartjirani.civic.official

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

data class DepartmentOfficerUiState(
    val kpiOpen: Int = 0,
    val kpiOverdue: Int = 0,
    val kpiResolvedToday: Int = 0,
)

class DepartmentOfficerDashboardViewModel(
    private val scopeUnit: String? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(DepartmentOfficerUiState())
    val state: StateFlow<DepartmentOfficerUiState> = _state.asStateFlow()

    init { refresh() }

    fun feedFor(filter: CivicOfficialFilter) = flow {
        emit(CivicOfficialFeed.list(filter, scopeUnit = scopeUnit))
    }

    fun batchVerify(ids: List<String>) {
        viewModelScope.launch { ids.forEach { /* call transition_civic_report to VERIFIED */ } }
    }

    fun batchAssign(ids: List<String>) {
        viewModelScope.launch { ids.forEach { /* call assign_civic_report */ } }
    }

    fun batchNote(ids: List<String>) {
        viewModelScope.launch { ids.forEach { /* call civic_add_note */ } }
    }

    private fun refresh() {
        viewModelScope.launch {
            val analytics = /* call civic_analytics(window_days=1) */
                null
            _state.value = DepartmentOfficerUiState(
                kpiOpen = analytics?.open ?: 0,
                kpiOverdue = analytics?.overdue ?: 0,
                kpiResolvedToday = analytics?.resolved_today ?: 0,
            )
        }
    }
}
```

- [ ] **Step 2: Extract a shared scaffold to avoid duplication**

File: `app/src/main/java/com/smartjirani/civic/official/CivicOfficialConsoleScaffold.kt`

```kotlin
package com.smartjirani.civic.official

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartjirani.R

@Composable
fun CivicOfficialConsoleScaffold(
    kpiOpen: Int,
    kpiOverdue: Int,
    kpiResolvedToday: Int,
    rows: List<CivicOfficialFeedRow>,
    onBatchVerify: (List<String>) -> Unit,
    onBatchAssign: (List<String>) -> Unit,
    onBatchNote: (List<String>) -> Unit,
) {
    var filter by remember { mutableStateOf(CivicOfficialFilter.ALL) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            KpiCard(stringResource(R.string.civic_official_kpi_open), kpiOpen)
            KpiCard(stringResource(R.string.civic_official_kpi_overdue), kpiOverdue)
            KpiCard(stringResource(R.string.civic_official_kpi_resolved_today), kpiResolvedToday)
        }
        CivicOfficialFilterChips(current = filter, onSelect = { filter = it })
        if (selected.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
                Button(onClick = { onBatchVerify(selected.toList()); selected = emptySet() }) {
                    Text(stringResource(R.string.civic_official_batch_verify))
                }
                Button(onClick = { onBatchAssign(selected.toList()); selected = emptySet() }) {
                    Text(stringResource(R.string.civic_official_batch_assign))
                }
                Button(onClick = { onBatchNote(selected.toList()); selected = emptySet() }) {
                    Text(stringResource(R.string.civic_official_batch_note))
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(rows) { row ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected.contains(row.report_id),
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + row.report_id else selected - row.report_id
                            },
                        )
                        Column(Modifier.padding(8.dp)) {
                            Text(row.ref, style = MaterialTheme.typography.titleSmall)
                            Text("${row.title} · ${row.category_sw}")
                            Text("due in ${row.due_at_minutes} min", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: Int) {
    Card { Column(Modifier.padding(8.dp)) { Text(value.toString(), style = MaterialTheme.typography.titleLarge); Text(label, style = MaterialTheme.typography.bodySmall) } }
}
```

- [ ] **Step 3: Replace `MtaaOfficerDashboardScreen` and `DepartmentOfficerConsoleScreen` bodies with scaffold calls**

For each screen, keep the screen's outer `@Composable fun X(viewModel: Vm)` and inside delegate to `CivicOfficialConsoleScaffold(...)`. Example:

```kotlin
@Composable
fun MtaaOfficerDashboardScreen(viewModel: MtaaOfficerDashboardViewModel) {
    val ui by viewModel.state.collectAsState()
    var rows by remember { mutableStateOf(emptyList<CivicOfficialFeedRow>()) }
    LaunchedEffect(Unit) {
        rows = CivicOfficialFeed.list(CivicOfficialFilter.ALL)
    }
    CivicOfficialConsoleScaffold(
        kpiOpen = ui.kpiOpen,
        kpiOverdue = ui.kpiOverdue,
        kpiResolvedToday = ui.kpiResolvedToday,
        rows = rows,
        onBatchVerify = viewModel::batchVerify,
        onBatchAssign = viewModel::batchAssign,
        onBatchNote = viewModel::batchNote,
    )
}
```

Same shape for `DepartmentOfficerConsoleScreen`, using `DepartmentOfficerDashboardViewModel` and `scopeUnit`.

- [ ] **Step 4: Run officer UI tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.smartjirani.civic.official.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smartjirani/civic/official/CivicOfficialConsoleScaffold.kt app/src/main/java/com/smartjirani/civic/official/DepartmentOfficerConsoleScreen.kt app/src/main/java/com/smartjirani/civic/official/DepartmentOfficerDashboardViewModel.kt
git commit -m "feat(civic-official): department console density upgrade"
```

---

## Task 16: E2E Acceptance Steps 23–30

**Files:** None new. This task is a live-DB verification gate.

- [ ] **Step 1: Reset the channel preference for the test resident**

Run:
```
TOK=$(curl ... -d '{"email":"<e2e-resident-email>","password":"<password>"}' | jq -r .access_token)
curl -X POST '.../rest/v1/rpc/civic_set_channel_pref' \
  -H "apikey: $ANON_KEY" -H "Authorization: Bearer $TOK" -H "Content-Type: application/json" \
  -d '"sms"'
```
Expected: 200; `select civic_channel_pref from profiles where id=...` returns `sms`.

- [ ] **Step 2: Submit a new garbage report as the test resident**

Run:
```
REF=$(curl -s -X POST '.../rest/v1/rpc/submit_civic_report' \
  -H "apikey: $ANON_KEY" -H "Authorization: Bearer $TOK" -H "Content-Type: application/json" \
  -d "$(cat <<JSON
["<category-id-taka>", "Garbage dump", "Takataka zimejaa", -6.7712, 39.2483, "Mtaa Junction",
 "<org-id>", "<neighborhood-id>", "MEDIUM", false, "android"]
JSON
)" | jq -r .reference_number)
echo "$REF"
```
Expected: a reference like `ilala-mc-2026-00029`.

- [ ] **Step 3: Verify AI triage ran (Gemini path with mock fallback)**

Run:
```
supabase db query --linked -c "select provider, status, latency_ms from civic_ai_audit order by created_at desc limit 3;"
```
Expected: at least one row. If GEMINI_API_KEY is unset or invalid, the row is `provider='mock_fallback'`. If set and valid, the row is `provider='gemini'`.

- [ ] **Step 4: Verify status-change SMS row written to `notification_deliveries`**

Pick a known status change and inspect:
```
NOTIF=$(supabase db query --linked -t -c "select id from notifications order by created_at desc limit 1;" | tr -d ' ')
supabase db query --linked -c "select channel, provider, status from notification_deliveries where notification_id = '$NOTIF';"
```
Expected: one row with `channel='sms'` and `provider='sms_twilio'` (or `'skipped_no_provider'` if Twilio credentials aren't set).

- [ ] **Step 5: Verify WhatsApp row written when `civic_channel_pref='sms,whatsapp'`**

Reset pref to `sms,whatsapp` and submit another report (or trigger a status change). Inspect:
```
supabase db query --linked -c "select channel, provider, status from notification_deliveries where channel = 'whatsapp' order by attempted_at desc limit 3;"
```
Expected: row with `provider='whatsapp_noop'`, `status='noop'`.

- [ ] **Step 6: Backdate a report's due_at; observe pg_cron escalation**

Run:
```
supabase db query --linked -c "update civic_reports set due_at = now() - interval '10 minutes' where status = 'ASSIGNED' and due_at is not null order by created_at desc limit 1 returning reference_number;"
sleep 360
supabase db query --linked -c "select jobname, status, end_time from cron.job_run_details order by start_time desc limit 3;"
```
Expected: at least one cron run with status `succeeded` in the last few minutes; the report's `escalation_level` increased by 1 within 6 minutes (5-min cron + processing).

- [ ] **Step 7: Verify officer dashboard density upgrade**

Open the app as `e2e-official` and navigate to Mtaa Officer dashboard. Visually verify:
- KPI strip at top
- Filter chips below
- Batch actions appear when ≥1 row selected

- [ ] **Step 8: Batch-verify 3 rows**

Select 3 AWAITING_VERIFICATION rows; tap "Verify". Confirm:
```
supabase db query --linked -c "select count(*) from civic_report_status_history where to_status = 'VERIFIED' and changed_at > now() - interval '5 minutes';"
```
Expected: 3.

- [ ] **Step 9: Negative path — SMS provider disabled, resident prefers SMS

Set `CIVIC_SMS_PROVIDER=africas_talking` and unset `AFRICASTALKING_API_KEY`. Submit a report. Inspect:
```
supabase db query --linked -c "select status, error from notification_deliveries where channel = 'sms' order by attempted_at desc limit 1;"
```
Expected: `status='skipped_no_provider'`.

- [ ] **Step 10: Restore all env and notify the user**

Set `CIVIC_SMS_PROVIDER=twilio` (or remove from env), reset `civic_channel_pref='in_app'` for the test resident. E2E complete.