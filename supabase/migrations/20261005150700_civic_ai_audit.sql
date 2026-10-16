-- civic_ai_audit: immutable log of all AI operations on civic reports
-- Provides audit trail for AI triage, summaries, and any future AI operations

create table if not exists civic_ai_audit (
    id uuid primary key default gen_random_uuid(),
    report_id uuid not null references civic_reports(id) on delete cascade,
    operation text not null check (operation in ('TRIAGE', 'SUMMARY', 'CLASSIFICATION', 'TRANSLATION')),
    provider text not null,
    model text,
    input_text text,
    output_text text,
    confidence numeric(3,2) check (confidence >= 0 and confidence <= 1),
    latency_ms integer,
    error_message text,
    created_at timestamptz not null default now(),
    created_by uuid references auth.users(id) on delete set null
);

-- Index for querying audit trail by report
create index if not exists idx_civic_ai_audit_report_id on civic_ai_audit(report_id);

-- Index for querying by operation type
create index if not exists idx_civic_ai_audit_operation on civic_ai_audit(operation);

-- Index for time-based queries
create index if not exists idx_civic_ai_audit_created_at on civic_ai_audit(created_at desc);

-- Enable RLS
alter table civic_ai_audit enable row level security;

-- Users can read audit entries for reports they have access to
create policy "Users can read civic_ai_audit for accessible reports"
on civic_ai_audit for select
to authenticated
using (
    exists (
        select 1 from civic_reports cr
        where cr.id = civic_ai_audit.report_id
        and (
            cr.reporter_id = auth.uid()
            or civic_has_report_scope(auth.uid(), cr.organization_id, cr.unit_id, cr.assigned_unit_id)
        )
    )
);

-- Service role can insert (for server-side triggers and edge functions)
create policy "Service role can insert civic_ai_audit"
on civic_ai_audit for insert
to service_role
with check (true);

-- Authenticated users can insert (client-side Kotlin code will call this)
create policy "Authenticated can insert civic_ai_audit"
on civic_ai_audit for insert
to authenticated
with check (true);

-- Function to record AI triage operation
create or replace function civic_record_ai_audit(
    p_report_id uuid,
    p_operation text,
    p_provider text,
    p_model text default null,
    p_input_text text default null,
    p_output_text text default null,
    p_confidence numeric default null,
    p_latency_ms integer default null,
    p_error_message text default null
) returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_audit_id uuid;
begin
    insert into civic_ai_audit (
        report_id,
        operation,
        provider,
        model,
        input_text,
        output_text,
        confidence,
        latency_ms,
        error_message,
        created_by
    ) values (
        p_report_id,
        p_operation,
        p_provider,
        p_model,
        p_input_text,
        p_output_text,
        p_confidence,
        p_latency_ms,
        p_error_message,
        auth.uid()
    )
    returning id into v_audit_id;

    return v_audit_id;
end;
$$;

-- Grant execute to authenticated users (client-side Kotlin code will call this)
grant execute on function civic_record_ai_audit to authenticated;

-- Grant execute to service_role (for server-side triggers)
grant execute on function civic_record_ai_audit to service_role;
