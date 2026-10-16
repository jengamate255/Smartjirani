-- SYSTEM_ADMIN was omitted from the org-wide branches of the scope
-- functions. The transition matrix grants SYSTEM_ADMIN on assigned-status
-- transitions and top-of-chain SLA alerts target them, but the scope check
-- then vetoed those same transitions (out_of_scope) and hid assigned
-- reports from their RLS reads. Add SYSTEM_ADMIN alongside the other
-- org-wide roles in both functions.

create or replace function public.civic_unit_scope(p_user uuid, p_org uuid)
returns setof uuid
language sql stable security definer set search_path = public as $$
    with recursive
    base as (
        select r.org_unit_id as id
        from public.user_roles r
        where r.user_id = p_user and r.is_active and r.organization_id = p_org
          and r.role in ('MJUMBE','MWENYEKITI','MTAA_OFFICER','WARD_OFFICER','DEPARTMENT_OFFICER')
        union
        select u.id
        from public.user_roles r
        join public.org_units u on u.organization_id = r.organization_id
        where r.user_id = p_user and r.is_active and r.organization_id = p_org
          and r.role in ('MUNICIPAL_OFFICER','COUNCIL_ADMIN','SYSTEM_ADMIN')
    ),
    tree as (
        select id from base
        union
        select u.id from public.org_units u join tree t on u.parent_id = t.id
    )
    select id from tree
$$;

create or replace function public.civic_has_report_scope(
    p_user uuid, p_org uuid, p_unit uuid, p_assigned_unit uuid
)
returns boolean
language sql stable security definer set search_path = public as $$
    select
        exists (select 1 from public.user_roles r
                where r.user_id = p_user and r.is_active
                  and r.organization_id = p_org
                  and r.role in ('MUNICIPAL_OFFICER','COUNCIL_ADMIN','SYSTEM_ADMIN'))
        or exists (select 1 from public.civic_unit_scope(p_user, p_org) s
                   where s = coalesce(p_assigned_unit, p_unit))
$$;
