-- ============================================================
-- 030: CIVIC SLA + ESCALATION ENGINE
-- pg_cron-driven overdue scanner. Resolution breaches escalate one
-- tier up the org chain (SLA_BREACH) through the same state machine
-- officials use (SYSTEM transitions seeded here — no side door);
-- triage breaches get a one-time response reminder. Top-of-chain
-- breaches get a 48h grace window + council-admin notification.
-- ============================================================

create extension if not exists pg_cron;

alter table public.civic_reports
    add column if not exists response_reminded_at timestamptz;

create index if not exists idx_civic_reports_overdue on public.civic_reports(due_at)
    where due_at is not null
      and status in ('ASSIGNED', 'IN_PROGRESS', 'WAITING_EXTERNAL_ACTION');

create index if not exists idx_civic_reports_response_scan on public.civic_reports(created_at)
    where response_reminded_at is null
      and status in ('NEW', 'AI_TRIAGED', 'AWAITING_VERIFICATION', 'VERIFIED');

-- ------------------------------------------------------------
-- SYSTEM transitions into ESCALATED so the engine escalates via the
-- state machine instead of bypassing it.
-- ------------------------------------------------------------
insert into public.civic_transitions (from_status, to_status, actor_kind, allowed_roles)
select v.from_status, 'ESCALATED', 'SYSTEM', null::text[]
from (values ('ASSIGNED'), ('IN_PROGRESS'), ('WAITING_EXTERNAL_ACTION')) as v(from_status)
where not exists (
    select 1 from public.civic_transitions t
    where t.from_status = v.from_status
      and t.to_status = 'ESCALATED'
      and t.actor_kind = 'SYSTEM'
);

-- ------------------------------------------------------------
-- ESCALATION CHAIN WALK — first active ancestor of the responsible
-- unit. Null means the top of the org tree has been reached.
-- ------------------------------------------------------------
create or replace function public.civic_next_escalation_unit(p_report_id uuid)
returns uuid
language plpgsql
stable
security definer
set search_path = public
as $$
declare
    v_org uuid;
    v_current uuid;
    v_parent uuid;
    v_depth integer := 0;
begin
    select organization_id, coalesce(assigned_unit_id, unit_id)
      into v_org, v_current
    from public.civic_reports
    where id = p_report_id;

    -- Unbound report: the org's top-level unit receives escalations
    if v_current is null then
        return (
            select u.id from public.org_units u
            where u.organization_id = v_org
              and u.parent_id is null
              and u.is_active
            order by u.created_at
            limit 1
        );
    end if;

    while v_current is not null and v_depth < 10 loop
        select u.parent_id into v_parent
        from public.org_units u
        where u.id = v_current
          and u.organization_id = v_org
          and u.is_active;
        exit when v_parent is null;
        if exists (
            select 1 from public.org_units p
            where p.id = v_parent
              and p.organization_id = v_org
              and p.is_active
        ) then
            return v_parent;
        end if;
        v_current := v_parent;
        v_depth := v_depth + 1;
    end loop;
    return null;
end $$;

