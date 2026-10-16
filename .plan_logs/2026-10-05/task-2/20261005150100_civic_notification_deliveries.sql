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