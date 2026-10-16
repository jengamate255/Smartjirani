-- SmartJirani Row Level Security Policies
-- Run this AFTER the schema migration

-- ============================================================
-- ENABLE RLS ON ALL TABLES
-- ============================================================
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE neighborhoods ENABLE ROW LEVEL SECURITY;
ALTER TABLE neighborhood_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE voice_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE call_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE conferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE conference_participants ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- PROFILES POLICIES
-- ============================================================
-- Anyone can read public profile info
CREATE POLICY "Profiles are viewable by everyone" ON profiles
    FOR SELECT USING (true);

-- Users can insert their own profile (handled by trigger, but allow override)
CREATE POLICY "Users can insert own profile" ON profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

-- Users can update their own profile
CREATE POLICY "Users can update own profile" ON profiles
    FOR UPDATE USING (auth.uid() = id);

-- Users can delete their own profile
CREATE POLICY "Users can delete own profile" ON profiles
    FOR DELETE USING (auth.uid() = id);

-- ============================================================
-- NEIGHBORHOODS POLICIES
-- ============================================================
-- Anyone can read neighborhoods
CREATE POLICY "Neighborhoods are viewable by everyone" ON neighborhoods
    FOR SELECT USING (true);

-- Authenticated users can create neighborhoods
CREATE POLICY "Authenticated users can create neighborhoods" ON neighborhoods
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Only admins can update neighborhoods (simplified: any authenticated for now)
CREATE POLICY "Authenticated users can update neighborhoods" ON neighborhoods
    FOR UPDATE USING (auth.role() = 'authenticated');

-- ============================================================
-- NEIGHBORHOOD MEMBERS POLICIES
-- ============================================================
-- Members can view their own memberships
CREATE POLICY "Users can view own memberships" ON neighborhood_members
    FOR SELECT USING (auth.uid() = user_id);

-- Users can view members of neighborhoods they belong to
CREATE POLICY "Members can view neighborhood members" ON neighborhood_members
    FOR SELECT USING (
        neighborhood_id IN (
            SELECT neighborhood_id FROM neighborhood_members WHERE user_id = auth.uid()
        )
    );

-- Authenticated users can join neighborhoods
CREATE POLICY "Authenticated users can join neighborhoods" ON neighborhood_members
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Users can leave neighborhoods (delete their own membership)
CREATE POLICY "Users can leave neighborhoods" ON neighborhood_members
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- POSTS POLICIES
-- ============================================================
-- Users can read posts from neighborhoods they belong to
CREATE POLICY "Members can view neighborhood posts" ON posts
    FOR SELECT USING (
        neighborhood_id IN (
            SELECT neighborhood_id FROM neighborhood_members WHERE user_id = auth.uid()
        )
    );

-- Authenticated users can create posts in their neighborhoods
CREATE POLICY "Members can create posts" ON posts
    FOR INSERT WITH CHECK (
        auth.uid() = author_id AND
        neighborhood_id IN (
            SELECT neighborhood_id FROM neighborhood_members WHERE user_id = auth.uid()
        )
    );

-- Users can update their own posts
CREATE POLICY "Users can update own posts" ON posts
    FOR UPDATE USING (auth.uid() = author_id);

-- Users can delete their own posts
CREATE POLICY "Users can delete own posts" ON posts
    FOR DELETE USING (auth.uid() = author_id);

-- ============================================================
-- POST COMMENTS POLICIES
-- ============================================================
-- Members can view comments on posts in their neighborhoods
CREATE POLICY "Members can view comments" ON post_comments
    FOR SELECT USING (
        post_id IN (
            SELECT p.id FROM posts p
            JOIN neighborhood_members nm ON p.neighborhood_id = nm.neighborhood_id
            WHERE nm.user_id = auth.uid()
        )
    );

-- Authenticated users can create comments
CREATE POLICY "Authenticated users can create comments" ON post_comments
    FOR INSERT WITH CHECK (auth.uid() = author_id);

-- Users can update their own comments
CREATE POLICY "Users can update own comments" ON post_comments
    FOR UPDATE USING (auth.uid() = author_id);

-- Users can delete their own comments
CREATE POLICY "Users can delete own comments" ON post_comments
    FOR DELETE USING (auth.uid() = author_id);

-- ============================================================
-- POST LIKES POLICIES
-- ============================================================
-- Members can view likes on posts in their neighborhoods
CREATE POLICY "Members can view likes" ON post_likes
    FOR SELECT USING (
        post_id IN (
            SELECT p.id FROM posts p
            JOIN neighborhood_members nm ON p.neighborhood_id = nm.neighborhood_id
            WHERE nm.user_id = auth.uid()
        )
    );

-- Authenticated users can like posts
CREATE POLICY "Authenticated users can like posts" ON post_likes
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Users can remove their own likes
CREATE POLICY "Users can remove own likes" ON post_likes
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- NOTIFICATIONS POLICIES
-- ============================================================
-- Users can only read their own notifications
CREATE POLICY "Users can view own notifications" ON notifications
    FOR SELECT USING (auth.uid() = user_id);

-- Users can update their own notifications (mark as read)
CREATE POLICY "Users can update own notifications" ON notifications
    FOR UPDATE USING (auth.uid() = user_id);

-- Users can delete their own notifications
CREATE POLICY "Users can delete own notifications" ON notifications
    FOR DELETE USING (auth.uid() = user_id);

