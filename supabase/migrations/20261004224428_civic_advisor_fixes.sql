-- ============================================================
-- 025: CIVIC ADVISOR FIXES
-- Fixes from `supabase db advisors` after 024:
--   1. civic_is_org_wide had a role-mutable search_path
--   2. trigger functions were executable via RPC by anon/authenticated
--   3. multiple permissive SELECT policies per role+command
--      (ALL admin policies overlapped the SELECT policies)
-- ============================================================

-- 1. Lock search_path on the remaining helper
create or replace function public.civic_is_org_wide(p_role text)
returns boolean
language sql stable set search_path = public, pg_temp as $$
    select p_role in ('MUNICIPAL_OFFICER', 'COUNCIL_ADMIN', 'SYSTEM_ADMIN')
$$;

-- 2. Trigger functions are not RPC endpoints
revoke execute on function public.civic_set_public_reference() from public, anon, authenticated;
revoke execute on function public.civic_refresh_report_count() from public, anon, authenticated;

-- 3. Replace ALL-command admin policies with per-command policies so
--    they no longer duplicate the SELECT path.
alter policy "orgs admin write" on public.organizations
    rename to "orgs admin write legacy";
drop policy "orgs admin write legacy" on public.organizations;
create policy "orgs admin insert" on public.organizations
    for insert to authenticated
    with check (public.civic_is_system_admin((select auth.uid())));
create policy "orgs admin update" on public.organizations
    for update to authenticated
    using (public.civic_is_system_admin((select auth.uid())))
    with check (public.civic_is_system_admin((select auth.uid())));
create policy "orgs admin delete" on public.organizations
    for delete to authenticated
    using (public.civic_is_system_admin((select auth.uid())));

drop policy "unit types admin write" on public.org_unit_types;
create policy "unit types admin insert" on public.org_unit_types
    for insert to authenticated
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = org_unit_types.organization_id)
    );
create policy "unit types admin update" on public.org_unit_types
    for update to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = org_unit_types.organization_id)
    )
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = org_unit_types.organization_id)
    );
create policy "unit types admin delete" on public.org_unit_types
    for delete to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = org_unit_types.organization_id)
    );

drop policy "units admin write" on public.org_units;
create policy "units admin insert" on public.org_units
    for insert to authenticated
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = org_units.organization_id)
    );
create policy "units admin update" on public.org_units
    for update to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = org_units.organization_id)
    )
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = org_units.organization_id)
    );
create policy "units admin delete" on public.org_units
    for delete to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = org_units.organization_id)
    );

drop policy "categories admin write" on public.civic_categories;
create policy "categories admin insert" on public.civic_categories
    for insert to authenticated
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = civic_categories.organization_id)
    );
create policy "categories admin update" on public.civic_categories
    for update to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = civic_categories.organization_id)
    )
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = civic_categories.organization_id)
    );
create policy "categories admin delete" on public.civic_categories
    for delete to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = civic_categories.organization_id)
    );

drop policy "sla admin write" on public.sla_rules;
create policy "sla admin insert" on public.sla_rules
    for insert to authenticated
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = sla_rules.organization_id)
    );
create policy "sla admin update" on public.sla_rules
    for update to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = sla_rules.organization_id)
    )
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = sla_rules.organization_id)
    );
create policy "sla admin delete" on public.sla_rules
    for delete to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = sla_rules.organization_id)
    );

-- 4. Merge duplicate SELECT policies (single policy, OR'd conditions)
drop policy "user roles self" on public.user_roles;
drop policy "user roles org admin" on public.user_roles;
create policy "user roles read" on public.user_roles
    for select to authenticated using (
        (select auth.uid()) = user_id
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.organization_id = user_roles.organization_id
                     and ar.role in ('COUNCIL_ADMIN','MUNICIPAL_OFFICER','MTAA_OFFICER','WARD_OFFICER'))
        or public.civic_is_system_admin((select auth.uid()))
    );
drop policy "user roles admin write" on public.user_roles;
create policy "user roles admin insert" on public.user_roles
    for insert to authenticated
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = user_roles.organization_id)
    );
create policy "user roles admin update" on public.user_roles
    for update to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = user_roles.organization_id)
    )
    with check (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = user_roles.organization_id)
    );
create policy "user roles admin delete" on public.user_roles
    for delete to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.role = 'COUNCIL_ADMIN' and ar.organization_id = user_roles.organization_id)
    );

drop policy "reports reporter own" on public.civic_reports;
drop policy "reports official scope" on public.civic_reports;
create policy "reports read" on public.civic_reports
    for select to authenticated using (
        (select auth.uid()) = reporter_id
        or public.civic_has_report_scope(
            (select auth.uid()), organization_id, unit_id, assigned_unit_id
        )
    );
