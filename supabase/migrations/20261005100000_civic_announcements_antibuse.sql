-- Civic C7: official announcements + community anti-abuse.
-- 1. civic_announcements — RPC-only writes (RLS has no INSERT path);
--    publish_civic_announcement is role-gated, audited, and notifies
--    every resident profile in the org's covered neighborhoods.
-- 2. post_flags — one flag per (post, flagger); writes go through
--    flag_post (rate limited, own-post guard). Counts are only exposed
--    via civic_reported_posts() because select-own RLS hides the table.

-- ------------------------------------------------------------
-- 1. OFFICIAL ANNOUNCEMENTS
-- ------------------------------------------------------------

create table if not exists public.civic_announcements (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations(id) on delete cascade,
    title text not null check (char_length(title) between 3 and 200),
    body text not null check (char_length(body) between 10 and 5000),
    category text not null default 'NOTICE'
        check (category in ('NOTICE','HEALTH','SAFETY','EVENT','UTILITY')),
    pinned boolean not null default false,
    is_active boolean not null default true,
    published_by uuid not null references public.profiles(id) on delete cascade,
    published_at timestamptz not null default now()
);
create index if not exists idx_civic_announcements_org
    on public.civic_announcements(organization_id, published_at desc);

comment on table public.civic_announcements is
    'Official announcements from government organizations. Writes happen only through publish_civic_announcement (RPC); RLS exposes active rows to authenticated readers.';

alter table public.civic_announcements enable row level security;

create policy "civic_announcements_select_active"
    on public.civic_announcements
    for select
    to authenticated
    using (is_active);

-- ------------------------------------------------------------
-- 2. PUBLISH RPC (role-gated, audited, fan-out notifications)
-- ------------------------------------------------------------

create or replace function public.publish_civic_announcement(
    p_title text,
    p_body text,
    p_category text default 'NOTICE',
    p_pinned boolean default false
) returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_org uuid;
    v_id uuid;
    v_recipients integer := 0;
begin
    if v_user is null then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;

    -- The publisher's org comes from their first active civic role;
    -- residents have no role row, so they hit 'forbidden'.
    select r.organization_id into v_org
    from public.civic_active_roles(v_user) r
    limit 1;
    if v_org is null then
        return jsonb_build_object('ok', false, 'error', 'forbidden');
    end if;

    if p_category not in ('NOTICE','HEALTH','SAFETY','EVENT','UTILITY') then
        return jsonb_build_object('ok', false, 'error', 'invalid_category');
    end if;

    if p_title is null or char_length(btrim(p_title)) < 3 or char_length(btrim(p_title)) > 200 then
        return jsonb_build_object('ok', false, 'error', 'invalid_title');
    end if;
    if p_body is null or char_length(btrim(p_body)) < 10 or char_length(btrim(p_body)) > 5000 then
        return jsonb_build_object('ok', false, 'error', 'invalid_body');
    end if;

    insert into public.civic_announcements
        (organization_id, title, body, category, pinned, published_by)
    values
        (v_org, btrim(p_title), btrim(p_body), p_category, p_pinned, v_user)
    returning id into v_id;

    insert into public.audit_logs
        (organization_id, actor_id, entity_type, entity_id, action, new_value)
    values
        (v_org, v_user, 'civic_announcement', v_id, 'publish',
         jsonb_build_object('category', p_category, 'pinned', p_pinned));

    -- Fan-out to every profile living in a neighborhood the org covers.
    insert into public.notifications (user_id, title, body, type, reference_id)
    select pr.id,
           'Tangazo: ' || btrim(p_title),
           left(btrim(p_body), 200),
           'CIVIC_ANNOUNCEMENT'::public.notification_type,
           v_id
    from public.profiles pr
    where pr.id <> v_user
      and pr.neighborhood_id is not null
      and exists (
          select 1 from public.org_units u
          where u.organization_id = v_org and u.neighborhood_id = pr.neighborhood_id
      );

    get diagnostics v_recipients = row_count;

    return jsonb_build_object(
        'ok', true,
        'announcement_id', v_id,
        'recipients', v_recipients);
