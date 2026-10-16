-- Phase 1.2: AI metrics table for AI project tracker dashboard
-- Date: 2026-10-06
-- Purpose: Track AI feature usage, latency, costs, errors across all providers
-- NOTE: FK to civic_organizations removed (table not yet on live DB)

create table if not exists ai_metrics (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),

  -- What was called
  feature text not null, -- 'triage', 'assistant', 'summarize', 'analyze', 'dedup', 'transcribe', 'insights', 'route'
  action text, -- specific sub-action if applicable

  -- Provider details
  provider text not null, -- 'groq', 'gemini', 'mock', 'whisper'
  model text not null, -- e.g., 'openai/gpt-oss-120b', 'gemini-2.0-flash'
  mock boolean not null default false,

  -- Performance
  latency_ms int,
  input_tokens int,
  output_tokens int,
  total_tokens int,

  -- Cost tracking (in USD, estimated)
  cost_usd numeric(10, 6),

  -- Success/failure
  success boolean not null default true,
  error_message text,
  error_code text,

  -- Context
  user_id uuid references auth.users(id),
  organization_id uuid,
  report_id uuid references civic_reports(id),

  -- Additional metadata (JSON for flexibility)
  metadata jsonb default '{}'::jsonb
);

-- Indexes for dashboard queries
create index if not exists idx_ai_metrics_created_at
  on ai_metrics (created_at desc);

create index if not exists idx_ai_metrics_feature
  on ai_metrics (feature, created_at desc);

create index if not exists idx_ai_metrics_provider
  on ai_metrics (provider, created_at desc);

create index if not exists idx_ai_metrics_user
  on ai_metrics (user_id, created_at desc);

create index if not exists idx_ai_metrics_organization
  on ai_metrics (organization_id, created_at desc);

-- RLS: service_role full access, authenticated can read their own
alter table ai_metrics enable row level security;

create policy "service_role full access ai_metrics"
  on ai_metrics
  for all
  to service_role
  using (true)
  with check (true);

create policy "authenticated can read own ai_metrics"
  on ai_metrics
  for select
  to authenticated
  using (user_id = (select auth.uid()));

-- Function to record AI metric (called from edge functions)
create or replace function ai_record_metric(
  p_feature text,
  p_provider text,
  p_model text,
  p_mock boolean default false,
  p_latency_ms int default null,
  p_input_tokens int default null,
  p_output_tokens int default null,
  p_total_tokens int default null,
  p_cost_usd numeric default null,
  p_success boolean default true,
  p_error_message text default null,
  p_error_code text default null,
  p_user_id uuid default null,
  p_organization_id uuid default null,
  p_report_id uuid default null,
  p_metadata jsonb default '{}'::jsonb,
  p_action text default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_metric_id uuid;
begin
  insert into ai_metrics (
    feature, action, provider, model, mock,
    latency_ms, input_tokens, output_tokens, total_tokens, cost_usd,
    success, error_message, error_code,
    user_id, organization_id, report_id, metadata
  )
  values (
    p_feature, p_action, p_provider, p_model, p_mock,
    p_latency_ms, p_input_tokens, p_output_tokens, p_total_tokens, p_cost_usd,
    p_success, p_error_message, p_error_code,
    p_user_id, p_organization_id, p_report_id, p_metadata
  )
  returning id into v_metric_id;

  return v_metric_id;
end;
$$;

-- Grant execute on the recording function
grant execute on function ai_record_metric to authenticated;
grant execute on function ai_record_metric to service_role;

-- View for dashboard: daily metrics summary
create or replace view ai_metrics_daily_summary as
select
  date_trunc('day', created_at)::date as metric_date,
  feature,
  provider,
  model,
  count(*) as total_requests,
  count(*) filter (where success) as successful_requests,
  count(*) filter (where not success) as failed_requests,
  avg(latency_ms) filter (where latency_ms is not null) as avg_latency_ms,
  percentile_cont(0.95) within group (order by latency_ms) filter (where latency_ms is not null) as p95_latency_ms,
  sum(total_tokens) filter (where total_tokens is not null) as total_tokens_used,
  sum(cost_usd) filter (where cost_usd is not null) as total_cost_usd,
  count(distinct user_id) as unique_users,
  count(distinct organization_id) as unique_organizations
from ai_metrics
group by date_trunc('day', created_at), feature, provider, model
order by metric_date desc, feature, provider;

grant select on ai_metrics_daily_summary to authenticated;
