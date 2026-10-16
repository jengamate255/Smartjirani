-- App release distribution: version metadata + public APK hosting bucket

create table if not exists public.app_releases (
    id uuid primary key default gen_random_uuid(),
    version_name text not null,
    version_code integer not null unique,
    apk_bucket text not null default 'apk-releases',
    apk_path text not null,
    sha256 text,
    size_bytes bigint,
    min_supported_code integer not null default 1,
    changelog jsonb not null default '[]'::jsonb,
    is_published boolean not null default false,
    released_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

alter table public.app_releases enable row level security;

create policy "Published releases are publicly readable"
    on public.app_releases for select
    using (is_published = true);

-- Writes happen via service role (edge function / admin), which bypasses RLS

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'apk-releases',
    'apk-releases',
    true,
    104857600,
    array['application/vnd.android.package-archive', 'application/octet-stream']
)
on conflict (id) do nothing;
