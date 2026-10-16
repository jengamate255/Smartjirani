-- SmartJirani community features: Mtaa Feed extras, Ujamaa Hub, Baraza, Resident Profile
-- Run AFTER 001_initial_schema.sql (and 002/003).
-- Screen copy is Sheng (Swahili/Sheng code-switching) per the project's language decision.

-- ============================================================
-- 0. SHARED ENUM TYPES
-- ============================================================
DO $$ BEGIN
    CREATE TYPE resource_category AS ENUM (
        'TOOL', 'GEAR', 'CARPOOL', 'BOOKS', 'SEEDS', 'SKILLS'
    );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE loan_status AS ENUM ('REQUESTED', 'ACTIVE', 'RETURNED', 'DECLINED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE baraza_mode AS ENUM ('HYBRID', 'PHYSICAL', 'ONLINE');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE project_status AS ENUM ('ACTIVE', 'SCHEDULED', 'CLOSED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE contribution_status AS ENUM ('PENDING', 'CONFIRMED', 'FAILED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================
-- 1. MTAA FEED — ambient street status
-- ============================================================
CREATE TABLE IF NOT EXISTS street_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    temperature_c INTEGER NOT NULL DEFAULT 24,
    power_status TEXT NOT NULL DEFAULT 'NORMAL',
    gate_status TEXT NOT NULL DEFAULT 'OPEN',
    street_note TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(neighborhood_id)
);

-- ============================================================
-- 2. MTAA FEED — Nyumba Kumi notices
-- ============================================================
CREATE TABLE IF NOT EXISTS street_notices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    eyebrow TEXT NOT NULL DEFAULT 'TAARIFA YA NYUMBA KUMI',
    body TEXT NOT NULL,
    authority TEXT NOT NULL DEFAULT '',
    status_label TEXT NOT NULL DEFAULT '',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_street_notices_neighborhood ON street_notices(neighborhood_id, created_at DESC);

-- ============================================================
-- 3. MTAA FEED — community polls
-- ============================================================
CREATE TABLE IF NOT EXISTS polls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    eyebrow TEXT NOT NULL DEFAULT 'SAUTI YA MTAA',
    question TEXT NOT NULL,
    subtext TEXT NOT NULL DEFAULT '',
    ends_label TEXT NOT NULL DEFAULT '',
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS poll_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    label TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_poll_options_poll ON poll_options(poll_id, position);

CREATE TABLE IF NOT EXISTS poll_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(poll_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_poll_votes_poll ON poll_votes(poll_id);
CREATE INDEX IF NOT EXISTS idx_poll_votes_option ON poll_votes(option_id);

-- ============================================================
-- 4. BARAZA — events / townhall assemblies
-- ============================================================
CREATE TABLE IF NOT EXISTS baraza_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    council_label TEXT NOT NULL DEFAULT '',
    committee_label TEXT NOT NULL DEFAULT '',
    district_label TEXT NOT NULL DEFAULT '',
    households_label TEXT NOT NULL DEFAULT '',
    quorum_label TEXT NOT NULL DEFAULT '',
    title TEXT NOT NULL,
    mode baraza_mode NOT NULL DEFAULT 'HYBRID',
    day_label TEXT NOT NULL DEFAULT '',
    date_label TEXT NOT NULL DEFAULT '',
    time_label TEXT NOT NULL DEFAULT '',
    venue TEXT NOT NULL DEFAULT '',
    stream_note TEXT NOT NULL DEFAULT '',
    agenda JSONB NOT NULL DEFAULT '[]'::jsonb,
    attending_count INTEGER NOT NULL DEFAULT 0,
    questions_queued INTEGER NOT NULL DEFAULT 0,
    event_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_baraza_events_neighborhood ON baraza_events(neighborhood_id, event_at);

CREATE TABLE IF NOT EXISTS baraza_rsvps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES baraza_events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, user_id)
);

-- ============================================================
-- 5. BARAZA — forum threads
-- ============================================================
CREATE TABLE IF NOT EXISTS forum_threads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    tag TEXT NOT NULL DEFAULT '',
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    solved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_forum_threads_neighborhood ON forum_threads(neighborhood_id, created_at DESC);

CREATE TABLE IF NOT EXISTS thread_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id UUID NOT NULL REFERENCES forum_threads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(thread_id, user_id)
);

