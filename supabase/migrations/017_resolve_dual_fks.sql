-- 017: Resolve dual foreign keys on author_id columns
--
-- Migration 012 was supposed to repoint posts.author_id / post_comments.author_id /
-- forum_threads.author_id from auth.users(id) to profiles(id), but its
-- IF NOT EXISTS guard matched the auto-generated constraint NAME from 001
-- (which already pointed to auth.users). Result: the auth.users FK survived
-- and no profiles FK exists, so PostgREST cannot resolve embedded profile joins.
--
-- This migration drops any FK on those columns referencing auth.users
-- (identified by referenced table, not name) and ensures exactly one FK
-- referencing profiles(id) exists. Idempotent.

DO $$
DECLARE
    fk RECORD;
BEGIN
    -- posts.author_id
    FOR fk IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'posts'::regclass
          AND contype = 'f'
          AND confrelid = 'auth.users'::regclass
          AND conkey = ARRAY[(SELECT attnum::smallint FROM pg_attribute
                              WHERE attrelid = 'posts'::regclass AND attname = 'author_id')]
    LOOP
        EXECUTE format('ALTER TABLE posts DROP CONSTRAINT %I', fk.conname);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'posts'::regclass
          AND contype = 'f'
          AND confrelid = 'profiles'::regclass
          AND conkey = ARRAY[(SELECT attnum::smallint FROM pg_attribute
                              WHERE attrelid = 'posts'::regclass AND attname = 'author_id')]
    ) THEN
        ALTER TABLE posts
            ADD CONSTRAINT posts_author_id_fkey
            FOREIGN KEY (author_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;

    -- post_comments.author_id
    FOR fk IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'post_comments'::regclass
          AND contype = 'f'
          AND confrelid = 'auth.users'::regclass
          AND conkey = ARRAY[(SELECT attnum::smallint FROM pg_attribute
                              WHERE attrelid = 'post_comments'::regclass AND attname = 'author_id')]
    LOOP
        EXECUTE format('ALTER TABLE post_comments DROP CONSTRAINT %I', fk.conname);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'post_comments'::regclass
          AND contype = 'f'
          AND confrelid = 'profiles'::regclass
          AND conkey = ARRAY[(SELECT attnum::smallint FROM pg_attribute
                              WHERE attrelid = 'post_comments'::regclass AND attname = 'author_id')]
    ) THEN
        ALTER TABLE post_comments
            ADD CONSTRAINT post_comments_author_id_fkey
            FOREIGN KEY (author_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;

    -- forum_threads.author_id
    FOR fk IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'forum_threads'::regclass
          AND contype = 'f'
          AND confrelid = 'auth.users'::regclass
          AND conkey = ARRAY[(SELECT attnum::smallint FROM pg_attribute
                              WHERE attrelid = 'forum_threads'::regclass AND attname = 'author_id')]
    LOOP
        EXECUTE format('ALTER TABLE forum_threads DROP CONSTRAINT %I', fk.conname);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'forum_threads'::regclass
          AND contype = 'f'
          AND confrelid = 'profiles'::regclass
          AND conkey = ARRAY[(SELECT attnum::smallint FROM pg_attribute
                              WHERE attrelid = 'forum_threads'::regclass AND attname = 'author_id')]
    ) THEN
        ALTER TABLE forum_threads
            ADD CONSTRAINT forum_threads_author_id_fkey
            FOREIGN KEY (author_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;
END $$;
