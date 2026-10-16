-- ============================================================
-- Civic C9: government admin config RPCs
-- Org tree, categories, SLA rules and civic role assignment for
-- COUNCIL_ADMIN / SYSTEM_ADMIN. Reads are SECURITY INVOKER so RLS
-- scopes every row; writes are SECURITY DEFINER because audit_logs
-- has no insert policy for callers — each write re-checks the
-- caller's admin role and leaves an audit trail.
-- ============================================================

-- Caller's admin organization: COUNCIL_ADMIN org wins over SYSTEM_ADMIN org.
create or replace function public.civic_admin_org()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
    select organization_id from (
        select ar.organization_id
        from public.civic_active_roles(auth.uid()) ar
        where ar.role = 'COUNCIL_ADMIN'
        union all
        select ar.organization_id
        from public.civic_active_roles(auth.uid()) ar
        where ar.role = 'SYSTEM_ADMIN'
    ) t
    limit 1
$$;

-- ------------------------------------------------------------
-- Reads
-- ------------------------------------------------------------

create or replace function public.civic_admin_unit_types()
returns table (id uuid, name text, level integer, is_department boolean)
language sql
stable
security invoker
set search_path = public
as $$
    select t.id, t.name, t.level, t.is_department
    from public.org_unit_types t
    where t.organization_id = public.civic_admin_org()
    order by t.level, t.name
$$;

create or replace function public.civic_admin_units()
returns table (id uuid, name text, code text, parent_id uuid,
               unit_type_id uuid, unit_type text, unit_level integer, is_active boolean)
language sql
stable
security invoker
set search_path = public
as $$
    select u.id, u.name, u.code, u.parent_id,
           u.unit_type_id, t.name, t.level, u.is_active
    from public.org_units u
    join public.org_unit_types t on t.id = u.unit_type_id
    where u.organization_id = public.civic_admin_org()
    order by t.level, u.name
$$;

create or replace function public.civic_admin_sla_rules()
returns table (id uuid, category_id uuid, category_sw text, severity text, priority text,
               response_hours numeric, resolution_hours numeric, is_active boolean)
language sql
stable
security invoker
set search_path = public
as $$
    select r.id, r.category_id, c.name_sw, r.severity, r.priority,
           r.response_hours, r.resolution_hours, r.is_active
    from public.sla_rules r
    left join public.civic_categories c on c.id = r.category_id
    where r.organization_id = public.civic_admin_org()
    order by r.created_at desc
$$;

create or replace function public.civic_admin_roles()
returns table (id uuid, user_id uuid, role text, org_unit_id uuid, unit_name text,
               display_name text, email text, is_active boolean)
language sql
stable
security invoker
set search_path = public
as $$
    select ur.id, ur.user_id, ur.role, ur.org_unit_id, u.name,
           p.display_name, p.email, ur.is_active
    from public.user_roles ur
    join public.org_units u on u.id = ur.org_unit_id
    left join public.profiles p on p.id = ur.user_id
    where ur.organization_id = public.civic_admin_org()
    order by ur.created_at desc
$$;

-- Resident directory search for role assignment (top 10 matches).
create or replace function public.civic_admin_directory(p_query text)
returns table (id uuid, display_name text, email text, username text)
language sql
stable
security invoker
set search_path = public
as $$
    select p.id, p.display_name, p.email, p.username
    from public.profiles p
    where public.civic_admin_org() is not null
      and (
        p.display_name ilike '%' || p_query || '%'
        or p.username ilike '%' || p_query || '%'
        or p.email ilike '%' || p_query || '%'
      )
    order by p.display_name
    limit 10
$$;