-- ------------------------------------------------------------
-- ESCALATE ONE REPORT — officials (in scope) or the system engine.
-- Records the escalation, walks the chain, transitions through the
-- state machine, notifies the receiving tier.
-- ------------------------------------------------------------
create or replace function public.civic_escalate_report(
    p_report_id uuid,
    p_trigger_type text default 'SLA_BREACH',
    p_reason text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_is_system boolean := v_user is null
        and coalesce(current_setting('role', true), '') = 'service_role';
    v_report public.civic_reports%rowtype;
    v_next uuid;
    v_transition jsonb;
    v_official record;
begin
    if p_trigger_type not in ('SLA_BREACH', 'OFFICER', 'RESIDENT_APPEAL', 'AI_RECOMMENDATION') then
        return jsonb_build_object('ok', false, 'error', 'invalid_trigger');
    end if;

    select * into v_report from public.civic_reports where id = p_report_id;
    if not found then
        return jsonb_build_object('ok', false, 'error', 'report_not_found');
    end if;

    if not v_is_system then
        if v_user is null then
            return jsonb_build_object('ok', false, 'error', 'unauthenticated');
        end if;
        if not public.civic_has_report_scope(
            v_user, v_report.organization_id, v_report.unit_id, v_report.assigned_unit_id
        ) then
            return jsonb_build_object('ok', false, 'error', 'out_of_scope');
        end if;
    end if;

    if v_report.status in ('RESOLVED', 'CLOSED', 'REJECTED', 'ESCALATED') then
        return jsonb_build_object('ok', false, 'error', 'not_escalatable', 'status', v_report.status);
    end if;

    v_next := public.civic_next_escalation_unit(p_report_id);
    if v_next is null then
        return jsonb_build_object('ok', false, 'error', 'no_escalation_target');
    end if;

    insert into public.civic_escalations
        (report_id, from_unit_id, to_unit_id, from_level, to_level,
         trigger_type, reason, created_by)
    values
        (p_report_id,
         coalesce(v_report.assigned_unit_id, v_report.unit_id),
         v_next,
         v_report.escalation_level,
         v_report.escalation_level + 1,
         p_trigger_type, p_reason, v_user);

    v_transition := public.transition_report(p_report_id, 'ESCALATED', p_reason);
    if not (v_transition->>'ok')::boolean then
        return v_transition;
    end if;

    for v_official in
        select distinct r.user_id from public.user_roles r
        where r.org_unit_id = v_next
          and r.is_active
          and (v_user is null or r.user_id <> v_user)
    loop
        insert into public.notifications (user_id, title, body, type, reference_id)
        values (v_official.user_id,
                'Ripoti imepandishwa: ' || v_report.public_reference,
                coalesce(p_reason, 'Ripoti imepandishwa kwenda ngazi yako.'),
                'CIVIC_ESCALATED',
                p_report_id);
    end loop;

    return jsonb_build_object('ok', true, 'to_unit', v_next,
                              'level', v_report.escalation_level + 1);
end $$;

-- ------------------------------------------------------------
-- ESCALATION LIFECYCLE — receiving tier takes ownership (ack) when
-- the report is assigned/worked; closure resolves the escalation.
-- ------------------------------------------------------------
create or replace function public.civic_escalation_lifecycle()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if new.status is distinct from old.status then
        if new.status in ('ASSIGNED', 'IN_PROGRESS') then
            update public.civic_escalations
            set acknowledged_at = coalesce(acknowledged_at, now())
            where report_id = new.id and resolved_at is null;
        elsif new.status in ('RESOLVED', 'CLOSED', 'REJECTED') then
            update public.civic_escalations
            set acknowledged_at = coalesce(acknowledged_at, now()),
                resolved_at = coalesce(resolved_at, now())
            where report_id = new.id and resolved_at is null;
        end if;
    end if;
    return new;
end $$;

drop trigger if exists trg_civic_escalation_lifecycle on public.civic_reports;
create trigger trg_civic_escalation_lifecycle
after update of status on public.civic_reports
for each row execute function public.civic_escalation_lifecycle();

-- ------------------------------------------------------------
-- OVERDUE SCANNER — the engine tick. The cron job runs as the
-- service_role (see schedule below) so the state machine accepts
-- SYSTEM transitions. One escalation per report per scan;
-- per-report failures never abort the whole run.
-- ------------------------------------------------------------
create or replace function public.civic_scan_overdue()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_r record;
    v_official record;
    v_res jsonb;
    v_escalated integer := 0;
    v_top_breach integer := 0;
    v_reminders integer := 0;
    v_unit uuid;
begin

    -- 1) Resolution SLA breaches
    for v_r in
        select * from public.civic_reports
        where due_at < now()
          and status in ('ASSIGNED', 'IN_PROGRESS', 'WAITING_EXTERNAL_ACTION')
          and canonical_report_id is null
        order by due_at
    loop
        begin
            if public.civic_next_escalation_unit(v_r.id) is null then
                -- Top of the org chain: 48h grace + council visibility
                update public.civic_reports
                set due_at = now() + interval '48 hours', updated_at = now()
                where id = v_r.id;
                insert into public.audit_logs
                    (organization_id, actor_role, entity_type, entity_id, action,
                     new_value, reason)
                values
                    (v_r.organization_id, 'SYSTEM', 'civic_report', v_r.id,
                     'sla_breach_top',
                     jsonb_build_object('escalation_level', v_r.escalation_level),
                     'SLA imevuka muda ngazi ya juu — muda mpya wa saa 48');
                for v_official in
                    select r.user_id from public.user_roles r
                    where r.organization_id = v_r.organization_id
                      and r.role in ('COUNCIL_ADMIN', 'SYSTEM_ADMIN')
                      and r.is_active
                loop
                    insert into public.notifications (user_id, title, body, type, reference_id)
                    values (v_official.user_id,
                            'SLA imevuka muda: ' || v_r.public_reference,
                            'Ripoti imevuka muda wa utatuzi hata ngazi ya juu.',
                            'CIVIC_SLA_TOP_BREACH', v_r.id);
                end loop;
                v_top_breach := v_top_breach + 1;
            else
                v_res := public.civic_escalate_report(
                    v_r.id, 'SLA_BREACH',
                    'SLA imevuka muda (' || v_r.public_reference || ')');
                if (v_res->>'ok')::boolean then
                    v_escalated := v_escalated + 1;
                end if;
            end if;
        exception when others then
            insert into public.audit_logs
                (organization_id, actor_role, entity_type, entity_id, action,
                 new_value, reason)
            values
                (v_r.organization_id, 'SYSTEM', 'civic_report', v_r.id,
                 'sla_scan_error',
                 jsonb_build_object('error', sqlerrm),
                 'Uchanganuzi wa SLA umeshindwa kwa ripoti hii');
        end;
    end loop;

    -- 2) Response (triage) SLA breaches — remind once, never re-spam
    for v_r in
        select r.id, r.organization_id, r.public_reference,
               r.unit_id, r.assigned_unit_id,
               coalesce(s.response_hours, 0) as response_hours
        from public.civic_reports r
        join lateral public.civic_resolve_sla(
                r.organization_id, r.category_id, r.severity, r.priority) s on true
        where r.status in ('NEW', 'AI_TRIAGED', 'AWAITING_VERIFICATION', 'VERIFIED')
          and r.canonical_report_id is null
          and r.response_reminded_at is null
          and s.response_hours is not null
          and r.created_at + (s.response_hours || ' hours')::interval < now()
        order by r.created_at
    loop
        begin
            update public.civic_reports
            set response_reminded_at = now(), updated_at = now()
            where id = v_r.id;

            v_unit := coalesce(v_r.unit_id, v_r.assigned_unit_id);
            for v_official in
                select distinct r.user_id
                from public.user_roles r
                where r.is_active
                  and r.org_unit_id in (
                      select x.id from public.org_units x
                      where x.id = v_unit
                         or x.id = (select y.parent_id from public.org_units y
                                    where y.id = v_unit)
                  )
            loop
                insert into public.notifications (user_id, title, body, type, reference_id)
                values (v_official.user_id,
                        'Muda wa majibu umevuka: ' || v_r.public_reference,
                        'Ripoti bado haijafikia majibu ndani ya muda uliopangwa.',
                        'CIVIC_RESPONSE_OVERDUE', v_r.id);
            end loop;
            v_reminders := v_reminders + 1;
        exception when others then
            insert into public.audit_logs
                (organization_id, actor_role, entity_type, entity_id, action,
                 new_value, reason)
            values
                (v_r.organization_id, 'SYSTEM', 'civic_report', v_r.id,
                 'sla_scan_error',
                 jsonb_build_object('error', sqlerrm),
                 'Uchanganuzi wa muda wa majibu umeshindwa kwa ripoti hii');
        end;
    end loop;

    return jsonb_build_object(
        'ok', true,
        'escalated', v_escalated,
        'top_breach', v_top_breach,
        'response_reminders', v_reminders);