CREATE TABLE IF NOT EXISTS thread_replies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id UUID NOT NULL REFERENCES forum_threads(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_thread_replies_thread ON thread_replies(thread_id, created_at);

-- ============================================================
-- 6. BARAZA — harambee projects
-- ============================================================
CREATE TABLE IF NOT EXISTS harambee_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    status project_status NOT NULL DEFAULT 'ACTIVE',
    status_label TEXT NOT NULL DEFAULT '',
    phase_label TEXT NOT NULL DEFAULT '',
    category_label TEXT NOT NULL DEFAULT '',
    title TEXT NOT NULL,
    subtitle TEXT NOT NULL DEFAULT '',
    icon_label TEXT NOT NULL DEFAULT '',
    photo_url TEXT,
    target_kes NUMERIC(12, 2) NOT NULL DEFAULT 0,
    raised_kes NUMERIC(12, 2) NOT NULL DEFAULT 0,
    progress_pct INTEGER NOT NULL DEFAULT 0,
    progress_label TEXT NOT NULL DEFAULT '',
    volunteer_slots INTEGER NOT NULL DEFAULT 0,
    volunteers_filled INTEGER NOT NULL DEFAULT 0,
    volunteer_note TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_harambee_projects_neighborhood ON harambee_projects(neighborhood_id, status);

CREATE TABLE IF NOT EXISTS project_volunteers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES harambee_projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, user_id)
);

-- ============================================================
-- 7. UJAMAA HUB — borrowable / shareable resources
-- ============================================================
CREATE TABLE IF NOT EXISTS resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category resource_category NOT NULL DEFAULT 'TOOL',
    tag_label TEXT NOT NULL DEFAULT '',
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    image_url TEXT,
    distance_label TEXT NOT NULL DEFAULT '',
    history_label TEXT NOT NULL DEFAULT '',
    availability_note TEXT NOT NULL DEFAULT '',
    deposit_note TEXT NOT NULL DEFAULT '',
    cta_label TEXT NOT NULL DEFAULT '',
    terms_label TEXT NOT NULL DEFAULT '',
    available_until TEXT NOT NULL DEFAULT '',
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    meta JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_resources_neighborhood ON resources(neighborhood_id, category);

CREATE TABLE IF NOT EXISTS resource_loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    borrower_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status loan_status NOT NULL DEFAULT 'REQUESTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(resource_id, borrower_id)
);

-- ============================================================
-- 8. UJAMAA HUB — urgent needs
-- ============================================================
CREATE TABLE IF NOT EXISTS urgent_needs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    requester_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL DEFAULT '',
    requester_label TEXT NOT NULL DEFAULT '',
    requester_meta TEXT NOT NULL DEFAULT '',
    distance_label TEXT NOT NULL DEFAULT '',
    status_label TEXT NOT NULL DEFAULT '',
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_urgent_needs_neighborhood ON urgent_needs(neighborhood_id, is_open, created_at DESC);

CREATE TABLE IF NOT EXISTS urgent_need_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    need_id UUID NOT NULL REFERENCES urgent_needs(id) ON DELETE CASCADE,
    helper_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(need_id, helper_id)
);

-- ============================================================
-- 9. UJAMAA HUB — emergency & guard pool (funded via PesaPal)
-- ============================================================
CREATE TABLE IF NOT EXISTS harambee_funds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    subtitle TEXT NOT NULL DEFAULT '',
    audit_label TEXT NOT NULL DEFAULT '',
    target_kes NUMERIC(12, 2) NOT NULL DEFAULT 0,
    raised_kes NUMERIC(12, 2) NOT NULL DEFAULT 0,
    progress_pct INTEGER NOT NULL DEFAULT 0,
    purpose_label TEXT NOT NULL DEFAULT '',
    remaining_label TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(neighborhood_id)
);

CREATE TABLE IF NOT EXISTS harambee_contributions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fund_id UUID NOT NULL REFERENCES harambee_funds(id) ON DELETE CASCADE,
    contributor_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    amount_kes NUMERIC(12, 2) NOT NULL,
    provider TEXT NOT NULL DEFAULT 'PesaPal',
    reference TEXT NOT NULL DEFAULT '',
    checkout_request_id TEXT NOT NULL DEFAULT '',
    status contribution_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_harambee_contributions_fund ON harambee_contributions(fund_id, created_at DESC);

