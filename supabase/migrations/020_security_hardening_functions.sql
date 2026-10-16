-- 020: Security hardening for over-privileged functions
--
-- 1. like_post / unlike_post ran as SECURITY DEFINER with no need: the
--    post_likes RLS policies ("Authenticated users can like posts" /
--    "Users can remove own likes") already permit exactly these operations
--    for auth.uid(). Recreated as SECURITY INVOKER.
--
-- 2. get_or_create_direct_conversation is SECURITY DEFINER (required: it
--    inserts a participant row for the other user, which RLS would block)
--    but accepted arbitrary user_a/user_b. Any authenticated caller could
--    fabricate a conversation between two unrelated users. Now restricted
--    to conversations the caller participates in.

CREATE OR REPLACE FUNCTION like_post(post_id_param UUID)
RETURNS VOID AS $$
BEGIN
    INSERT INTO post_likes (post_id, user_id)
    VALUES (post_id_param, auth.uid())
    ON CONFLICT (post_id, user_id) DO NOTHING;
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE OR REPLACE FUNCTION unlike_post(post_id_param UUID)
RETURNS VOID AS $$
BEGIN
    DELETE FROM post_likes
    WHERE post_id = post_id_param AND user_id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY INVOKER;

CREATE OR REPLACE FUNCTION get_or_create_direct_conversation(user_a UUID, user_b UUID)
RETURNS UUID AS $$
DECLARE
    conv_id UUID;
BEGIN
    IF auth.uid() IS NULL OR (auth.uid() <> user_a AND auth.uid() <> user_b) THEN
        RAISE EXCEPTION 'not_authorized'
            USING ERRCODE = '42501';
    END IF;

    SELECT c.id INTO conv_id
    FROM conversations c
    WHERE c.is_group = FALSE
      AND EXISTS (SELECT 1 FROM conversation_participants cp WHERE cp.conversation_id = c.id AND cp.user_id = user_a)
      AND EXISTS (SELECT 1 FROM conversation_participants cp WHERE cp.conversation_id = c.id AND cp.user_id = user_b)
      AND (SELECT COUNT(*) FROM conversation_participants cp WHERE cp.conversation_id = c.id) = 2
    LIMIT 1;

    IF conv_id IS NULL THEN
        INSERT INTO conversations (is_group, created_by)
        VALUES (FALSE, auth.uid())
        RETURNING id INTO conv_id;

        INSERT INTO conversation_participants (conversation_id, user_id)
        VALUES (conv_id, user_a), (conv_id, user_b);
    END IF;

    RETURN conv_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

REVOKE EXECUTE ON FUNCTION get_or_create_direct_conversation(UUID, UUID) FROM anon;
GRANT EXECUTE ON FUNCTION get_or_create_direct_conversation(UUID, UUID) TO authenticated;
