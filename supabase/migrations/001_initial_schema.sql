-- SmartJirani Database Schema
-- Run this in Supabase SQL Editor: https://supabase.com/dashboard/project/hkpghinqousjboyftjcm/sql/new

-- Enable UUID extension (optional: defaults use gen_random_uuid(), so a failure here is fine)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'uuid-ossp not created (%); gen_random_uuid() will be used', SQLERRM;
END $$;

-- ============================================================
-- 1. PROFILES
-- ============================================================
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL DEFAULT '',
    username TEXT NOT NULL DEFAULT '' UNIQUE,
    phone_number TEXT,
    email TEXT,
    avatar_url TEXT,
    bio TEXT,
    neighborhood_id UUID,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profiles_username ON profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_neighborhood_id ON profiles(neighborhood_id);

-- ============================================================
-- 2. NEIGHBORHOODS
-- ============================================================
CREATE TABLE IF NOT EXISTS neighborhoods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    city TEXT NOT NULL,
    region TEXT NOT NULL,
    country TEXT NOT NULL DEFAULT 'Tanzania',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius DOUBLE PRECISION NOT NULL DEFAULT 5000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_neighborhoods_country ON neighborhoods(country);
CREATE INDEX IF NOT EXISTS idx_neighborhoods_city ON neighborhoods(city);
CREATE INDEX IF NOT EXISTS idx_neighborhoods_location ON neighborhoods(latitude, longitude);

-- Add foreign key from profiles to neighborhoods
ALTER TABLE profiles
    ADD CONSTRAINT fk_profiles_neighborhood
    FOREIGN KEY (neighborhood_id) REFERENCES neighborhoods(id)
    ON DELETE SET NULL;

-- ============================================================
-- 3. NEIGHBORHOOD MEMBERS
-- ============================================================
CREATE TABLE IF NOT EXISTS neighborhood_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'moderator', 'member')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, neighborhood_id)
);

CREATE INDEX IF NOT EXISTS idx_neighborhood_members_user ON neighborhood_members(user_id);
CREATE INDEX IF NOT EXISTS idx_neighborhood_members_neighborhood ON neighborhood_members(neighborhood_id);

-- ============================================================
-- 4. POSTS
-- ============================================================
CREATE TYPE post_type AS ENUM (
    'GENERAL', 'ANNOUNCEMENT', 'ALERT', 'LOST_FOUND',
    'BUY_SELL', 'SERVICE', 'EVENT'
);

CREATE TABLE IF NOT EXISTS posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    image_url TEXT,
    post_type post_type NOT NULL DEFAULT 'GENERAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_posts_neighborhood_id ON posts(neighborhood_id);
CREATE INDEX IF NOT EXISTS idx_posts_author_id ON posts(author_id);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_post_type ON posts(post_type);
CREATE INDEX IF NOT EXISTS idx_posts_neighborhood_created ON posts(neighborhood_id, created_at DESC);

-- ============================================================
-- 5. POST COMMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS post_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_post_comments_post_id ON post_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_post_comments_author_id ON post_comments(author_id);
CREATE INDEX IF NOT EXISTS idx_post_comments_created_at ON post_comments(created_at);

-- ============================================================
-- 6. POST LIKES (with unique constraint to prevent duplicates)
-- ============================================================
CREATE TABLE IF NOT EXISTS post_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_post_likes_user_id ON post_likes(user_id);

-- ============================================================
-- 7. NOTIFICATIONS
-- ============================================================
CREATE TYPE notification_type AS ENUM (
    'NEW_COMMENT', 'NEW_LIKE', 'ANNOUNCEMENT', 'INCOMING_CALL',
    'MISSED_CALL', 'CONFERENCE_REMINDER', 'CONFERENCE_STARTED',
    'COMMUNITY_ALERT', 'GENERAL'
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type notification_type NOT NULL DEFAULT 'GENERAL',
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);

-- ============================================================
-- 8. VOICE MESSAGES
-- ============================================================
CREATE TABLE IF NOT EXISTS voice_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    receiver_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    neighborhood_id UUID REFERENCES neighborhoods(id) ON DELETE CASCADE,
    storage_path TEXT NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_voice_messages_sender ON voice_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_voice_messages_receiver ON voice_messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_voice_messages_neighborhood ON voice_messages(neighborhood_id);
CREATE INDEX IF NOT EXISTS idx_voice_messages_created_at ON voice_messages(created_at DESC);