end
$$;

revoke execute on function public.publish_civic_announcement(text, text, text, boolean)
    from anon, public;
grant execute on function public.publish_civic_announcement(text, text, text, boolean)
    to authenticated, service_role;

-- ------------------------------------------------------------
-- 3. POST FLAGGING (community anti-abuse)
-- ------------------------------------------------------------

create table if not exists public.post_flags (
    id uuid primary key default gen_random_uuid(),
    post_id uuid not null references public.posts(id) on delete cascade,
    flagger_id uuid not null references public.profiles(id) on delete cascade,
    reason text,
    created_at timestamptz not null default now(),
    unique (post_id, flagger_id)
);
create index if not exists idx_post_flags_post on public.post_flags(post_id);

comment on table public.post_flags is
    'Community spam/inappropriate flags on feed posts. One vote per (post, flagger). Rows are select-own via RLS; the aggregate "3+ distinct flaggers" signal is exposed only through civic_reported_posts().';

alter table public.post_flags enable row level security;

create policy "post_flags_select_own"
    on public.post_flags
    for select
    to authenticated
    using ((select auth.uid()) = flagger_id);

create policy "post_flags_insert_own"
    on public.post_flags
    for insert
    to authenticated
    with check ((select auth.uid()) = flagger_id);

create or replace function public.flag_post(
    p_post_id uuid,
    p_reason text default null
) returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_recent integer;
begin
    if v_user is null then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;

    if not exists (select 1 from public.posts where id = p_post_id) then
        return jsonb_build_object('ok', false, 'error', 'post_not_found');
    end if;

    -- Authors moderate their own posts (delete), they don't flag them.
    if exists (
        select 1 from public.posts where id = p_post_id and author_id = v_user
    ) then
        return jsonb_build_object('ok', false, 'error', 'own_post');
    end if;

    -- Anti-abuse: max 10 flags per user per 24h
    select count(*) into v_recent from public.post_flags
    where flagger_id = v_user and created_at > now() - interval '24 hours';
    if v_recent >= 10 then
        return jsonb_build_object('ok', false, 'error', 'rate_limited', 'window_hours', 24);
    end if;

    insert into public.post_flags (post_id, flagger_id, reason)
    values (p_post_id, v_user, nullif(left(btrim(coalesce(p_reason, '')), 500), ''))
    on conflict (post_id, flagger_id) do nothing;

    return jsonb_build_object('ok', true);
end
$$;

revoke execute on function public.flag_post(uuid, text) from anon, public;
grant execute on function public.flag_post(uuid, text) to authenticated, service_role;

-- Aggregated report signal for the feed: posts flagged by 3+ distinct
-- residents. Security definer because post_flags itself is select-own;
-- stable so it can be safely called per feed load.
create or replace function public.civic_reported_posts()
returns table (post_id uuid, flag_count bigint)
language sql
stable
security definer
set search_path = public
as $$
    select f.post_id, count(distinct f.flagger_id) as flag_count
    from public.post_flags f
    group by f.post_id
    having count(distinct f.flagger_id) >= 3
$$;

revoke execute on function public.civic_reported_posts() from anon, public;
grant execute on function public.civic_reported_posts() to authenticated, service_role;

-- ------------------------------------------------------------
-- 4. SPAM GUARD in submit_civic_report: identical report (same
--    reporter + category + title) within 1h merges into the recent
--    copy. Geohash duplicate clustering keeps precedence; this only
--    fills v_canonical when geo clustering found nothing.
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

    -- Spam guard: same reporter re-submitting the identical report
    -- (category + title) within 1 hour merges instead of duplicating.
    if v_canonical.id is null then
        select * into v_canonical
        from public.civic_reports r
        where r.reporter_id = v_user
          and r.category_id = p_category_id
          and lower(r.title) = lower(p_title)
          and r.created_at > now() - interval '1 hour'
          and r.status not in ('REJECTED', 'CLOSED')
        order by r.created_at desc
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
