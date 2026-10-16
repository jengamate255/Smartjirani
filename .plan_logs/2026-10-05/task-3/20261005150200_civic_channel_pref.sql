-- Civic Production Support: per-resident channel preference. Backfilled to
-- 'in_app' for existing rows so the column is never NULL.

alter table public.profiles
    add column if not exists civic_channel_pref text;

update public.profiles
    set civic_channel_pref = 'in_app'
    where civic_channel_pref is null;

alter table public.profiles
    alter column civic_channel_pref set default 'in_app';

alter table public.profiles
    alter column civic_channel_pref set not null;

alter table public.profiles
    drop constraint if exists profiles_civic_channel_pref_check;
alter table public.profiles
    add constraint profiles_civic_channel_pref_check
    check (civic_channel_pref in ('in_app','sms','whatsapp','sms,whatsapp'));