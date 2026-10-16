-- ============================================================
-- 027: CIVIC WORKFLOW RPCs
-- submit_civic_report (rate limit + geospatial duplicate clustering),
-- assign_civic_report (assign/reassign with assignment records),
-- mark_duplicate (official dedupe), civic_geohash (server-side encoding).
-- ============================================================

-- ------------------------------------------------------------
-- Geohash encoder (base32). Lets the server derive cell keys from
-- coordinates so duplicate clustering cannot be spoofed by clients.
-- ------------------------------------------------------------
create or replace function public.civic_geohash(
    p_lat double precision,
    p_lng double precision,
    p_precision integer default 8
)
returns text
language plpgsql immutable
as $$
declare
    v_base32 text := '0123456789bcdefghjkmnpqrstuvwxyz';
    v_lat_lo double precision := -90;
    v_lat_hi double precision := 90;
    v_lng_lo double precision := -180;
    v_lng_hi double precision := 180;
    v_is_lng boolean := true;
    v_mid double precision;
    v_bit integer;
    v_char integer;
    v_result text := '';
    i integer := 0;
begin
    while i < p_precision loop
        v_char := 0;
        for v_bit in 1..5 loop
            if v_is_lng then
                v_mid := (v_lng_lo + v_lng_hi) / 2;
                if p_lng >= v_mid then
                    v_char := v_char * 2 + 1;
                    v_lng_lo := v_mid;
                else
                    v_char := v_char * 2;
                    v_lng_hi := v_mid;
                end if;
            else
                v_mid := (v_lat_lo + v_lat_hi) / 2;
                if p_lat >= v_mid then
                    v_char := v_char * 2 + 1;
                    v_lat_lo := v_mid;
                else
                    v_char := v_char * 2;
                    v_lat_hi := v_mid;
                end if;
            end if;
            v_is_lng := not v_is_lng;
        end loop;
        v_result := v_result || substr(v_base32, v_char + 1, 1);
        i := i + 1;
    end loop;
    return v_result;
end $$;

