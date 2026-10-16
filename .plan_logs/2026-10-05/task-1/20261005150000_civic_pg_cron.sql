-- Civic Production Support: pg_cron extension + scheduled SLA scan.
-- The civic state machine already exposes civic_run_sla_scan() to service_role.
-- This migration schedules it to run every 5 minutes via pg_cron. No schema changes.

create extension if not exists pg_cron;

-- pg_cron's default schema is `cron` and its schedule() function is variadic.
-- The job is idempotent — pg_cron errors if a job with the same name exists, so
-- the cleanup step below guards re-application after a prior partial apply.

do $$
begin
  if exists (select 1 from cron.job where jobname = 'civic_run_sla_scan') then
    perform cron.unschedule('civic_run_sla_scan');
  end if;
end
$$;

select cron.schedule(
  'civic_run_sla_scan',
  '*/5 * * * *',
  $$ select public.civic_run_sla_scan(); $$
);