-- ============================================================
-- 9. CALLS
-- ============================================================
CREATE TYPE call_type AS ENUM ('ONE_TO_ONE', 'GROUP');
CREATE TYPE call_status AS ENUM ('RINGING', 'ACTIVE', 'ENDED', 'MISSED', 'DECLINED');

CREATE TABLE IF NOT EXISTS calls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caller_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    call_type call_type NOT NULL DEFAULT 'ONE_TO_ONE',
    status call_status NOT NULL DEFAULT 'RINGING',
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_calls_caller_id ON calls(caller_id);
CREATE INDEX IF NOT EXISTS idx_calls_status ON calls(status);
CREATE INDEX IF NOT EXISTS idx_calls_created_at ON calls(created_at DESC);

-- ============================================================
-- 10. CALL PARTICIPANTS
-- ============================================================
CREATE TABLE IF NOT EXISTS call_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_id UUID NOT NULL REFERENCES calls(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at TIMESTAMPTZ,
    UNIQUE(call_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_call_participants_call_id ON call_participants(call_id);
CREATE INDEX IF NOT EXISTS idx_call_participants_user_id ON call_participants(user_id);

-- ============================================================
-- 11. CONFERENCES
-- ============================================================
CREATE TYPE conference_status AS ENUM ('SCHEDULED', 'WAITING', 'ACTIVE', 'ENDED');
CREATE TYPE conference_participant_role AS ENUM ('HOST', 'MODERATOR', 'PARTICIPANT');

CREATE TABLE IF NOT EXISTS conferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    neighborhood_id UUID NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    status conference_status NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conferences_host_id ON conferences(host_id);
CREATE INDEX IF NOT EXISTS idx_conferences_neighborhood_id ON conferences(neighborhood_id);
CREATE INDEX IF NOT EXISTS idx_conferences_status ON conferences(status);
CREATE INDEX IF NOT EXISTS idx_conferences_scheduled_at ON conferences(scheduled_at);

-- ============================================================
-- 12. CONFERENCE PARTICIPANTS
-- ============================================================
CREATE TABLE IF NOT EXISTS conference_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conference_id UUID NOT NULL REFERENCES conferences(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role conference_participant_role NOT NULL DEFAULT 'PARTICIPANT',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at TIMESTAMPTZ,
    UNIQUE(conference_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_conference_participants_conference ON conference_participants(conference_id);
CREATE INDEX IF NOT EXISTS idx_conference_participants_user ON conference_participants(user_id);

-- ============================================================
-- HELPER FUNCTIONS
-- ============================================================

-- Function to get post likes count
CREATE OR REPLACE FUNCTION get_post_likes_count(post_uuid UUID)
RETURNS BIGINT AS $$
    SELECT COUNT(*)::BIGINT FROM post_likes WHERE post_id = post_uuid;
$$ LANGUAGE sql STABLE;

-- Function to get post comments count
CREATE OR REPLACE FUNCTION get_post_comments_count(post_uuid UUID)
RETURNS BIGINT AS $$
    SELECT COUNT(*)::BIGINT FROM post_comments WHERE post_id = post_uuid;
$$ LANGUAGE sql STABLE;

-- Function to check if user liked a post
CREATE OR REPLACE FUNCTION has_user_liked_post(post_uuid UUID, user_uuid UUID)
RETURNS BOOLEAN AS $$
    SELECT EXISTS(SELECT 1 FROM post_likes WHERE post_id = post_uuid AND user_id = user_uuid);
$$ LANGUAGE sql STABLE;

-- Function to like a post (prevents duplicates via unique constraint)
CREATE OR REPLACE FUNCTION like_post(post_id_param UUID)
RETURNS VOID AS $$
BEGIN
    INSERT INTO post_likes (post_id, user_id)
    VALUES (post_id_param, auth.uid())
    ON CONFLICT (post_id, user_id) DO NOTHING;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to unlike a post
CREATE OR REPLACE FUNCTION unlike_post(post_id_param UUID)
RETURNS VOID AS $$
BEGIN
    DELETE FROM post_likes
    WHERE post_id = post_id_param AND user_id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to auto-create profile on signup
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, display_name, created_at, updated_at)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'display_name', split_part(NEW.email, '@', 1)),
        NOW(),
        NOW()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to auto-create profile
CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION handle_new_user();

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at triggers
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_posts_updated_at BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_post_comments_updated_at BEFORE UPDATE ON post_comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_neighborhoods_updated_at BEFORE UPDATE ON neighborhoods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
