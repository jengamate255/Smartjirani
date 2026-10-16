-- ============================================================
-- C8: Government analytics from real civic records.
--
-- civic_analytics() is SECURITY INVOKER: every aggregate is computed over
-- exactly the civic_reports rows the caller's RLS policies expose, so an
-- official sees their organization's numbers and a resident sees only
-- their own reports. No org id parameter, no privilege bypass.
-- ============================================================

create or replace function public.civic_analytics()
returns jsonb
language sql
stable
security invoker
set search_path = public
as $$
    with visible as (
        select r.*
        from public.civic_reports r
        where r.canonical_report_id is null
    ),
    agg as (
        select
            count(*) as total,
            count(*) filter (
                where status not in ('RESOLVED','CLOSED','REJECTED')) as active,
            count(*) filter (
                where due_at is not null and due_at < now()
                and status not in ('RESOLVED','CLOSED','REJECTED')) as overdue,
            count(*) filter (where escalation_level > 0) as escalated,
            coalesce(sum(greatest(report_count - 1, 0)), 0) as duplicates_folded,
            avg(
                case when resolved_at is not null
                    then extract(epoch from (resolved_at - created_at)) / 3600.0 end
            ) as avg_resolution_hours,
            count(*) filter (
                where status in ('RESOLVED','CLOSED')
                and resolved_at is not null and due_at is not null
                and resolved_at <= due_at) as sla_met,
            count(*) filter (
                where status in ('RESOLVED','CLOSED')
                and (resolved_at is null or due_at is null or resolved_at > due_at)) as sla_missed
        from visible
    ),
    by_status as (
        select jsonb_agg(jsonb_build_object('status', status, 'count', cnt) order by cnt desc)
        from (select status, count(*) as cnt from visible group by status) s
    ),
    by_severity as (
        select jsonb_agg(jsonb_build_object('severity', severity, 'count', cnt) order by cnt desc)
        from (select severity, count(*) as cnt from visible group by severity) s
    ),
    by_category as (
        select jsonb_agg(jsonb_build_object(
            'category_id', c.id,
            'category', c.name_en,
            'category_sw', c.name_sw,
            'count', t.cnt
        ) order by t.cnt desc)
        from (
            select r.category_id, count(*) as cnt
            from visible r
            group by r.category_id
        ) t
        join public.civic_categories c on c.id = t.category_id
    ),
    trend as (
        select jsonb_agg(jsonb_build_object(
            'day', to_char(d::date, 'YYYY-MM-DD'),
            'count', coalesce(t.cnt, 0)
        ) order by d)
        from generate_series(
            (current_date - interval '13 days')::date,
            current_date::date,
            interval '1 day'
        ) d
        left join (
            select (created_at at time zone 'UTC')::date as day0, count(*) as cnt
            from visible
            where created_at >= current_date - interval '13 days'
            group by 1
        ) t on t.day0 = d::date
    )
    select jsonb_build_object(
        'total', agg.total,
        'active', agg.active,
        'overdue', agg.overdue,
        'escalated', agg.escalated,
        'duplicates_folded', agg.duplicates_folded,
        'avg_resolution_hours', round(agg.avg_resolution_hours::numeric, 1),
        'sla_met', agg.sla_met,
        'sla_missed', agg.sla_missed,
        'by_status', coalesce(by_status.jsonb_agg, '[]'::jsonb),
        'by_severity', coalesce(by_severity.jsonb_agg, '[]'::jsonb),
        'by_category', coalesce(by_category.jsonb_agg, '[]'::jsonb),
        'trend_14d', coalesce(trend.jsonb_agg, '[]'::jsonb)
    )
    from agg, by_status, by_severity, by_category, trend
$$;

revoke execute on function public.civic_analytics() from anon, public;
grant execute on function public.civic_analytics() to authenticated, service_role;
