-- ============================================================
-- 024: CIVIC GOVERNMENT CORE
-- Two-sided civic platform: configurable org hierarchy, civic RBAC,
-- civic reports with 12-status state machine, SLA rules, escalations,
-- immutable audit log. Existing resident tables are untouched.
-- ============================================================

-- ------------------------------------------------------------
-- 2. ORGANIZATIONS + CONFIGURABLE UNIT TREE
-- ------------------------------------------------------------

create table if not exists public.organizations (
    id uuid primary key default gen_random_uuid(),
    slug text not null unique,
    name text not null,
    org_type text not null default 'COUNCIL'
        check (org_type in ('COUNCIL','MUNICIPALITY','DISTRICT','TOWN','AGENCY')),
    region text,
    country text not null default 'Tanzania',
    contact_email text,
    contact_phone text,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Unit types are rows, not enums: every organization defines its own
-- hierarchy names and depth (spec: councils differ).
create table if not exists public.org_unit_types (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations(id) on delete cascade,
    name text not null,
    level integer not null,
    is_department boolean not null default false,
    description text,
    unique (organization_id, name)
);
create index if not exists idx_org_unit_types_org on public.org_unit_types(organization_id);

create table if not exists public.org_units (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations(id) on delete cascade,
    unit_type_id uuid not null references public.org_unit_types(id) on delete restrict,
    parent_id uuid references public.org_units(id) on delete set null,
    name text not null,
    code text,
    -- Bridge to the resident side: MTAA-level units bind to existing
    -- neighborhoods so profiles.neighborhood_id resolves to a civic unit.
    neighborhood_id uuid references public.neighborhoods(id) on delete set null,
    latitude double precision,
    longitude double precision,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (organization_id, unit_type_id, name)
);
create index if not exists idx_org_units_org on public.org_units(organization_id);
create index if not exists idx_org_units_parent on public.org_units(parent_id);
create index if not exists idx_org_units_neighborhood on public.org_units(neighborhood_id);

-- ------------------------------------------------------------
-- 3. CIVIC ROLES (officials only; RESIDENT is implicit for every
--    authenticated user and is never stored as a row)
-- ------------------------------------------------------------

create table if not exists public.user_roles (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    organization_id uuid not null references public.organizations(id) on delete cascade,
    org_unit_id uuid not null references public.org_units(id) on delete cascade,
    role text not null check (role in (
        'MJUMBE','MWENYEKITI','MTAA_OFFICER','WARD_OFFICER','MUNICIPAL_OFFICER',
        'DEPARTMENT_OFFICER','COUNCIL_ADMIN','SYSTEM_ADMIN'
    )),
    assigned_by uuid references auth.users(id) on delete set null,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (user_id, org_unit_id, role)
);
create index if not exists idx_user_roles_user on public.user_roles(user_id) where is_active;
create index if not exists idx_user_roles_unit on public.user_roles(org_unit_id) where is_active;
create index if not exists idx_user_roles_org on public.user_roles(organization_id, role) where is_active;

-- ------------------------------------------------------------
-- 4. CIVIC CATEGORIES (per-org, Swahili-first labels)
-- ------------------------------------------------------------

create table if not exists public.civic_categories (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations(id) on delete cascade,
    name_en text not null,
    name_sw text not null,
    description text,
    icon text,
    sort_order integer not null default 0,
    requires_verification boolean not null default true,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    unique (organization_id, name_en)
);
create index if not exists idx_civic_categories_org on public.civic_categories(organization_id);

-- ------------------------------------------------------------
-- 5. SLA RULES (configurable; no hard-coded deadlines)
--    Most-specific match wins: category > severity > priority.
-- ------------------------------------------------------------

create table if not exists public.sla_rules (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null references public.organizations(id) on delete cascade,
    category_id uuid references public.civic_categories(id) on delete cascade,
    severity text check (severity in ('LOW','MEDIUM','HIGH','CRITICAL')),
    priority text check (priority in ('LOW','NORMAL','HIGH','URGENT')),
    response_hours numeric(6,2) not null,
    resolution_hours numeric(6,2) not null,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists idx_sla_rules_org on public.sla_rules(organization_id) where is_active;

-- ------------------------------------------------------------
-- 6. CIVIC REPORTS
-- ------------------------------------------------------------

create sequence if not exists public.civic_report_ref_seq start 1;

create table if not exists public.civic_reports (
    id uuid primary key default gen_random_uuid(),
    -- Generated: {org-slug}-{year}-{00001}
    public_reference text not null unique,
    organization_id uuid not null references public.organizations(id) on delete restrict,
    category_id uuid not null references public.civic_categories(id) on delete restrict,

    -- Always recorded (anti-abuse + contact); hidden publicly when anonymous
    reporter_id uuid references public.profiles(id) on delete set null,
    is_anonymous boolean not null default false,

    title text not null check (length(title) between 3 and 200),
    description text not null check (length(description) between 10 and 5000),
    description_ai text,

    latitude double precision,
    longitude double precision,
    geohash text,
    location_description text,

    severity text not null default 'MEDIUM'
        check (severity in ('LOW','MEDIUM','HIGH','CRITICAL')),
    priority text not null default 'NORMAL'
        check (priority in ('LOW','NORMAL','HIGH','URGENT')),
    status text not null default 'NEW'
        check (status in (
            'NEW','AI_TRIAGED','AWAITING_VERIFICATION','VERIFIED','REJECTED',
            'ASSIGNED','IN_PROGRESS','WAITING_EXTERNAL_ACTION',
            'AWAITING_RESIDENT_CONFIRMATION','RESOLVED','CLOSED','ESCALATED'
        )),
    verification_status text not null default 'PENDING'
        check (verification_status in ('PENDING','VERIFIED','REJECTED')),

    -- Origin/responsibility routing
    unit_id uuid references public.org_units(id) on delete set null,
    assigned_unit_id uuid references public.org_units(id) on delete set null,
    responsible_department_id uuid references public.org_units(id) on delete set null,

    -- Duplicate clustering: duplicates point at the canonical issue
    canonical_report_id uuid references public.civic_reports(id) on delete set null,
    report_count integer not null default 1,

    ai_confidence numeric(4,3),
    ai_summary text,
    source text not null default 'APP'
        check (source in ('APP','WHATSAPP','SMS','PHONE','WEB')),

    due_at timestamptz,
    escalation_level integer not null default 0,

    verified_by uuid references public.profiles(id) on delete set null,
    verified_at timestamptz,
    assigned_at timestamptz,
    resolved_at timestamptz,
    resolved_by uuid references public.profiles(id) on delete set null,
    closed_at timestamptz,
    resolution_description text,

    resident_confirmation text
        check (resident_confirmation in ('CONFIRMED','REJECTED')),
    confirmed_at timestamptz,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_civic_reports_org_status on public.civic_reports(organization_id, status);
create index if not exists idx_civic_reports_unit on public.civic_reports(unit_id);
create index if not exists idx_civic_reports_assigned_unit on public.civic_reports(assigned_unit_id);
create index if not exists idx_civic_reports_responsible_dept on public.civic_reports(responsible_department_id);
create index if not exists idx_civic_reports_reporter on public.civic_reports(reporter_id);
create index if not exists idx_civic_reports_category on public.civic_reports(category_id);
create index if not exists idx_civic_reports_geohash on public.civic_reports(geohash);
create index if not exists idx_civic_reports_canonical on public.civic_reports(canonical_report_id)
    where canonical_report_id is not null;
create index if not exists idx_civic_reports_due on public.civic_reports(due_at)
    where status in ('ASSIGNED','IN_PROGRESS','WAITING_EXTERNAL_ACTION','ESCALATED');
create index if not exists idx_civic_reports_created on public.civic_reports(created_at desc);

-- public_reference generator: {slug}-{year}-{00001}
create or replace function public.civic_set_public_reference()
returns trigger
language plpgsql security definer set search_path = public as $$
declare
    v_slug text;
begin
    if new.public_reference is null or new.public_reference = '' then
        select slug into v_slug from public.organizations where id = new.organization_id;
        new.public_reference := v_slug || '-' || to_char(now(), 'YYYY') || '-'
            || lpad(nextval('public.civic_report_ref_seq')::text, 5, '0');
    end if;
    return new;
end $$;

drop trigger if exists trg_civic_reports_reference on public.civic_reports;
create trigger trg_civic_reports_reference
    before insert on public.civic_reports
    for each row execute function public.civic_set_public_reference();

-- Keep canonical.report_count = duplicates + 1 (the "N residents reported" figure)
create or replace function public.civic_refresh_report_count()
returns trigger
language plpgsql security definer set search_path = public as $$
begin
    if new.canonical_report_id is not null then
        update public.civic_reports c
        set report_count = (
                select count(*) + 1 from public.civic_reports d
                where d.canonical_report_id = c.id
            )
        where c.id = new.canonical_report_id;
    end if;
    if old is not null and old.canonical_report_id is not null
       and (tg_op = 'UPDATE' and old.canonical_report_id is distinct from new.canonical_report_id) then
        update public.civic_reports c
        set report_count = (
                select count(*) + 1 from public.civic_reports d
                where d.canonical_report_id = c.id
            )
        where c.id = old.canonical_report_id;
    end if;
    return null;
end $$;

drop trigger if exists trg_civic_reports_count on public.civic_reports;
create trigger trg_civic_reports_count
    after insert or update of canonical_report_id on public.civic_reports
    for each row execute function public.civic_refresh_report_count();

-- ------------------------------------------------------------
-- 7. EVIDENCE
-- ------------------------------------------------------------

create table if not exists public.civic_report_evidence (
    id uuid primary key default gen_random_uuid(),
    report_id uuid not null references public.civic_reports(id) on delete cascade,
    uploaded_by uuid references public.profiles(id) on delete set null,
    storage_path text not null,
    mime_type text,
    kind text not null default 'PHOTO'
        check (kind in ('PHOTO','VIDEO','AUDIO','DOCUMENT')),
    created_at timestamptz not null default now()
);
create index if not exists idx_civic_evidence_report on public.civic_report_evidence(report_id);

-- ------------------------------------------------------------
-- 8. STATUS HISTORY (written ONLY by transition_report RPC)
-- ------------------------------------------------------------

create table if not exists public.civic_report_status_history (
    id uuid primary key default gen_random_uuid(),
    report_id uuid not null references public.civic_reports(id) on delete cascade,
    from_status text,
    to_status text not null,
    changed_by uuid references public.profiles(id) on delete set null,
    changed_by_role text,
    reason text,
    created_at timestamptz not null default now()
);
create index if not exists idx_civic_history_report on public.civic_report_status_history(report_id, created_at);

-- ------------------------------------------------------------
-- 9. TRANSITION MATRIX (configurable state machine)
--    actor_kind: OFFICIAL (role-checked, scope-checked) |
--                REPORTER (own report) | SYSTEM (service_role only)
-- ------------------------------------------------------------

create table if not exists public.civic_transitions (
    id uuid primary key default gen_random_uuid(),
    from_status text not null,
    to_status text not null,
    actor_kind text not null check (actor_kind in ('OFFICIAL','REPORTER','SYSTEM')),
    allowed_roles text[],
    unique (from_status, to_status, actor_kind)
);

-- ------------------------------------------------------------
-- 10. ASSIGNMENTS
-- ------------------------------------------------------------

create table if not exists public.civic_report_assignments (
    id uuid primary key default gen_random_uuid(),
    report_id uuid not null references public.civic_reports(id) on delete cascade,
    assigned_to uuid not null references public.profiles(id) on delete cascade,
    assigned_by uuid not null references public.profiles(id) on delete set null,
    org_unit_id uuid references public.org_units(id) on delete set null,
    note text,
    status text not null default 'ACTIVE'
        check (status in ('ACTIVE','COMPLETED','REASSIGNED','CANCELLED')),
    assigned_at timestamptz not null default now(),
    completed_at timestamptz
);
create index if not exists idx_civic_assignments_report on public.civic_report_assignments(report_id);
create index if not exists idx_civic_assignments_active on public.civic_report_assignments(assigned_to)
    where status = 'ACTIVE';

-- ------------------------------------------------------------
-- 11. ESCALATIONS
-- ------------------------------------------------------------

create table if not exists public.civic_escalations (
    id uuid primary key default gen_random_uuid(),
    report_id uuid not null references public.civic_reports(id) on delete cascade,
    from_unit_id uuid references public.org_units(id) on delete set null,
    to_unit_id uuid not null references public.org_units(id) on delete restrict,
    from_level integer,
    to_level integer,
    trigger_type text not null
        check (trigger_type in ('SLA_BREACH','OFFICER','RESIDENT_APPEAL','AI_RECOMMENDATION')),
    reason text,
    created_by uuid references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    acknowledged_at timestamptz,
    resolved_at timestamptz
);
create index if not exists idx_civic_escalations_report on public.civic_escalations(report_id);
create index if not exists idx_civic_escalations_open on public.civic_escalations(to_unit_id)
    where resolved_at is null;

-- ------------------------------------------------------------
-- 12. AUDIT LOG (append-only; no UPDATE/DELETE path exists at all)
-- ------------------------------------------------------------

create table if not exists public.audit_logs (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid references public.organizations(id) on delete set null,
    actor_id uuid references public.profiles(id) on delete set null,
    actor_role text,
    entity_type text not null,
    entity_id uuid not null,
    action text not null,
    previous_value jsonb,
    new_value jsonb,
    reason text,
    metadata jsonb,
    created_at timestamptz not null default now()
);
create index if not exists idx_audit_entity on public.audit_logs(entity_type, entity_id, created_at);
create index if not exists idx_audit_org on public.audit_logs(organization_id, created_at desc);
create index if not exists idx_audit_actor on public.audit_logs(actor_id);

-- ------------------------------------------------------------
-- 13. RBAC HELPERS (SECURITY DEFINER to avoid RLS recursion;
--     both check auth.uid() internally or take it as a parameter
--     and are only granted EXECUTE to authenticated/service_role)
-- ------------------------------------------------------------

create or replace function public.civic_active_roles(p_user uuid)
returns table (role text, organization_id uuid, org_unit_id uuid)
language sql stable security definer set search_path = public as $$
    select r.role, r.organization_id, r.org_unit_id
    from public.user_roles r
    where r.user_id = p_user and r.is_active
$$;

create or replace function public.civic_is_system_admin(p_user uuid)
returns boolean
language sql stable security definer set search_path = public as $$
    select exists (
        select 1 from public.user_roles r
        where r.user_id = p_user and r.is_active and r.role = 'SYSTEM_ADMIN'
    )
$$;

-- Org-wide official roles (see every unit in their organization)
create or replace function public.civic_is_org_wide(p_role text)
returns boolean
language sql stable as $$
    select p_role in ('MUNICIPAL_OFFICER', 'COUNCIL_ADMIN', 'SYSTEM_ADMIN')
$$;

-- All org_unit_ids a user governs in an org: their own units plus every
-- descendant (Mjumbe -> street, Mwenyekiti -> whole mtaa, WEO -> ward, ...).
-- Org-wide roles get every unit of the org.
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
          and r.role in ('MUNICIPAL_OFFICER','COUNCIL_ADMIN')
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
                  and r.role in ('MUNICIPAL_OFFICER','COUNCIL_ADMIN'))
        or exists (select 1 from public.civic_unit_scope(p_user, p_org) s
                   where s = coalesce(p_assigned_unit, p_unit))
$$;

revoke all on function public.civic_active_roles(uuid) from public, anon;
revoke all on function public.civic_is_system_admin(uuid) from public, anon;
revoke all on function public.civic_is_org_wide(text) from public, anon;
revoke all on function public.civic_unit_scope(uuid, uuid) from public, anon;
revoke all on function public.civic_has_report_scope(uuid, uuid, uuid, uuid) from public, anon;
grant execute on function public.civic_active_roles(uuid),
    public.civic_is_system_admin(uuid),
    public.civic_is_org_wide(text),
    public.civic_unit_scope(uuid, uuid),
    public.civic_has_report_scope(uuid, uuid, uuid, uuid)
    to authenticated, service_role;

-- ------------------------------------------------------------
-- 14. RLS
-- ------------------------------------------------------------

alter table public.organizations            enable row level security;
alter table public.org_unit_types           enable row level security;
alter table public.org_units                enable row level security;
alter table public.user_roles               enable row level security;
alter table public.civic_categories         enable row level security;
alter table public.sla_rules                enable row level security;
alter table public.civic_reports            enable row level security;
alter table public.civic_report_evidence    enable row level security;
alter table public.civic_report_status_history enable row level security;
alter table public.civic_transitions        enable row level security;
alter table public.civic_report_assignments enable row level security;
alter table public.civic_escalations        enable row level security;
alter table public.audit_logs               enable row level security;

-- Config tables: readable by authenticated, writable by org/system admins
create policy "orgs readable" on public.organizations
    for select to authenticated using (true);
create policy "orgs admin write" on public.organizations
    for all to authenticated
    using (public.civic_is_system_admin((select auth.uid())))
    with check (public.civic_is_system_admin((select auth.uid())));

create policy "unit types readable" on public.org_unit_types
    for select to authenticated using (true);
create policy "unit types admin write" on public.org_unit_types
    for all to authenticated
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

create policy "units readable" on public.org_units
    for select to authenticated using (true);
create policy "units admin write" on public.org_units
    for all to authenticated
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

create policy "categories readable" on public.civic_categories
    for select to authenticated using (true);
create policy "categories admin write" on public.civic_categories
    for all to authenticated
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

create policy "sla readable" on public.sla_rules
    for select to authenticated using (true);
create policy "sla admin write" on public.sla_rules
    for all to authenticated
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

-- user_roles: see your own; admins see within their org
create policy "user roles self" on public.user_roles
    for select to authenticated using ((select auth.uid()) = user_id);
create policy "user roles org admin" on public.user_roles
    for select to authenticated using (
        exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                where ar.organization_id = user_roles.organization_id
                  and ar.role in ('COUNCIL_ADMIN','MUNICIPAL_OFFICER','MTAA_OFFICER','WARD_OFFICER'))
        or public.civic_is_system_admin((select auth.uid()))
    );
create policy "user roles admin write" on public.user_roles
    for all to authenticated
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

-- transitions matrix: read-only reference data
create policy "transitions readable" on public.civic_transitions
    for select to authenticated using (true);

-- civic_reports: reporter sees own; officials see scoped
create policy "reports reporter own" on public.civic_reports
    for select to authenticated
    using ((select auth.uid()) = reporter_id);
create policy "reports official scope" on public.civic_reports
    for select to authenticated
    using (
        public.civic_has_report_scope(
            (select auth.uid()), organization_id, unit_id, assigned_unit_id
        )
    );
-- Direct UPDATE is never allowed; all changes flow through transition_report().
create policy "reports insert self" on public.civic_reports
    for insert to authenticated
    with check (
        (select auth.uid()) is not null
        and reporter_id = (select auth.uid())
    );
-- No UPDATE/DELETE policies on civic_reports for any role.

-- evidence mirrors report visibility
create policy "evidence report scope" on public.civic_report_evidence
    for select to authenticated
    using (
        exists (
            select 1 from public.civic_reports r
            where r.id = report_id
              and (
                    r.reporter_id = (select auth.uid())
                    or public.civic_has_report_scope(
                        (select auth.uid()), r.organization_id, r.unit_id, r.assigned_unit_id)
              )
        )
    );
create policy "evidence insert scope" on public.civic_report_evidence
    for insert to authenticated
    with check (
        exists (
            select 1 from public.civic_reports r
            where r.id = report_id
              and (
                    r.reporter_id = (select auth.uid())
                    or public.civic_has_report_scope(
                        (select auth.uid()), r.organization_id, r.unit_id, r.assigned_unit_id)
              )
        )
        and uploaded_by = (select auth.uid())
    );

-- history/assignments/escalations: scoped read; writes via RPC only
create policy "history report scope" on public.civic_report_status_history
    for select to authenticated
    using (
        exists (
            select 1 from public.civic_reports r
            where r.id = report_id
              and (
                    r.reporter_id = (select auth.uid())
                    or public.civic_has_report_scope(
                        (select auth.uid()), r.organization_id, r.unit_id, r.assigned_unit_id)
              )
        )
    );
create policy "assignments report scope" on public.civic_report_assignments
    for select to authenticated
    using (
        exists (
            select 1 from public.civic_reports r
            where r.id = report_id
              and (
                    r.reporter_id = (select auth.uid())
                    or public.civic_has_report_scope(
                        (select auth.uid()), r.organization_id, r.unit_id, r.assigned_unit_id)
              )
        )
    );
create policy "escalations report scope" on public.civic_escalations
    for select to authenticated
    using (
        exists (
            select 1 from public.civic_reports r
            where r.id = report_id
              and (
                    r.reporter_id = (select auth.uid())
                    or public.civic_has_report_scope(
                        (select auth.uid()), r.organization_id, r.unit_id, r.assigned_unit_id)
              )
        )
    );

-- audit: read for org-wide officials + system admin; nobody writes directly
create policy "audit official read" on public.audit_logs
    for select to authenticated
    using (
        public.civic_is_system_admin((select auth.uid()))
        or exists (select 1 from public.civic_active_roles((select auth.uid())) ar
                   where ar.organization_id = audit_logs.organization_id
                     and ar.role in ('MUNICIPAL_OFFICER','COUNCIL_ADMIN'))
    );

revoke all on public.audit_logs from anon;
revoke insert, update, delete on public.audit_logs from authenticated;
revoke insert, update, delete on public.civic_report_status_history from anon, authenticated;
revoke update, delete on public.civic_reports from anon, authenticated;
revoke update, delete on public.civic_transitions from anon, authenticated;

-- ------------------------------------------------------------
-- 15. SLA MATCHING
-- ------------------------------------------------------------

create or replace function public.civic_resolve_sla(
    p_org uuid, p_category uuid, p_severity text, p_priority text
)
returns table (response_hours numeric, resolution_hours numeric)
language sql stable security definer set search_path = public as $$
    select r.response_hours, r.resolution_hours
    from public.sla_rules r
    where r.is_active and r.organization_id = p_org
      and (r.category_id is null or r.category_id = p_category)
      and (r.severity is null or r.severity = p_severity)
      and (r.priority is null or r.priority = p_priority)
    order by
        (case when r.category_id is not null then 4 else 0 end)
      + (case when r.severity    is not null then 2 else 0 end)
      + (case when r.priority    is not null then 1 else 0 end) desc,
        r.updated_at desc
    limit 1
$$;

revoke all on function public.civic_resolve_sla(uuid, uuid, text, text) from public, anon;
grant execute on function public.civic_resolve_sla(uuid, uuid, text, text)
    to authenticated, service_role;

-- ------------------------------------------------------------
-- 16. STATE MACHINE RPC — the ONLY way report status changes
-- ------------------------------------------------------------

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
begin
    -- Resolve actor context
    if v_is_system then
        v_display_role := 'SYSTEM';
    elsif v_actor is not null then
        select ar.role into v_role
        from public.civic_active_roles(v_actor) ar
        where ar.organization_id = (select organization_id from public.civic_reports where id = p_report_id)
        order by case ar.role
            when 'SYSTEM_ADMIN' then 1 when 'COUNCIL_ADMIN' then 2 when 'MUNICIPAL_OFFICER' then 3
            when 'WARD_OFFICER' then 4 when 'MTAA_OFFICER' then 5 when 'MWENYEKITI' then 6
            when 'DEPARTMENT_OFFICER' then 7 when 'MJUMBE' then 8 end
        limit 1;
    end if;

    if v_actor is null and not v_is_system then
        return jsonb_build_object('ok', false, 'error', 'unauthenticated');
    end if;

    select * into v_report from public.civic_reports where id = p_report_id for update;
    if not found then
        return jsonb_build_object('ok', false, 'error', 'report_not_found');
    end if;

    if v_to_status = v_report.status then
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

-- ------------------------------------------------------------
-- 17. PUBLIC STATUS LOOKUP (transparency without exposure)
--     "Reported" labeling + description hidden until verified;
--     no reporter identity ever.
-- ------------------------------------------------------------

create or replace function public.public_issue_status(p_reference text)
returns jsonb
language sql
security definer
set search_path = public
as $$
    select jsonb_build_object(
        'public_reference', r.public_reference,
        'status', r.status,
        'category_en', c.name_en,
        'category_sw', c.name_sw,
        'verified', r.verification_status = 'VERIFIED',
        -- Unverified reports never publicly accuse: no title, no description
        'title', case when r.verification_status = 'VERIFIED' then r.title else null end,
        'description', case when r.verification_status = 'VERIFIED' then r.description else null end,
        'residents_reported', r.report_count,
        'created_at', r.created_at,
        'resolved_at', r.resolved_at,
        'closed_at', r.closed_at,
        'timeline', (
            select jsonb_agg(jsonb_build_object(
                       'from', h.from_status, 'to', h.to_status, 'at', h.created_at)
                       order by h.created_at)
            from public.civic_report_status_history h
            where h.report_id = r.id
        )
    )
    from public.civic_reports r
    join public.civic_categories c on c.id = r.category_id
    where r.public_reference = p_reference
$$;

revoke all on function public.public_issue_status(text) from public, anon;
grant execute on function public.public_issue_status(text) to anon, authenticated;

-- ------------------------------------------------------------
-- 18. SEED — demo organization (idempotent)
--     SYSTEM_ADMIN bootstrap is done via SQL by the project owner:
--       insert into user_roles (user_id, organization_id, org_unit_id, role)
--       select '<uid>', o.id, u.id, 'SYSTEM_ADMIN'
--       from organizations o, org_units u
--       where o.slug='ilala-mc' and u.name='Ilala Municipal Council';
-- ------------------------------------------------------------

insert into public.organizations (slug, name, org_type, region, country)
values ('ilala-mc', 'Ilala Municipal Council', 'MUNICIPALITY', 'Dar es Salaam', 'Tanzania')
on conflict (slug) do nothing;

insert into public.org_unit_types (organization_id, name, level, is_department)
select o.id, t.name, t.level, t.is_department
from public.organizations o
join (values
    ('COUNCIL', 1, false),
    ('DEPARTMENT', 2, true),
    ('WARD', 2, false),
    ('MTAA', 3, false),
    ('ZONE', 4, false)
) as t(name, level, is_department) on true
where o.slug = 'ilala-mc'
on conflict (organization_id, name) do nothing;

-- Units: council root -> ward -> mtaa (linked to an existing neighborhood when
-- one matches) -> zone; plus two departments
insert into public.org_units (organization_id, unit_type_id, parent_id, name, code, neighborhood_id)
select o.id, ut.id, null, 'Ilala Municipal Council', 'ILM', null
from public.organizations o
join public.org_unit_types ut on ut.organization_id = o.id and ut.name = 'COUNCIL'
where o.slug = 'ilala-mc'
on conflict (organization_id, unit_type_id, name) do nothing;

insert into public.org_units (organization_id, unit_type_id, parent_id, name, code, neighborhood_id)
select o.id, ut.id, root.id, 'Kunduchi Ward', 'ILM-KND',
       (select n.id from public.neighborhoods n where lower(n.name) = 'kunduchi' limit 1)
from public.organizations o
join public.org_unit_types ut on ut.organization_id = o.id and ut.name = 'WARD'
join public.org_units root on root.organization_id = o.id and root.code = 'ILM'
where o.slug = 'ilala-mc'
on conflict (organization_id, unit_type_id, name) do nothing;

insert into public.org_units (organization_id, unit_type_id, parent_id, name, code, neighborhood_id)
select o.id, ut.id, ward.id, 'Kunduchi Mtaa', 'ILM-KND-M1',
       (select n.id from public.neighborhoods n where lower(n.name) like 'kunduchi%' limit 1)
from public.organizations o
join public.org_unit_types ut on ut.organization_id = o.id and ut.name = 'MTAA'
join public.org_units ward on ward.organization_id = o.id and ward.code = 'ILM-KND'
where o.slug = 'ilala-mc'
on conflict (organization_id, unit_type_id, name) do nothing;

insert into public.org_units (organization_id, unit_type_id, parent_id, name, code, neighborhood_id)
select o.id, ut.id, mtaa.id, z.name, 'ILM-KND-M1-' || z.name, null
from public.organizations o
join public.org_unit_types ut on ut.organization_id = o.id and ut.name = 'ZONE'
join public.org_units mtaa on mtaa.organization_id = o.id and mtaa.code = 'ILM-KND-M1'
join (values ('Zone A'), ('Zone B')) as z(name) on true
where o.slug = 'ilala-mc'
on conflict (organization_id, unit_type_id, name) do nothing;

insert into public.org_units (organization_id, unit_type_id, parent_id, name, code, neighborhood_id)
select o.id, ut.id, root.id, d.name, 'ILM-D-' || upper(substr(replace(d.name, ' ', ''), 1, 6)), null
from public.organizations o
join public.org_unit_types ut on ut.organization_id = o.id and ut.name = 'DEPARTMENT'
join public.org_units root on root.organization_id = o.id and root.code = 'ILM'
join (values ('Public Works'), ('Water and Sanitation'), ('Waste Management'), ('Health')) as d(name) on true
where o.slug = 'ilala-mc'
on conflict (organization_id, unit_type_id, name) do nothing;

-- Categories (spec section 4, Swahili-first)
insert into public.civic_categories (organization_id, name_en, name_sw, sort_order)
select o.id, c.name_en, c.name_sw, c.sort_order
from public.organizations o
join (values
    ('Waste', 'Taka', 1),
    ('Roads', 'Barabara', 2),
    ('Drainage', 'Majarazi', 3),
    ('Water', 'Maji', 4),
    ('Electricity', 'Umeme', 5),
    ('Street Lighting', 'Taa za Mtaani', 6),
    ('Public Safety', 'Usalama wa Umma', 7),
    ('Environment', 'Mazingira', 8),
    ('Noise', 'Kelele', 9),
    ('Public Facilities', 'Vitu vya Umma', 10),
    ('Traffic', 'Magenjembe', 11),
    ('Health', 'Afya', 12),
    ('Education', 'Elimu', 13),
    ('Community Events', 'Matukio ya Jamii', 14),
    ('Other', 'Nyingine', 15)
) as c(name_en, name_sw, sort_order) on true
where o.slug = 'ilala-mc'
on conflict (organization_id, name_en) do nothing;

-- SLA defaults (configuration examples, not legal requirements)
insert into public.sla_rules (organization_id, category_id, severity, priority, response_hours, resolution_hours)
select o.id, null, null, null, 24, 72
from public.organizations o where o.slug = 'ilala-mc'
  and not exists (select 1 from public.sla_rules s where s.organization_id = o.id);

insert into public.sla_rules (organization_id, category_id, severity, priority, response_hours, resolution_hours)
select o.id, null, s.severity, null, s.response_hours, s.resolution_hours
from public.organizations o
join (values
    ('HIGH', 4, 24),
    ('CRITICAL', 2, 8),
    ('LOW', 48, 120)
) as s(severity, response_hours, resolution_hours) on true
where o.slug = 'ilala-mc'
  and not exists (
      select 1 from public.sla_rules r
      where r.organization_id = o.id and r.severity = s.severity and r.category_id is null and r.priority is null
  );

-- Transition matrix (spec section 5)
insert into public.civic_transitions (from_status, to_status, actor_kind, allowed_roles)
select t.from_status, t.to_status, t.actor_kind, t.allowed_roles::text[]
from (values
    ('NEW','AI_TRIAGED','SYSTEM',null),
    ('NEW','AWAITING_VERIFICATION','SYSTEM',null),
    ('NEW','AI_TRIAGED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('NEW','AWAITING_VERIFICATION','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('NEW','REJECTED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('AI_TRIAGED','AWAITING_VERIFICATION','SYSTEM',null),
    ('AI_TRIAGED','AWAITING_VERIFICATION','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('AI_TRIAGED','REJECTED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('AWAITING_VERIFICATION','VERIFIED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('AWAITING_VERIFICATION','REJECTED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('REJECTED','AWAITING_VERIFICATION','REPORTER',null),
    ('REJECTED','AWAITING_VERIFICATION','OFFICIAL','{MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('VERIFIED','ASSIGNED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('VERIFIED','REJECTED','OFFICIAL','{WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('ASSIGNED','IN_PROGRESS','OFFICIAL','{MJUMBE,DEPARTMENT_OFFICER,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('ASSIGNED','VERIFIED','OFFICIAL','{DEPARTMENT_OFFICER,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('IN_PROGRESS','WAITING_EXTERNAL_ACTION','OFFICIAL','{MJUMBE,DEPARTMENT_OFFICER,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('IN_PROGRESS','AWAITING_RESIDENT_CONFIRMATION','OFFICIAL','{MJUMBE,DEPARTMENT_OFFICER,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('IN_PROGRESS','ASSIGNED','OFFICIAL','{MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('WAITING_EXTERNAL_ACTION','IN_PROGRESS','OFFICIAL','{MJUMBE,DEPARTMENT_OFFICER,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('WAITING_EXTERNAL_ACTION','AWAITING_RESIDENT_CONFIRMATION','OFFICIAL','{MJUMBE,DEPARTMENT_OFFICER,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('WAITING_EXTERNAL_ACTION','ESCALATED','OFFICIAL','{MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('WAITING_EXTERNAL_ACTION','ESCALATED','SYSTEM',null),
    ('AWAITING_RESIDENT_CONFIRMATION','RESOLVED','REPORTER',null),
    ('AWAITING_RESIDENT_CONFIRMATION','IN_PROGRESS','REPORTER',null),
    ('AWAITING_RESIDENT_CONFIRMATION','IN_PROGRESS','OFFICIAL','{MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('RESOLVED','CLOSED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('RESOLVED','CLOSED','SYSTEM',null),
    ('RESOLVED','AWAITING_RESIDENT_CONFIRMATION','REPORTER',null),
    ('RESOLVED','AWAITING_RESIDENT_CONFIRMATION','OFFICIAL','{MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('NEW','ESCALATED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('AWAITING_VERIFICATION','ESCALATED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('VERIFIED','ESCALATED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('ASSIGNED','ESCALATED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('ASSIGNED','ESCALATED','SYSTEM',null),
    ('IN_PROGRESS','ESCALATED','OFFICIAL','{MJUMBE,MWENYEKITI,MTAA_OFFICER,WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('IN_PROGRESS','ESCALATED','SYSTEM',null),
    ('ESCALATED','ASSIGNED','OFFICIAL','{WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('ESCALATED','IN_PROGRESS','OFFICIAL','{WARD_OFFICER,MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}'),
    ('ESCALATED','REJECTED','OFFICIAL','{MUNICIPAL_OFFICER,COUNCIL_ADMIN,SYSTEM_ADMIN}')
) as t(from_status, to_status, actor_kind, allowed_roles)
on conflict (from_status, to_status, actor_kind) do nothing;
