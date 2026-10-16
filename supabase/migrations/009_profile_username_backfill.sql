-- SmartJirani profile usernames
-- profiles.username defaults to '' and neither the signup trigger nor the
-- profile backfill set it, so the profile header rendered a bare "@".
-- 1) Backfill existing rows (local part of the email, collision-safe).
-- 2) Make handle_new_user assign a username at signup.

-- ============================================================
-- BACKFILL existing empty usernames
-- ============================================================
DO $$
DECLARE
    r RECORD;
    v TEXT;
BEGIN
    FOR r IN SELECT id, display_name, email FROM profiles WHERE username = '' LOOP
        v := COALESCE(
            NULLIF(split_part(r.email, '@', 1), ''),
            NULLIF(r.display_name, ''),
            'mtaani'
        );
        IF EXISTS (SELECT 1 FROM profiles WHERE username = v) THEN
            v := v || '_' || left(replace(r.id::text, '-', ''), 8);
        END IF;
        -- last resort: the id fragment is unique, so iterate until free
        WHILE EXISTS (SELECT 1 FROM profiles WHERE username = v) LOOP
            v := v || '_';
        END LOOP;
        UPDATE profiles SET username = v WHERE id = r.id;
    END LOOP;
END $$;

-- ============================================================
-- SIGNUP TRIGGER assigns a username too
-- ============================================================
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    v_username TEXT;
BEGIN
    v_username := COALESCE(
        NULLIF(NEW.raw_user_meta_data->>'username', ''),
        NULLIF(split_part(NEW.email, '@', 1), ''),
        'mtaani'
    );
    IF EXISTS (SELECT 1 FROM public.profiles WHERE username = v_username) THEN
        v_username := v_username || '_' || left(replace(NEW.id::text, '-', ''), 8);
    END IF;

    INSERT INTO public.profiles (id, email, display_name, username, created_at, updated_at)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'display_name', split_part(NEW.email, '@', 1)),
        v_username,
        NOW(),
        NOW()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
