-- Add missing FK constraints from author_id columns to profiles(id)
-- so PostgREST can resolve profile joins. All three tables reference
-- auth.users(id) in the original schema but the app joins via profiles.
--
-- Idempotent: the constraints may already exist when they were applied
-- manually through the dashboard before this migration was recorded.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'posts_author_id_fkey'
          AND conrelid = 'posts'::regclass
    ) THEN
        ALTER TABLE posts
            ADD CONSTRAINT posts_author_id_fkey
            FOREIGN KEY (author_id) REFERENCES profiles(id)
            ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'post_comments_author_id_fkey'
          AND conrelid = 'post_comments'::regclass
    ) THEN
        ALTER TABLE post_comments
            ADD CONSTRAINT post_comments_author_id_fkey
            FOREIGN KEY (author_id) REFERENCES profiles(id)
            ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'forum_threads_author_id_fkey'
          AND conrelid = 'forum_threads'::regclass
    ) THEN
        ALTER TABLE forum_threads
            ADD CONSTRAINT forum_threads_author_id_fkey
            FOREIGN KEY (author_id) REFERENCES profiles(id)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_posts_author_id_fkey ON posts(author_id);
CREATE INDEX IF NOT EXISTS idx_post_comments_author_id_fkey ON post_comments(author_id);
CREATE INDEX IF NOT EXISTS idx_forum_threads_author_id_fkey ON forum_threads(author_id);
