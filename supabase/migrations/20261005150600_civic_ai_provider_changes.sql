-- ============================================================
-- Task 12: Add provider_changes column to ai_comms.
-- Tracks provider swap history (e.g. gemini -> mock fallback)
-- as a JSONB array of {ts, from, to, reason} entries.
-- ============================================================

alter table public.ai_comms
    add column if not exists provider_changes jsonb default '[]'::jsonb;
