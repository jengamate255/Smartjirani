-- ============================================================
-- 031: TRANSITION RBAC FIX — union-of-roles permission check.
-- Previously transition_report resolved only the actor's single
-- highest-priority org role, so a user holding both MJUMBE and
-- DEPARTMENT_OFFICER could never perform MJUMBE-only transitions
-- (e.g. NEW -> AWAITING_VERIFICATION). Now the actor may act if ANY
-- of their active org roles is in allowed_roles; the highest-priority
-- ALLOWED role is used for attribution. Also records the official's
-- resolved role in status history + audit (was NULL for officials).
-- Also: register the SLA-scan notification types that were missing
-- from the notification_type enum (scan inserts would fail otherwise).
-- ============================================================

alter type public.notification_type add value if not exists 'CIVIC_RESPONSE_OVERDUE' after 'CIVIC_SLA_ALERT';
alter type public.notification_type add value if not exists 'CIVIC_SLA_TOP_BREACH' after 'CIVIC_RESPONSE_OVERDUE';

create or replace function public.transition_report(
    p_report_id uuid,
    p_to_status text,
    p_reason text default null,
    p_note jsonb default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_actor uuid := auth.uid();
    v_is_system boolean := v_actor is null
        and coalesce(current_setting('role', true), '') = 'service_role';
    v_report public.civic_reports%rowtype;
    v_tr public.civic_transitions%rowtype;
    v_role text;
    v_allowed boolean;
    v_sla record;
    v_display_role text;
    v_org uuid;
begin
    -- Resolve actor context
    if v_is_system then
        v_display_role := 'SYSTEM';
    elsif v_actor is not null then
        select organization_id into v_org
        from public.civic_reports where id = p_report_id;

        -- Union-of-roles RBAC: the actor may act if ANY of their active
        -- roles in the report's org is allowed for this transition.
        -- Attribution uses the highest-priority allowed role.
        select ar.role into v_role
        from public.civic_active_roles(v_actor) ar
        join public.civic_transitions t
          on t.from_status = (select status from public.civic_reports where id = p_report_id)
         and t.to_status = p_to_status
         and t.actor_kind = 'OFFICIAL'
         and ar.role = any(t.allowed_roles)
        where ar.organization_id = v_org
        order by case ar.role
            when 'SYSTEM_ADMIN' then 1 when 'COUNCIL_ADMIN' then 2 when 'MUNICIPAL_OFFICER' then 3
            when 'WARD_OFFICER' then 4 when 'MTAA_OFFICER' then 5 when 'MWENYEKITI' then 6
            when 'DEPARTMENT_OFFICER' then 7 when 'MJUMBE' then 8 end
        limit 1;

        if v_role is not null then
            v_display_role := v_role;
        elsif exists (
            select 1 from public.civic_active_roles(v_actor) ar
            where ar.organization_id = v_org
        ) then
            -- Has roles in the org, but none permitted: fall through to
            -- transition_not_allowed below; keep their top role for the error.
            select ar.role into v_role
            from public.civic_active_roles(v_actor) ar
            where ar.organization_id = v_org
            order by case ar.role
                when 'SYSTEM_ADMIN' then 1 when 'COUNCIL_ADMIN' then 2 when 'MUNICIPAL_OFFICER' then 3
                when 'WARD_OFFICER' then 4 when 'MTAA_OFFICER' then 5 when 'MWENYEKITI' then 6
                when 'DEPARTMENT_OFFICER' then 7 when 'MJUMBE' then 8 end
            limit 1;
            v_display_role := v_role;
        end if;
    end if;

    if v_actor is null and not v_is_system then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;

    select * into v_report from public.civic_reports where id = p_report_id for update;
    if not found then
        return jsonb_build_object('ok', false, 'error', 'report_not_found');
    end if;

    if p_to_status = v_report.status then
        return jsonb_build_object('ok', false, 'error', 'already_in_status');
    end if;

    -- Permission by transition row
    if v_is_system then
        select * into v_tr from public.civic_transitions
        where from_status = v_report.status and to_status = p_to_status and actor_kind = 'SYSTEM'
        limit 1;
    elsif v_actor = v_report.reporter_id then
        -- Reporters act only via REPORTER transitions
        select * into v_tr from public.civic_transitions
        where from_status = v_report.status and to_status = p_to_status and actor_kind = 'REPORTER'
        limit 1;
    else
        if v_role is null then
            return jsonb_build_object('ok', false, 'error', 'no_civic_role');
        end if;
        select * into v_tr from public.civic_transitions
        where from_status = v_report.status and to_status = p_to_status
          and actor_kind = 'OFFICIAL' and v_role = any(allowed_roles)
        limit 1;
        if found and v_report.unit_id is not null and v_report.assigned_unit_id is not null then
            v_allowed := exists (
                select 1 from public.civic_has_report_scope(
                    v_actor, v_report.organization_id, v_report.unit_id, v_report.assigned_unit_id) s
            );
            if not v_allowed then
                return jsonb_build_object('ok', false, 'error', 'out_of_scope');
            end if;
        end if;
    end if;

    if v_tr is null then
        return jsonb_build_object(
            'ok', false, 'error', 'transition_not_allowed',
            'from', v_report.status, 'to', p_to_status,
            'actor_role', coalesce(v_role, v_display_role));
    end if;

    -- Apply status-specific side effects
    update public.civic_reports r set
        status = p_to_status,
        verification_status = case
            when p_to_status = 'VERIFIED' then 'VERIFIED'
            when p_to_status = 'REJECTED' then 'REJECTED'
            else r.verification_status end,
        verified_by = case when p_to_status = 'VERIFIED' then v_actor else r.verified_by end,
        verified_at = case when p_to_status = 'VERIFIED' then now() else r.verified_at end,
        assigned_at = case when p_to_status = 'ASSIGNED' then now() else r.assigned_at end,
        resolved_at = case when p_to_status = 'RESOLVED' then now() else r.resolved_at end,
        resolved_by = case when p_to_status = 'RESOLVED' then v_actor else r.resolved_by end,
        closed_at = case when p_to_status = 'CLOSED' then now() else r.closed_at end,
        confirmed_at = case when p_to_status = 'RESOLVED' then now() else r.confirmed_at end,
        resident_confirmation = case
            when p_to_status = 'RESOLVED' then 'CONFIRMED'
            when p_to_status = 'IN_PROGRESS' and v_report.status = 'AWAITING_RESIDENT_CONFIRMATION'
                then 'REJECTED'
            else r.resident_confirmation end,
        escalation_level = case
            when p_to_status = 'ESCALATED' then r.escalation_level + 1
            else r.escalation_level end,
        updated_at = now()
    where r.id = p_report_id;

    -- SLA clock starts at ASSIGNED (config-driven, never hard-coded)
    if p_to_status = 'ASSIGNED' then
        select * into v_sla from public.civic_resolve_sla(
            v_report.organization_id, v_report.category_id, v_report.severity, v_report.priority);
        if v_sla is not null then
            update public.civic_reports
            set due_at = now() + (v_sla.resolution_hours || ' hours')::interval
            where id = p_report_id;
        end if;
    end if;

    -- History (immutable trail)
    insert into public.civic_report_status_history
        (report_id, from_status, to_status, changed_by, changed_by_role, reason)
    values
        (p_report_id, v_report.status, p_to_status, v_actor, v_display_role, p_reason);

    -- Audit
    insert into public.audit_logs
        (organization_id, actor_id, actor_role, entity_type, entity_id, action,
         previous_value, new_value, reason, metadata)
    values
        (v_report.organization_id, v_actor, v_display_role, 'civic_report', p_report_id,
         'status_transition',
         jsonb_build_object('status', v_report.status),
         jsonb_build_object('status', p_to_status),
         p_reason, p_note);

    -- Notify the reporter on every change they did not make themselves
    if v_report.reporter_id is not null and v_report.reporter_id is distinct from v_actor then
        insert into public.notifications (user_id, title, body, type, reference_id)
        values (
            v_report.reporter_id,
            'Ripoti ' || v_report.public_reference,
            'Hali ya ripoti yako imebadilika: ' || v_report.status || ' -> ' || p_to_status,
            'CIVIC_STATUS_CHANGED',
            p_report_id
        );
    end if;

    return jsonb_build_object('ok', true, 'from', v_report.status, 'to', p_to_status);
end $$;

revoke all on function public.transition_report(uuid, text, text, jsonb) from public, anon;
grant execute on function public.transition_report(uuid, text, text, jsonb)
    to authenticated, service_role;