end $$;

-- ------------------------------------------------------------
-- SCHEDULE — every 15 minutes. pg_cron jobs run as postgres, which
-- cannot SET ROLE inside a security definer function; this invoker
-- wrapper claims the service_role first so the engine drives SYSTEM
-- transitions through the state machine.
-- ------------------------------------------------------------
create or replace function public.civic_run_sla_scan()
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
    v_res jsonb;
begin
    perform set_config('role', 'service_role', true);
    v_res := public.civic_scan_overdue();
    return v_res;
end $$;

select cron.unschedule('civic-sla-scan')
where exists (select 1 from cron.job where jobname = 'civic-sla-scan');

select cron.schedule('civic-sla-scan', '*/15 * * * *', 'select public.civic_run_sla_scan()');

-- ------------------------------------------------------------
-- GRANTS
-- ------------------------------------------------------------
revoke all on function public.civic_next_escalation_unit(uuid) from public, anon;
revoke all on function public.civic_escalate_report(uuid, text, text) from public, anon;
revoke all on function public.civic_scan_overdue() from public, anon, authenticated;
revoke all on function public.civic_run_sla_scan() from public, anon, authenticated;
grant execute on function public.civic_next_escalation_unit(uuid) to authenticated, service_role;
grant execute on function public.civic_escalate_report(uuid, text, text) to authenticated, service_role;
grant execute on function public.civic_scan_overdue() to service_role;
grant execute on function public.civic_run_sla_scan() to service_role;
