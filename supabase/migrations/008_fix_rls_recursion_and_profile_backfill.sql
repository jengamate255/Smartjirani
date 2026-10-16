-- SmartJirani fixes
-- 1) 42P17 infinite recursion: policies on neighborhood_members sub-queried
--    neighborhood_members itself. Route every membership check through the
--    SECURITY DEFINER helper from 005 (is_neighborhood_member) instead.
-- 2) Backfill profiles for auth users created before the on_auth_user_created
--    trigger existed (caused "Profile not found" on login).

-- ============================================================
-- HELPER: ids of neighborhoods the caller belongs to
-- ============================================================
CREATE OR REPLACE FUNCTION my_neighborhood_ids()
RETURNS SETOF UUID
LANGUAGE sql STABLE SECURITY DEFINER AS $$
    SELECT neighborhood_id FROM neighborhood_members WHERE user_id = auth.uid();
$$;

-- ============================================================
-- neighborhood_members (source of the recursion)
-- ============================================================
DROP POLICY IF EXISTS "Members can view neighborhood members" ON neighborhood_members;
CREATE POLICY "Members can view neighborhood members" ON neighborhood_members
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

-- ============================================================
-- posts
-- ============================================================
DROP POLICY IF EXISTS "Members can view neighborhood posts" ON posts;
CREATE POLICY "Members can view neighborhood posts" ON posts
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

DROP POLICY IF EXISTS "Members can create posts" ON posts;
CREATE POLICY "Members can create posts" ON posts
    FOR INSERT WITH CHECK (
        auth.uid() = author_id AND is_neighborhood_member(neighborhood_id)
    );

-- ============================================================
-- post_comments / post_likes (used posts JOIN neighborhood_members)
-- ============================================================
DROP POLICY IF EXISTS "Members can view comments" ON post_comments;
CREATE POLICY "Members can view comments" ON post_comments
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = post_id AND is_neighborhood_member(p.neighborhood_id)
        )
    );

DROP POLICY IF EXISTS "Members can view likes" ON post_likes;
CREATE POLICY "Members can view likes" ON post_likes
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM posts p
            WHERE p.id = post_id AND is_neighborhood_member(p.neighborhood_id)
        )
    );

-- ============================================================
-- voice_messages / conferences / conference_participants
-- ============================================================
DROP POLICY IF EXISTS "Users can view own voice messages" ON voice_messages;
CREATE POLICY "Users can view own voice messages" ON voice_messages
    FOR SELECT USING (
        auth.uid() = sender_id OR auth.uid() = receiver_id OR
        (neighborhood_id IS NOT NULL AND is_neighborhood_member(neighborhood_id))
    );

DROP POLICY IF EXISTS "Members can view neighborhood conferences" ON conferences;
CREATE POLICY "Members can view neighborhood conferences" ON conferences
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

DROP POLICY IF EXISTS "Members can view conference participants" ON conference_participants;
CREATE POLICY "Members can view conference participants" ON conference_participants
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM conferences c
            WHERE c.id = conference_id AND is_neighborhood_member(c.neighborhood_id)
        )
    );

-- ============================================================
-- PROFILE BACKFILL for users created before the signup trigger
-- ============================================================
INSERT INTO public.profiles (id, email, display_name, created_at, updated_at)
SELECT
    u.id,
    u.email,
    COALESCE(u.raw_user_meta_data->>'display_name', split_part(u.email, '@', 1)),
    NOW(),
    NOW()
FROM auth.users u
WHERE u.email IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = u.id)
ON CONFLICT (id) DO NOTHING;