-- Active categories for the admin org (civic_list_categories resolves org via
-- the reporter's neighborhood, which fails for officials without one).
create or replace function public.civic_admin_categories()
returns table (id uuid, name_en text, name_sw text, description text, icon text,
               sort_order integer, requires_verification boolean)
language sql
stable
security invoker
set search_path = public
as $$
    select c.id, c.name_en, c.name_sw, c.description, c.icon,
           c.sort_order, c.requires_verification
    from public.civic_categories c
    where c.organization_id = public.civic_admin_org()
      and c.is_active
    order by c.sort_order, c.name_sw
$$;

-- ------------------------------------------------------------
-- Writes (admin gate inside; RLS write policies re-check too)
-- ------------------------------------------------------------

create or replace function public.civic_admin_create_unit(
    p_unit_type_id uuid,
    p_name text,
    p_parent_id uuid default null,
    p_code text default null,
    p_neighborhood_id uuid default null
) returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_org uuid := public.civic_admin_org();
    v_actor_role text;
    v_type public.org_unit_types%rowtype;
    v_unit public.org_units%rowtype;
begin
    select ar.role into v_actor_role
    from public.civic_active_roles(v_user) ar
    where ar.role in ('COUNCIL_ADMIN','SYSTEM_ADMIN')
    order by case ar.role when 'COUNCIL_ADMIN' then 0 else 1 end
    limit 1;
    if v_org is null or v_actor_role is null then
        return jsonb_build_object('ok', false, 'error', 'not_admin');
    end if;
    if p_name is null or length(trim(p_name)) < 2 then
        return jsonb_build_object('ok', false, 'error', 'invalid_name');
    end if;

    select * into v_type from public.org_unit_types where id = p_unit_type_id;
    if v_type.id is null or v_type.organization_id is distinct from v_org then
        return jsonb_build_object('ok', false, 'error', 'unit_type_not_found');
    end if;

    if p_parent_id is not null and not exists (
        select 1 from public.org_units pu
        where pu.id = p_parent_id and pu.organization_id = v_org
    ) then
        return jsonb_build_object('ok', false, 'error', 'parent_not_found');
    end if;

    insert into public.org_units
        (organization_id, unit_type_id, parent_id, name, code, neighborhood_id)
    values
        (v_org, p_unit_type_id, p_parent_id, trim(p_name), nullif(trim(p_code), ''), p_neighborhood_id)
    returning * into v_unit;

    insert into public.audit_logs
        (organization_id, actor_id, actor_role, entity_type, entity_id, action, new_value)
    values
        (v_org, v_user, v_actor_role, 'org_unit', v_unit.id, 'create_unit',
         jsonb_build_object('name', v_unit.name, 'unit_type', v_type.name, 'parent_id', p_parent_id));

    return jsonb_build_object('ok', true, 'id', v_unit.id);
exception
    when unique_violation then
        return jsonb_build_object('ok', false, 'error', 'duplicate_unit');
end;
$$;

create or replace function public.civic_admin_create_category(
    p_name_en text,
    p_name_sw text,
    p_description text default null,
    p_requires_verification boolean default true,
    p_icon text default null,
    p_sort_order integer default 0
) returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_org uuid := public.civic_admin_org();
    v_actor_role text;
    v_category public.civic_categories%rowtype;
begin
    select ar.role into v_actor_role
    from public.civic_active_roles(v_user) ar
    where ar.role in ('COUNCIL_ADMIN','SYSTEM_ADMIN')
    order by case ar.role when 'COUNCIL_ADMIN' then 0 else 1 end
    limit 1;
    if v_org is null or v_actor_role is null then
        return jsonb_build_object('ok', false, 'error', 'not_admin');
    end if;
    if length(trim(p_name_en)) < 2 or length(trim(p_name_sw)) < 2 then
        return jsonb_build_object('ok', false, 'error', 'invalid_name');
    end if;

    insert into public.civic_categories
        (organization_id, name_en, name_sw, description, icon, sort_order, requires_verification)
    values
        (v_org, trim(p_name_en), trim(p_name_sw), nullif(trim(p_description), ''),
         p_icon, p_sort_order, p_requires_verification)
    returning * into v_category;

    insert into public.audit_logs
        (organization_id, actor_id, actor_role, entity_type, entity_id, action, new_value)
    values
        (v_org, v_user, v_actor_role, 'civic_category', v_category.id, 'create_category',
         jsonb_build_object('name_en', v_category.name_en, 'name_sw', v_category.name_sw));

    return jsonb_build_object('ok', true, 'id', v_category.id);
exception
    when unique_violation then
        return jsonb_build_object('ok', false, 'error', 'duplicate_category');
end;
$$;

create or replace function public.civic_admin_upsert_sla_rule(
    p_category_id uuid default null,
    p_severity text default null,
    p_priority text default null,
    p_response_hours numeric default null,
    p_resolution_hours numeric default null
) returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_org uuid := public.civic_admin_org();
    v_actor_role text;
    v_rule public.sla_rules%rowtype;
begin
    select ar.role into v_actor_role
    from public.civic_active_roles(v_user) ar
    where ar.role in ('COUNCIL_ADMIN','SYSTEM_ADMIN')
    order by case ar.role when 'COUNCIL_ADMIN' then 0 else 1 end
    limit 1;
    if v_org is null or v_actor_role is null then
        return jsonb_build_object('ok', false, 'error', 'not_admin');
    end if;
    if p_response_hours is null or p_response_hours <= 0
       or p_resolution_hours is null or p_resolution_hours <= 0 then
        return jsonb_build_object('ok', false, 'error', 'invalid_hours');
    end if;
    if p_severity is not null and p_severity not in ('LOW','MEDIUM','HIGH','CRITICAL') then
        return jsonb_build_object('ok', false, 'error', 'invalid_severity');
    end if;
    if p_priority is not null and p_priority not in ('LOW','NORMAL','HIGH','URGENT') then
        return jsonb_build_object('ok', false, 'error', 'invalid_priority');
    end if;
    if p_category_id is not null and not exists (
        select 1 from public.civic_categories c
        where c.id = p_category_id and c.organization_id = v_org
    ) then
        return jsonb_build_object('ok', false, 'error', 'category_not_found');
    end if;

    if exists (
        select 1 from public.sla_rules r
        where r.organization_id = v_org
          and r.category_id is not distinct from p_category_id
          and r.severity is not distinct from p_severity
          and r.priority is not distinct from p_priority
          and r.is_active
    ) then
        return jsonb_build_object('ok', false, 'error', 'duplicate_rule');
    end if;

    insert into public.sla_rules
        (organization_id, category_id, severity, priority, response_hours, resolution_hours)
    values
        (v_org, p_category_id, p_severity, p_priority, p_response_hours, p_resolution_hours)
    returning * into v_rule;

    insert into public.audit_logs
        (organization_id, actor_id, actor_role, entity_type, entity_id, action, new_value)
    values
        (v_org, v_user, v_actor_role, 'sla_rule', v_rule.id, 'create_sla_rule',
         jsonb_build_object('category_id', p_category_id, 'severity', p_severity,
                            'priority', p_priority,
                            'response_hours', p_response_hours,
                            'resolution_hours', p_resolution_hours));

    return jsonb_build_object('ok', true, 'id', v_rule.id);
end;
$$;

create or replace function public.civic_admin_assign_role(
    p_user_id uuid,
    p_role text,
    p_org_unit_id uuid
) returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_org uuid := public.civic_admin_org();
    v_actor_role text;
    v_row public.user_roles%rowtype;
begin
    select ar.role into v_actor_role
    from public.civic_active_roles(v_user) ar
    where ar.role in ('COUNCIL_ADMIN','SYSTEM_ADMIN')
    order by case ar.role when 'COUNCIL_ADMIN' then 0 else 1 end
    limit 1;
    if v_org is null or v_actor_role is null then
        return jsonb_build_object('ok', false, 'error', 'not_admin');
    end if;
    if p_role not in ('MJUMBE','MWENYEKITI','MTAA_OFFICER','WARD_OFFICER',
                      'MUNICIPAL_OFFICER','DEPARTMENT_OFFICER','COUNCIL_ADMIN','SYSTEM_ADMIN') then
        return jsonb_build_object('ok', false, 'error', 'invalid_role');
    end if;
    -- Only a SYSTEM_ADMIN may mint another SYSTEM_ADMIN.
    if p_role = 'SYSTEM_ADMIN' and not public.civic_is_system_admin(v_user) then
        return jsonb_build_object('ok', false, 'error', 'forbidden_role');
    end if;
    if not exists (select 1 from public.profiles pr where pr.id = p_user_id) then
        return jsonb_build_object('ok', false, 'error', 'user_not_found');
    end if;
    if not exists (
        select 1 from public.org_units pu
        where pu.id = p_org_unit_id and pu.organization_id = v_org
    ) then
        return jsonb_build_object('ok', false, 'error', 'unit_not_found');
    end if;

    insert into public.user_roles (user_id, organization_id, org_unit_id, role, assigned_by)
    values (p_user_id, v_org, p_org_unit_id, p_role, v_user)
    on conflict (user_id, org_unit_id, role) do update
        set is_active = true, assigned_by = v_user
    returning * into v_row;

    insert into public.audit_logs
        (organization_id, actor_id, actor_role, entity_type, entity_id, action, new_value)
    values
        (v_org, v_user, v_actor_role, 'user_role', v_row.id, 'assign_role',
         jsonb_build_object('user_id', p_user_id, 'role', p_role, 'org_unit_id', p_org_unit_id));

    return jsonb_build_object('ok', true, 'id', v_row.id);
end;
$$;

create or replace function public.civic_admin_revoke_role(p_role_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_user uuid := auth.uid();
    v_org uuid := public.civic_admin_org();
    v_actor_role text;
    v_row public.user_roles%rowtype;
begin
    select ar.role into v_actor_role
    from public.civic_active_roles(v_user) ar
    where ar.role in ('COUNCIL_ADMIN','SYSTEM_ADMIN')
    order by case ar.role when 'COUNCIL_ADMIN' then 0 else 1 end
    limit 1;
    if v_org is null or v_actor_role is null then
        return jsonb_build_object('ok', false, 'error', 'not_admin');
    end if;

    update public.user_roles
    set is_active = false
    where id = p_role_id and organization_id = v_org
    returning * into v_row;

    if v_row.id is null then
        return jsonb_build_object('ok', false, 'error', 'not_found');
    end if;

    insert into public.audit_logs
        (organization_id, actor_id, actor_role, entity_type, entity_id, action, previous_value)
    values
        (v_org, v_user, v_actor_role, 'user_role', v_row.id, 'revoke_role',
         jsonb_build_object('user_id', v_row.user_id, 'role', v_row.role,
                            'org_unit_id', v_row.org_unit_id));

    return jsonb_build_object('ok', true);
end;
$$;

revoke execute on function public.civic_admin_org() from anon, public;
revoke execute on function public.civic_admin_unit_types() from anon, public;
revoke execute on function public.civic_admin_units() from anon, public;
revoke execute on function public.civic_admin_sla_rules() from anon, public;
revoke execute on function public.civic_admin_roles() from anon, public;
revoke execute on function public.civic_admin_directory(text) from anon, public;
revoke execute on function public.civic_admin_categories() from anon, public;
revoke execute on function public.civic_admin_create_unit(uuid, text, uuid, text, uuid) from anon, public;
revoke execute on function public.civic_admin_create_category(text, text, text, boolean, text, integer) from anon, public;
revoke execute on function public.civic_admin_upsert_sla_rule(uuid, text, text, numeric, numeric) from anon, public;
revoke execute on function public.civic_admin_assign_role(uuid, text, uuid) from anon, public;
revoke execute on function public.civic_admin_revoke_role(uuid) from anon, public;

grant execute on function public.civic_admin_org() to authenticated, service_role;
grant execute on function public.civic_admin_unit_types() to authenticated, service_role;
grant execute on function public.civic_admin_units() to authenticated, service_role;
grant execute on function public.civic_admin_sla_rules() to authenticated, service_role;
grant execute on function public.civic_admin_roles() to authenticated, service_role;
grant execute on function public.civic_admin_directory(text) to authenticated, service_role;
grant execute on function public.civic_admin_categories() to authenticated, service_role;
grant execute on function public.civic_admin_create_unit(uuid, text, uuid, text, uuid) to authenticated, service_role;
grant execute on function public.civic_admin_create_category(text, text, text, boolean, text, integer) to authenticated, service_role;
grant execute on function public.civic_admin_upsert_sla_rule(uuid, text, text, numeric, numeric) to authenticated, service_role;
grant execute on function public.civic_admin_assign_role(uuid, text, uuid) to authenticated, service_role;
grant execute on function public.civic_admin_revoke_role(uuid) to authenticated, service_role;
