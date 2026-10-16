-- Phase 1.3: Weekly AI insights for officials
-- Date: 2026-10-06
-- Purpose: Store AI-generated weekly community summaries for officials
-- NOTE: FK to civic_organizations removed (table not yet on live DB)

create table if not exists civic_weekly_insights (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),

  -- Time period covered
  week_start date not null, -- Monday of the week
  week_end date not null, -- Sunday of the week

  -- Scope
  organization_id uuid not null,

  -- AI-generated content (Swahili markdown)
  summary_text text not null, -- Main weekly summary
  top_concerns jsonb not null default '[]'::jsonb, -- [{category, count, trend, sample_reports}]
  emerging_issues jsonb not null default '[]'::jsonb, -- [{description, severity, locations}]
  hotspots jsonb not null default '[]'::jsonb, -- [{geohash, label, issue_count, top_category}]

  -- Statistics
  total_reports int not null default 0,
  resolved_reports int not null default 0,
  avg_resolution_hours numeric(6,2),
  resolution_rate numeric(4,3), -- 0.0 to 1.0

  -- AI metadata
  provider text not null, -- 'groq', 'gemini'
  model text not null,
  latency_ms int,
  input_tokens int,
  output_tokens int,

  -- Delivery tracking
  delivered_to jsonb default '[]'::jsonb, -- [{user_id, delivered_at, channel}]

  -- Unique constraint: one insight per org per week
  constraint uq_civic_weekly_insights_org_week
    unique (organization_id, week_start)
);

-- Indexes
create index if not exists idx_civic_weekly_insights_org
  on civic_weekly_insights (organization_id, week_start desc);

create index if not exists idx_civic_weekly_insights_week
  on civic_weekly_insights (week_start desc);

-- RLS
alter table civic_weekly_insights enable row level security;

create policy "service_role full access weekly insights"
  on civic_weekly_insights
  for all
  to service_role
  using (true)
  with check (true);

create policy "authenticated can read weekly insights"
  on civic_weekly_insights
  for select
  to authenticated
  using (true);

-- Function to generate weekly insights (called by pg_cron)
-- This is a placeholder - the actual AI generation happens in the edge function
create or replace function civic_generate_weekly_insights(
  p_organization_id uuid,
  p_week_start date
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_week_end date := p_week_start + interval '6 days';
  v_insight_id uuid;
begin
  -- Check if already exists
  select id into v_insight_id
  from civic_weekly_insights
  where organization_id = p_organization_id
    and week_start = p_week_start;

  if v_insight_id is not null then
    return v_insight_id; -- Already generated
  end if;

  -- Insert placeholder (edge function will update with AI content)
  insert into civic_weekly_insights (
    week_start, week_end, organization_id,
    summary_text, top_concerns, emerging_issues, hotspots,
    total_reports, resolved_reports,
    provider, model
  )
  values (
    p_week_start, v_week_end, p_organization_id,
    'IN_PROGRESS', '[]'::jsonb, '[]'::jsonb, '[]'::jsonb,
    0, 0,
    'pending', 'pending'
  )
  returning id into v_insight_id;

  return v_insight_id;
end;
$$;

grant execute on function civic_generate_weekly_insights to service_role;

-- Function to update weekly insights with AI-generated content
create or replace function civic_update_weekly_insights(
  p_insight_id uuid,
  p_summary_text text,
  p_top_concerns jsonb,
  p_emerging_issues jsonb,
  p_hotspots jsonb,
  p_total_reports int,
  p_resolved_reports int,
  p_avg_resolution_hours numeric,
  p_resolution_rate numeric,
  p_provider text,
  p_model text,
  p_latency_ms int,
  p_input_tokens int,
  p_output_tokens int
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update civic_weekly_insights
  set
    summary_text = p_summary_text,
    top_concerns = p_top_concerns,
    emerging_issues = p_emerging_issues,
    hotspots = p_hotspots,
    total_reports = p_total_reports,
    resolved_reports = p_resolved_reports,
    avg_resolution_hours = p_avg_resolution_hours,
    resolution_rate = p_resolution_rate,
    provider = p_provider,
    model = p_model,
    latency_ms = p_latency_ms,
    input_tokens = p_input_tokens,
    output_tokens = p_output_tokens
  where id = p_insight_id;
end;
$$;

grant execute on function civic_update_weekly_insights to service_role;
