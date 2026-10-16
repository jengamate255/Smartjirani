-- Civic C4: categories for the resident report flow, scoped to the caller's
-- organization (resolved from their profile neighborhood, mirroring
-- submit_civic_report). SECURITY DEFINER because org_units visibility is
-- official-scoped under RLS; the function only leaks the caller's own org
-- category catalog and returns nothing for anonymous callers.

create or replace function public.civic_list_categories()
returns setof public.civic_categories
language sql
stable
security definer
set search_path = public
as $$
    select c.*
    from public.civic_categories c
    where c.organization_id = (
        select u.organization_id
        from public.org_units u
        where u.neighborhood_id = (
            select pr.neighborhood_id from public.profiles pr where pr.id = auth.uid()
        )
        order by u.created_at
        limit 1
    )
      and c.is_active
    order by c.sort_order
$$;
