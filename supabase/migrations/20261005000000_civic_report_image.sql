-- Civic C4: photo evidence for resident reports.
-- Adds image_url to civic_reports and threads it through submit_civic_report
-- as an optional param (existing callers are unaffected).

alter table public.civic_reports
    add column if not exists image_url text;

comment on column public.civic_reports.image_url is
    'Optional photo evidence uploaded by the reporter to storage (media/civic/...).';

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
    p_source text default 'APP',
    p_image_url text default null
) returns jsonb
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
         unit_id, canonical_report_id, image_url)
    values
        (v_org, p_category_id, v_user, p_is_anonymous,
         p_title, p_description, p_latitude, p_longitude, v_geohash, p_location_description,
         p_severity,
         case p_severity
             when 'CRITICAL' then 'URGENT' when 'HIGH' then 'HIGH'
             when 'LOW' then 'LOW' else 'NORMAL' end,
         'NEW', 'PENDING', p_source,
         v_unit, v_canonical.id, p_image_url)
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
end
$$;
