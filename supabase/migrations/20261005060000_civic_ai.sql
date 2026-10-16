-- ============================================================
-- 032: CIVIC AI — comms log + assistant tool RPCs.
-- ai_comms: every AI interaction is logged (who, what, provider,
-- latency) for transparency and abuse review. Inserted only by the
-- service role (edge function); users read their own history.
-- civic_can_manage_report: scope probe used by the app + edge function.
-- civic_assistant_context / civic_assistant_search: bounded, RLS-safe
-- tools the assistant may use — officials get scoped data, residents
-- only their own reports.
-- ============================================================

create table if not exists public.ai_comms (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.profiles(id) on delete set null,
    organization_id uuid references public.organizations(id) on delete set null,
    kind text not null check (kind in ('TRIAGE','ASSISTANT','SUMMARY')),
    prompt_summary text not null,
    response_summary text,
    provider text not null default 'gemini',
    model text,
    mock boolean not null default false,
    latency_ms integer,
    created_at timestamptz not null default now()
);

create index if not exists idx_ai_comms_user on public.ai_comms (user_id, created_at desc);
create index if not exists idx_ai_comms_org on public.ai_comms (organization_id, created_at desc);

alter table public.ai_comms enable row level security;

drop policy if exists "ai_comms_select_own" on public.ai_comms;
create policy "ai_comms_select_own" on public.ai_comms
    for select to authenticated
    using ((select auth.uid()) = user_id);

-- ------------------------------------------------------------
-- Scope probe: can the caller act on this report (official scope)?
-- ------------------------------------------------------------
create or replace function public.civic_can_manage_report(p_report_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.civic_reports r
        where r.id = p_report_id
          and public.civic_has_report_scope(
              (select auth.uid()), r.organization_id, r.unit_id, r.assigned_unit_id)
    )
$$;

-- ------------------------------------------------------------
-- Bounded context for the assistant: roles, own recent reports, and
-- for officials a scoped open/overdue count per org.
-- ------------------------------------------------------------
create or replace function public.civic_assistant_context()
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_role_count integer;
begin
    if v_user is null then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;

    select count(*) into v_role_count from public.civic_active_roles(v_user);

    return jsonb_build_object(
        'ok', true,
        'is_official', v_role_count > 0,
        'roles', (
            select coalesce(jsonb_agg(jsonb_build_object(
                'role', ar.role,
                'organization_id', ar.organization_id,
                'org_unit_id', ar.org_unit_id)), '[]'::jsonb)
            from public.civic_active_roles(v_user) ar
        ),
        'my_recent_reports', (
            select coalesce(jsonb_agg(jsonb_build_object(
                'reference', r.public_reference,
                'status', r.status,
                'title', r.title,
                'severity', r.severity,
                'created_at', r.created_at)
                order by r.created_at desc), '[]'::jsonb)
            from (
                select public_reference, status, title, severity, created_at
                from public.civic_reports
                where reporter_id = v_user
                order by created_at desc
                limit 5
            ) r
        ),
        'org_stats', case when v_role_count = 0 then '[]'::jsonb else (
            select coalesce(jsonb_agg(jsonb_build_object(
                'organization_id', g.organization_id,
                'open_reports', g.open_reports,
                'overdue', g.overdue)), '[]'::jsonb)
            from (
                select r.organization_id,
                    count(*) filter (where public.civic_has_report_scope(
                            v_user, r.organization_id, r.unit_id, r.assigned_unit_id)
                        and r.status not in ('RESOLVED','CLOSED','REJECTED')) as open_reports,
                    count(*) filter (where public.civic_has_report_scope(
                            v_user, r.organization_id, r.unit_id, r.assigned_unit_id)
                        and r.due_at < now()
                        and r.status in ('ASSIGNED','IN_PROGRESS','WAITING_EXTERNAL_ACTION')) as overdue
                from public.civic_reports r
                where r.organization_id in (
                    select organization_id from public.civic_active_roles(v_user))
                group by r.organization_id
            ) g
        ) end
    );
end $$;

-- ------------------------------------------------------------
-- Scoped search for the assistant: residents search only their own
-- reports; officials additionally search reports inside their scope.
-- Unverified titles stay hidden from anyone without a stake.
-- ------------------------------------------------------------
create or replace function public.civic_assistant_search(p_query text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_q text := btrim(coalesce(p_query, ''));
begin
    if v_user is null then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;
    if length(v_q) < 2 then
        return jsonb_build_object('ok', true, 'results', '[]'::jsonb);
    end if;

    return jsonb_build_object(
        'ok', true,
        'results', (
            select coalesce(jsonb_agg(jsonb_build_object(
                'id', x.id,
                'reference', x.reference,
                'status', x.status,
                'title', x.title,
                'severity', x.severity,
                'category_sw', x.category_sw,
                'created_at', x.created_at,
                'is_own', x.is_own
            ) order by x.created_at desc), '[]'::jsonb)
            from (
                select r.id,
                       r.public_reference as reference,
                       r.status,
                       case
                           when r.verification_status = 'VERIFIED'
                             or r.reporter_id = v_user
                             or r.organization_id in (
                                 select organization_id from public.civic_active_roles(v_user))
                           then r.title
                           else null end as title,
                       r.severity,
                       c.name_sw as category_sw,
                       r.created_at,
                       r.reporter_id = v_user as is_own
                from public.civic_reports r
                join public.civic_categories c on c.id = r.category_id
                where (r.reporter_id = v_user
                       or (r.organization_id in (
                               select organization_id from public.civic_active_roles(v_user))
                           and public.civic_has_report_scope(
                               v_user, r.organization_id, r.unit_id, r.assigned_unit_id)))
                  and (r.public_reference ilike '%' || v_q || '%'
                       or r.title ilike '%' || v_q || '%'
                       or r.description ilike '%' || v_q || '%')
                order by r.created_at desc
                limit 8
            ) x
        )
    );
end $$;

revoke all on function public.civic_can_manage_report(uuid) from public, anon;
revoke all on function public.civic_assistant_context() from public, anon;
revoke all on function public.civic_assistant_search(text) from public, anon;
grant execute on function public.civic_can_manage_report(uuid) to authenticated, service_role;
grant execute on function public.civic_assistant_context() to authenticated, service_role;
grant execute on function public.civic_assistant_search(text) to authenticated, service_role;