-- ------------------------------------------------------------
-- SUBMIT — the only way the app creates reports. Reporter identity is
-- always recorded (anti-abuse); is_anonymous only controls display.
-- Clusters near-duplicates: same org+category in the same geohash-6
-- cell (~1.2km) within 14 days merge into the oldest open report.
-- ------------------------------------------------------------
create or replace function public.submit_civic_report(
    p_category_id uuid,
    p_title text,
    p_description text,
    p_latitude double precision default null,
    p_longitude double precision default null,
    p_location_description text default null,
    p_organization_id uuid default null,
    p_neighborhood_id uuid default null,
    p_severity text default 'MEDIUM',
    p_is_anonymous boolean default false,
    p_source text default 'APP'
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_org uuid;
    v_unit uuid;
    v_geohash text;
    v_canonical public.civic_reports%rowtype;
    v_new public.civic_reports%rowtype;
    v_recent integer;
    v_official record;
begin
    if v_user is null then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;

    -- Resolve organization: explicit param, else via neighborhood bridge —
    -- caller param first, then the reporter's own profile neighborhood.
    v_org := coalesce(
        p_organization_id,
        (select u.organization_id from public.org_units u
         where u.neighborhood_id = coalesce(
             p_neighborhood_id,
             (select pr.neighborhood_id from public.profiles pr where pr.id = v_user)
         )
         order by u.created_at limit 1)
    );
    if v_org is null then
        return jsonb_build_object('ok', false, 'error', 'organization_unresolved');
    end if;

    if not exists (
        select 1 from public.civic_categories
        where id = p_category_id and organization_id = v_org and is_active
    ) then
        return jsonb_build_object('ok', false, 'error', 'invalid_category');
    end if;

    if p_severity not in ('LOW','MEDIUM','HIGH','CRITICAL') then
        return jsonb_build_object('ok', false, 'error', 'invalid_severity');
    end if;

    -- Anti-abuse rate limit: max 10 reports per resident per 24h
    select count(*) into v_recent from public.civic_reports
    where reporter_id = v_user and created_at > now() - interval '24 hours';
    if v_recent >= 10 then
        return jsonb_build_object('ok', false, 'error', 'rate_limited', 'window_hours', 24);
    end if;

    v_geohash := case
        when p_latitude is not null and p_longitude is not null
        then public.civic_geohash(p_latitude, p_longitude, 8)
    end;

    -- Bind the report to the civic unit covering the reporter's
    -- neighborhood (MTAA-level bridge), when one exists.
    v_unit := (
        select u.id from public.org_units u
        join public.profiles pr on pr.neighborhood_id = u.neighborhood_id
        where pr.id = v_user and u.organization_id = v_org
        order by u.created_at limit 1
    );

    -- Duplicate clustering: oldest still-open report in the same cell wins
    if v_geohash is not null then
        select * into v_canonical
        from public.civic_reports r
        where r.organization_id = v_org
          and r.category_id = p_category_id
          and r.geohash is not null
          and left(r.geohash, 6) = left(v_geohash, 6)
          and r.created_at > now() - interval '14 days'
          and r.status not in ('REJECTED', 'CLOSED')
          and r.canonical_report_id is null
        order by r.created_at
        limit 1;
    end if;

    insert into public.civic_reports
        (organization_id, category_id, reporter_id, is_anonymous,
         title, description, latitude, longitude, geohash, location_description,
         severity, priority, status, verification_status, source,
         unit_id, canonical_report_id)
    values
        (v_org, p_category_id, v_user, p_is_anonymous,
         p_title, p_description, p_latitude, p_longitude, v_geohash, p_location_description,
         p_severity,
         case p_severity
             when 'CRITICAL' then 'URGENT' when 'HIGH' then 'HIGH'
             when 'LOW' then 'LOW' else 'NORMAL' end,
         'NEW', 'PENDING', p_source,
         v_unit, v_canonical.id)
    returning * into v_new;

    -- Audit: submissions are auditable too
    insert into public.audit_logs
        (organization_id, actor_id, entity_type, entity_id, action, new_value)
    values
        (v_org, v_user, 'civic_report', v_new.id, 'submit',
         jsonb_build_object('category_id', p_category_id, 'severity', p_severity,
                            'duplicate_of', v_canonical.id, 'source', p_source));

    -- Notify street-level officials of the new report
    if v_unit is not null and v_canonical.id is null then
        for v_official in
            select r.user_id from public.user_roles r
            where r.org_unit_id = v_unit and r.is_active
              and r.role in ('MJUMBE','MWENYEKITI','MTAA_OFFICER')
              and r.user_id <> v_user
        loop
            insert into public.notifications (user_id, title, body, type, reference_id)
            values (v_official.user_id,
                    'Ripoti mpya: ' || p_title,
                    'Ripoti mpya imepokezwa katika eneo lako.',
                    'CIVIC_NEW_REPORT', v_new.id);
        end loop;
    end if;

    if v_canonical.id is not null then
        return jsonb_build_object(
            'ok', true, 'merged', true,
            'report_id', v_new.id,
            'public_reference', v_new.public_reference,
            'canonical_report_id', v_canonical.id,
            'canonical_reference', v_canonical.public_reference);
    end if;
    return jsonb_build_object(
        'ok', true, 'merged', false,
        'report_id', v_new.id,
        'public_reference', v_new.public_reference);
end $$;

-- ------------------------------------------------------------
-- ASSIGN / REASSIGN — officials route a verified (or escalated /
-- in-progress) report to a responsible unit and optionally a person.
-- Unit changes are impossible through direct UPDATE (no policy), so
-- this RPC is the only routing path.
-- ------------------------------------------------------------
create or replace function public.assign_civic_report(
    p_report_id uuid,
    p_unit_id uuid,
    p_assignee uuid default null,
    p_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_report public.civic_reports%rowtype;
    v_is_system boolean := v_user is null
        and coalesce(current_setting('role', true), '') = 'service_role';
    v_transition jsonb;
begin
    if v_user is null and not v_is_system then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;

    select * into v_report from public.civic_reports where id = p_report_id;
    if not found then
        return jsonb_build_object('ok', false, 'error', 'report_not_found');
    end if;

    if not v_is_system and not public.civic_has_report_scope(
        v_user, v_report.organization_id, v_report.unit_id, v_report.assigned_unit_id
    ) then
        return jsonb_build_object('ok', false, 'error', 'out_of_scope');
    end if;

    if not exists (
        select 1 from public.org_units
        where id = p_unit_id and organization_id = v_report.organization_id and is_active
    ) then
        return jsonb_build_object('ok', false, 'error', 'invalid_unit');
    end if;

    if v_report.status not in ('VERIFIED','ASSIGNED','IN_PROGRESS','ESCALATED') then
        return jsonb_build_object('ok', false, 'error', 'not_assignable', 'status', v_report.status);
    end if;

    if p_assignee is not null and not exists (
        select 1 from public.user_roles
        where user_id = p_assignee
          and organization_id = v_report.organization_id
          and org_unit_id = p_unit_id
          and is_active
    ) then
        return jsonb_build_object('ok', false, 'error', 'assignee_not_in_unit');
    end if;

    -- Close any open assignment record
    update public.civic_report_assignments
    set status = 'REASSIGNED'
    where report_id = p_report_id and status = 'ACTIVE';

    insert into public.civic_report_assignments
        (report_id, assigned_to, assigned_by, org_unit_id, note)
    values (p_report_id, p_assignee, v_user, p_unit_id, p_note);

    update public.civic_reports
    set assigned_unit_id = p_unit_id, updated_at = now()
    where id = p_report_id;

    -- Fresh assignment (from VERIFIED/ESCALATED) runs through the state
    -- machine so the SLA clock starts; reassignments stay in status.
    if v_report.status in ('VERIFIED','ESCALATED') then
        v_transition := public.transition_report(p_report_id, 'ASSIGNED', p_note);
        if not (v_transition->>'ok')::boolean then
            return v_transition;
        end if;
    end if;

    -- Notify the assignee
    if p_assignee is not null and p_assignee is distinct from v_user then
        insert into public.notifications (user_id, title, body, type, reference_id)
        values (p_assignee,
                'Ripoti imekabidhiwa: ' || v_report.public_reference,
                'Umekabidhiwa ripoti ya kufuatilia.',
                'CIVIC_ASSIGNED', p_report_id);
    end if;

    return jsonb_build_object('ok', true, 'assigned_unit_id', p_unit_id,
                              'status', case when v_report.status in ('VERIFIED','ESCALATED')
                                        then 'ASSIGNED' else v_report.status end);
end $$;

-- ------------------------------------------------------------
-- MARK DUPLICATE — officials merge two reports by hand when clustering
-- missed them (different wording, older than 14d, adjacent cells).
-- p_canonical_id = null unlinks.
-- ------------------------------------------------------------
create or replace function public.mark_duplicate(
    p_report_id uuid,
    p_canonical_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_report public.civic_reports%rowtype;
    v_canonical public.civic_reports%rowtype;
begin
    if v_user is null then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;

    select * into v_report from public.civic_reports where id = p_report_id;
    if not found then
        return jsonb_build_object('ok', false, 'error', 'report_not_found');
    end if;

    if not public.civic_has_report_scope(
        v_user, v_report.organization_id, v_report.unit_id, v_report.assigned_unit_id
    ) then
        return jsonb_build_object('ok', false, 'error', 'out_of_scope');
    end if;

    if p_canonical_id is null then
        update public.civic_reports
        set canonical_report_id = null, updated_at = now()
        where id = p_report_id;
        insert into public.audit_logs
            (organization_id, actor_id, entity_type, entity_id, action, previous_value)
        values
            (v_report.organization_id, v_user, 'civic_report', p_report_id, 'duplicate_unlinked',
             jsonb_build_object('previous_canonical', v_report.canonical_report_id));
        return jsonb_build_object('ok', true, 'unlinked', true);
    end if;

    select * into v_canonical from public.civic_reports where id = p_canonical_id;
    if not found then
        return jsonb_build_object('ok', false, 'error', 'canonical_not_found');
    end if;

    if v_canonical.organization_id <> v_report.organization_id
       or p_report_id = p_canonical_id
       or v_canonical.canonical_report_id is not null then
        return jsonb_build_object('ok', false, 'error', 'invalid_canonical');
    end if;

    update public.civic_reports
    set canonical_report_id = p_canonical_id, updated_at = now()
    where id = p_report_id;

    insert into public.audit_logs
        (organization_id, actor_id, entity_type, entity_id, action,
         previous_value, new_value)
    values
        (v_report.organization_id, v_user, 'civic_report', p_report_id, 'duplicate_marked',
         jsonb_build_object('previous_canonical', v_report.canonical_report_id),
         jsonb_build_object('canonical', p_canonical_id));

    return jsonb_build_object('ok', true, 'merged', true,
                              'canonical', p_canonical_id);
end $$;

-- RPC grants: submissions for every resident, routing for officials
revoke all on function public.submit_civic_report(uuid, text, text, double precision, double precision, text, uuid, uuid, text, boolean, text) from public, anon;
revoke all on function public.assign_civic_report(uuid, uuid, uuid, text) from public, anon;
revoke all on function public.mark_duplicate(uuid, uuid) from public, anon;
grant execute on function public.submit_civic_report(uuid, text, text, double precision, double precision, text, uuid, uuid, text, boolean, text)
    to authenticated, service_role;
grant execute on function public.assign_civic_report(uuid, uuid, uuid, text)
    to authenticated, service_role;
grant execute on function public.mark_duplicate(uuid, uuid)
    to authenticated, service_role;