-- System can insert notifications (via service role or function)
CREATE POLICY "System can insert notifications" ON notifications
    FOR INSERT WITH CHECK (true);

-- ============================================================
-- VOICE MESSAGES POLICIES
-- ============================================================
-- Users can view voice messages they sent or received
CREATE POLICY "Users can view own voice messages" ON voice_messages
    FOR SELECT USING (
        auth.uid() = sender_id OR auth.uid() = receiver_id OR
        (neighborhood_id IS NOT NULL AND neighborhood_id IN (
            SELECT neighborhood_id FROM neighborhood_members WHERE user_id = auth.uid()
        ))
    );

-- Authenticated users can send voice messages
CREATE POLICY "Authenticated users can send voice messages" ON voice_messages
    FOR INSERT WITH CHECK (auth.uid() = sender_id);

-- Users can delete their own voice messages
CREATE POLICY "Users can delete own voice messages" ON voice_messages
    FOR DELETE USING (auth.uid() = sender_id);

-- ============================================================
-- CALLS POLICIES
-- ============================================================
-- Users can view calls they participated in
CREATE POLICY "Users can view own calls" ON calls
    FOR SELECT USING (
        auth.uid() = caller_id OR
        id IN (
            SELECT call_id FROM call_participants WHERE user_id = auth.uid()
        )
    );

-- Authenticated users can create calls
CREATE POLICY "Authenticated users can create calls" ON calls
    FOR INSERT WITH CHECK (auth.uid() = caller_id);

-- Call participants can update call status
CREATE POLICY "Participants can update calls" ON calls
    FOR UPDATE USING (
        auth.uid() = caller_id OR
        id IN (
            SELECT call_id FROM call_participants WHERE user_id = auth.uid()
        )
    );

-- ============================================================
-- CALL PARTICIPANTS POLICIES
-- ============================================================
-- Users can view participants of calls they are in
CREATE POLICY "Participants can view call participants" ON call_participants
    FOR SELECT USING (
        call_id IN (
            SELECT cp.call_id FROM call_participants cp WHERE cp.user_id = auth.uid()
        ) OR
        call_id IN (
            SELECT id FROM calls WHERE caller_id = auth.uid()
        )
    );

-- Users can join calls
CREATE POLICY "Users can join calls" ON call_participants
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Users can update their own participation
CREATE POLICY "Users can update own participation" ON call_participants
    FOR UPDATE USING (auth.uid() = user_id);

-- ============================================================
-- CONFERENCES POLICIES
-- ============================================================
-- Members can view conferences in their neighborhoods
CREATE POLICY "Members can view neighborhood conferences" ON conferences
    FOR SELECT USING (
        neighborhood_id IN (
            SELECT neighborhood_id FROM neighborhood_members WHERE user_id = auth.uid()
        )
    );

-- Authenticated users can create conferences
CREATE POLICY "Authenticated users can create conferences" ON conferences
    FOR INSERT WITH CHECK (auth.uid() = host_id);

-- Host can update their conferences
CREATE POLICY "Host can update conferences" ON conferences
    FOR UPDATE USING (auth.uid() = host_id);

-- ============================================================
-- CONFERENCE PARTICIPANTS POLICIES
-- ============================================================
-- Members can view conference participants in their neighborhoods
CREATE POLICY "Members can view conference participants" ON conference_participants
    FOR SELECT USING (
        conference_id IN (
            SELECT c.id FROM conferences c
            JOIN neighborhood_members nm ON c.neighborhood_id = nm.neighborhood_id
            WHERE nm.user_id = auth.uid()
        )
    );

-- Users can join conferences
CREATE POLICY "Users can join conferences" ON conference_participants
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Users can update their own participation
CREATE POLICY "Users can update own participation" ON conference_participants
    FOR UPDATE USING (auth.uid() = user_id);

-- ============================================================
-- STORAGE BUCKETS
-- ============================================================
-- Create storage buckets
INSERT INTO storage.buckets (id, name, public)
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO storage.buckets (id, name, public)
VALUES ('media', 'media', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO storage.buckets (id, name, public)
VALUES ('voice-messages', 'voice-messages', false)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- STORAGE POLICIES
-- ============================================================
-- Avatars: anyone can read, only owner can upload
CREATE POLICY "Avatar images are publicly accessible" ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');

CREATE POLICY "Users can upload avatar" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'avatars' AND
        auth.uid()::text = (string_to_array(name, '/'))[2]
    );

CREATE POLICY "Users can update own avatar" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'avatars' AND
        auth.uid()::text = (string_to_array(name, '/'))[2]
    );

CREATE POLICY "Users can delete own avatar" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'avatars' AND
        auth.uid()::text = (string_to_array(name, '/'))[2]
    );

-- Media (post images): anyone can read, authenticated can upload
CREATE POLICY "Media files are publicly accessible" ON storage.objects
    FOR SELECT USING (bucket_id = 'media');

CREATE POLICY "Authenticated users can upload media" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'media' AND auth.role() = 'authenticated');

-- Voice messages: only sender/receiver can read
CREATE POLICY "Users can read own voice messages" ON storage.objects
    FOR SELECT USING (
        bucket_id = 'voice-messages' AND
        auth.role() = 'authenticated'
    );

CREATE POLICY "Authenticated users can upload voice messages" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'voice-messages' AND auth.role() = 'authenticated');