-- ============================================================
-- 10. RESIDENT PROFILE — karma (derived, not stored)
-- ============================================================
CREATE OR REPLACE FUNCTION karma_summary(p_profile_id UUID)
RETURNS TABLE (
    score NUMERIC,
    trust_pct INTEGER,
    actions_count INTEGER,
    favors_done INTEGER,
    shared_items INTEGER,
    barazas INTEGER,
    rank_label TEXT
) LANGUAGE sql STABLE AS $$
    WITH stats AS (
        SELECT
            (SELECT COUNT(*) FROM posts WHERE author_id = p_profile_id) AS posts_made,
            (SELECT COUNT(*) FROM post_likes pl
                JOIN posts p ON p.id = pl.post_id
                WHERE p.author_id = p_profile_id) AS likes_received,
            (SELECT COUNT(*) FROM post_comments WHERE author_id = p_profile_id) AS comments_made,
            (SELECT COUNT(*) FROM post_likes WHERE user_id = p_profile_id) AS likes_given,
            (SELECT COUNT(*) FROM baraza_rsvps WHERE user_id = p_profile_id) AS rsvps,
            (SELECT COUNT(*) FROM project_volunteers WHERE user_id = p_profile_id) AS volunteers,
            (SELECT COUNT(*) FROM resources WHERE owner_id = p_profile_id) AS items_shared,
            (SELECT COUNT(*) FROM urgent_need_offers WHERE helper_id = p_profile_id) AS helps
    )
    SELECT
        ROUND(LEAST(5.0, 3.0 + (stats.likes_received * 0.02 + stats.comments_made * 0.01 + stats.helps * 0.05))::NUMERIC, 1),
        ROUND(LEAST(99, 70 + stats.likes_received + stats.helps)::INTEGER),
        (stats.posts_made + stats.comments_made + stats.likes_given + stats.rsvps + stats.volunteers + stats.helps)::INTEGER,
        (stats.helps + stats.likes_given)::INTEGER,
        stats.items_shared::INTEGER,
        (stats.rsvps + stats.volunteers)::INTEGER,
        CASE
            WHEN stats.likes_received >= 40 THEN 'Bora 5% ya Mtaa'
            WHEN stats.likes_received >= 15 THEN 'Jirani wa Kuaminika'
            ELSE 'Mjanzee wa Mtaa'
        END
    FROM stats;
$$;

-- ============================================================
-- 11. RESIDENT PROFILE — shared locker, skills, reviews
-- ============================================================
CREATE TABLE IF NOT EXISTS shared_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    neighborhood_id UUID REFERENCES neighborhoods(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    subtitle TEXT NOT NULL DEFAULT '',
    state_label TEXT NOT NULL DEFAULT 'Tayari',
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    loans_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shared_items_owner ON shared_items(owner_id);

CREATE TABLE IF NOT EXISTS skills_offered (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    neighborhood_id UUID REFERENCES neighborhoods(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    testimonial TEXT NOT NULL DEFAULT '',
    endorsement_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_skills_offered_owner ON skills_offered(owner_id);

CREATE TABLE IF NOT EXISTS profile_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    author_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    author_label TEXT NOT NULL DEFAULT '',
    body TEXT NOT NULL,
    rating INTEGER NOT NULL DEFAULT 5 CHECK (rating BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profile_reviews_subject ON profile_reviews(subject_id, created_at DESC);

-- ============================================================
-- 12. RESIDENT PROFILE — security circle, privacy, household
-- ============================================================
CREATE TABLE IF NOT EXISTS security_circle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    cluster_label TEXT NOT NULL DEFAULT '',
    designation_label TEXT NOT NULL DEFAULT '',
    sos_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    contacts JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS privacy_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    visibility_label TEXT NOT NULL DEFAULT '',
    visibility_radius_m INTEGER NOT NULL DEFAULT 500,
    house_number_visibility TEXT NOT NULL DEFAULT '',
    household_label TEXT NOT NULL DEFAULT '',
    status_label TEXT NOT NULL DEFAULT 'Imelindwa',
    show_house_number BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS household_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    member_name TEXT NOT NULL,
    relation_label TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_household_members_profile ON household_members(profile_id);

-- ============================================================
-- 13. NOTIFICATIONS — unread badge source for the header bell
-- ============================================================
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read BOOLEAN NOT NULL DEFAULT FALSE;
