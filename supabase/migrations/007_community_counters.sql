-- SmartJirani community counters + atomic actions
-- Run AFTER 004_community_features.sql and 005_community_rls.sql
--
-- Denormalised counters (attending_count, volunteers_filled, raised_kes/progress_pct) live on
-- rows the app user is NOT allowed to UPDATE under RLS, so every counter bump goes through a
-- SECURITY DEFINER function that verifies membership first and writes both rows atomically.

CREATE OR REPLACE FUNCTION public.cast_poll_vote(p_poll_id UUID, p_option_id UUID)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_uid UUID := auth.uid();
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM polls WHERE id = p_poll_id AND neighborhood_id IN (
            SELECT neighborhood_id FROM neighborhood_members WHERE user_id = v_uid
        )
    ) THEN
        RAISE EXCEPTION 'Not a member of this neighborhood';
    END IF;

    DELETE FROM poll_votes WHERE poll_id = p_poll_id AND user_id = v_uid;
    INSERT INTO poll_votes (poll_id, option_id, user_id)
    VALUES (p_poll_id, p_option_id, v_uid);
END;
$$;

CREATE OR REPLACE FUNCTION public.rsvp_baraza(p_event_id UUID)
RETURNS INTEGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_count INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    INSERT INTO baraza_rsvps (event_id, user_id)
    VALUES (p_event_id, v_uid)
    ON CONFLICT (event_id, user_id) DO NOTHING;

    UPDATE baraza_events
       SET attending_count = attending_count + 1
     WHERE id = p_event_id
       AND id IN (SELECT event_id FROM baraza_rsvps WHERE user_id = v_uid);

    SELECT attending_count INTO v_count FROM baraza_events WHERE id = p_event_id;
    RETURN COALESCE(v_count, 0);
END;
$$;

CREATE OR REPLACE FUNCTION public.cancel_rsvp_baraza(p_event_id UUID)
RETURNS INTEGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_count INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    DELETE FROM baraza_rsvps WHERE event_id = p_event_id AND user_id = v_uid;

    UPDATE baraza_events
       SET attending_count = GREATEST(attending_count - 1, 0)
     WHERE id = p_event_id;

    SELECT attending_count INTO v_count FROM baraza_events WHERE id = p_event_id;
    RETURN COALESCE(v_count, 0);
END;
$$;

CREATE OR REPLACE FUNCTION public.volunteer_project(p_project_id UUID)
RETURNS INTEGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_count INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    INSERT INTO project_volunteers (project_id, user_id)
    VALUES (p_project_id, v_uid)
    ON CONFLICT (project_id, user_id) DO NOTHING;

    UPDATE harambee_projects
       SET volunteers_filled = volunteers_filled + 1
     WHERE id = p_project_id;

    SELECT volunteers_filled INTO v_count FROM harambee_projects WHERE id = p_project_id;
    RETURN COALESCE(v_count, 0);
END;
$$;

CREATE OR REPLACE FUNCTION public.withdraw_volunteer(p_project_id UUID)
RETURNS INTEGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_count INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    DELETE FROM project_volunteers WHERE project_id = p_project_id AND user_id = v_uid;

    UPDATE harambee_projects
       SET volunteers_filled = GREATEST(volunteers_filled - 1, 0)
     WHERE id = p_project_id;

    SELECT volunteers_filled INTO v_count FROM harambee_projects WHERE id = p_project_id;
    RETURN COALESCE(v_count, 0);
END;
$$;

-- Confirms a PesaPal contribution and moves the money into the fund's public counters.
-- PENDING rows are inserted by the client before checkout; CONFIRMED is only written here.
CREATE OR REPLACE FUNCTION public.record_contribution(
    p_fund_id UUID,
    p_amount_kes NUMERIC,
    p_reference TEXT,
    p_checkout_request_id TEXT,
    p_status contribution_status DEFAULT 'CONFIRMED'
)
RETURNS NUMERIC LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_contrib_id UUID;
    v_total NUMERIC;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;
    IF p_amount_kes <= 0 THEN
        RAISE EXCEPTION 'Contribution must be positive';
    END IF;

    INSERT INTO harambee_contributions
        (fund_id, contributor_id, amount_kes, provider, reference, checkout_request_id, status)
    VALUES
        (p_fund_id, v_uid, p_amount_kes, 'PesaPal', COALESCE(p_reference, ''),
         COALESCE(p_checkout_request_id, ''), p_status)
    RETURNING id INTO v_contrib_id;

    UPDATE harambee_funds
       SET raised_kes = raised_kes + p_amount_kes,
           progress_pct = LEAST(100, ROUND((raised_kes + p_amount_kes) / NULLIF(target_kes, 0) * 100)::INTEGER)
     WHERE id = p_fund_id
    RETURNING raised_kes INTO v_total;

    RETURN COALESCE(v_total, 0);
END;
$$;

-- Same money maths for harambee projects (no fund row to attach to).
CREATE OR REPLACE FUNCTION public.record_project_contribution(
    p_project_id UUID,
    p_amount_kes NUMERIC,
    p_reference TEXT
)
RETURNS NUMERIC LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_project_fund UUID;
    v_total NUMERIC;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT id INTO v_project_fund FROM harambee_funds WHERE neighborhood_id = (
        SELECT neighborhood_id FROM harambee_projects WHERE id = p_project_id
    );
    IF v_project_fund IS NULL THEN
        RAISE EXCEPTION 'No harambee fund for this neighborhood';
    END IF;

    UPDATE harambee_projects
       SET raised_kes = raised_kes + p_amount_kes,
           progress_pct = LEAST(100, ROUND((raised_kes + p_amount_kes) / NULLIF(target_kes, 0) * 100)::INTEGER)
     WHERE id = p_project_id
    RETURNING raised_kes INTO v_total;

    INSERT INTO harambee_contributions
        (fund_id, contributor_id, amount_kes, provider, reference, checkout_request_id, status)
    VALUES
        (v_project_fund, v_uid, p_amount_kes, 'PesaPal', COALESCE(p_reference, ''), '',
         'CONFIRMED'::contribution_status);

    RETURN COALESCE(v_total, 0);
END;
$$;

GRANT EXECUTE ON FUNCTION public.cast_poll_vote(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.rsvp_baraza(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.cancel_rsvp_baraza(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.volunteer_project(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.withdraw_volunteer(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.record_contribution(UUID, NUMERIC, TEXT, TEXT, contribution_status) TO authenticated;
GRANT EXECUTE ON FUNCTION public.record_project_contribution(UUID, NUMERIC, TEXT) TO authenticated;
