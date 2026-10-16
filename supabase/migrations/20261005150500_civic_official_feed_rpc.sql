create or replace function public.civic_official_feed(
    p_filter text,
    p_scope_unit uuid default null
)
returns table (
    report_id uuid,
    ref text,
    title text,
    category_sw text,
    category_en text,
    severity text,
    status text,
    due_at_minutes int,
    assigned_unit_name text,
    created_at timestamptz,
    neighborhood_name text
)
language sql stable security invoker set search_path = public as $$
    select
        r.id,
        r.public_reference,
        r.title,
        c.name_sw,
        c.name_en,
        r.severity::text,
        r.status::text,
        (extract(epoch from (r.due_at - now())) / 60)::int as due_at_minutes,
        coalesce(au.name, '')::text,
        r.created_at,
        coalesce(n.name, '')::text
    from public.civic_reports r
    join public.civic_categories c on c.id = r.category_id
    left join public.org_units au on au.id = r.assigned_unit_id
    left join public.neighborhoods n on n.id = au.neighborhood_id
    where public.civic_has_report_scope(auth.uid(), r.organization_id, r.unit_id, r.assigned_unit_id)
      and (
          p_scope_unit is null
          or r.assigned_unit_id = p_scope_unit
          or r.unit_id = p_scope_unit
      )
      and case p_filter
            when 'awaiting_verification' then r.status = 'AWAITING_VERIFICATION'
            when 'overdue' then r.due_at < now()
                                 and r.status in ('ASSIGNED','IN_PROGRESS','WAITING_EXTERNAL_ACTION')
            when 'mine' then exists (
                select 1 from public.user_roles ur
                where ur.user_id = auth.uid()
                  and ur.is_active
                  and ur.org_unit_id = r.assigned_unit_id
            )
            when 'new_today' then r.created_at::date = current_date
            else true
          end
    order by r.created_at desc
    limit 200;
$$;

grant execute on function public.civic_official_feed(text, uuid) to authenticated;
