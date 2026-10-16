-- 022: Optimize karma_summary
--
-- Original karma_summary() runs 8 scalar subqueries per call. Fine for a
-- single profile, but a leaderboard calling it per row becomes O(8N).
-- karma_summary_bulk() computes all counts in one pass (8 index scans total,
-- regardless of profile count) via UNION ALL + FILTER pivoting.
-- karma_summary() is retained as a thin wrapper for the single-profile
-- call path the app uses today.

CREATE OR REPLACE FUNCTION karma_summary_bulk(p_profile_ids UUID[])
RETURNS TABLE (
    profile_id UUID,
    score NUMERIC,
    trust_pct INTEGER,
    actions_count INTEGER,
    favors_done INTEGER,
    shared_items INTEGER,
    barazas INTEGER,
    rank_label TEXT
) LANGUAGE sql STABLE AS $$
    WITH events AS (
        SELECT author_id AS pid, 'post'::text AS kind
            FROM posts WHERE author_id = ANY (p_profile_ids)
        UNION ALL
        SELECT pp.author_id, 'like_recv'::text
            FROM post_likes pl
            JOIN posts pp ON pp.id = pl.post_id
            WHERE pp.author_id = ANY (p_profile_ids)
        UNION ALL
        SELECT author_id, 'comment'::text
            FROM post_comments WHERE author_id = ANY (p_profile_ids)
        UNION ALL
        SELECT user_id, 'like_given'::text
            FROM post_likes WHERE user_id = ANY (p_profile_ids)
        UNION ALL
        SELECT user_id, 'rsvp'::text
            FROM baraza_rsvps WHERE user_id = ANY (p_profile_ids)
        UNION ALL
        SELECT user_id, 'volunteer'::text
            FROM project_volunteers WHERE user_id = ANY (p_profile_ids)
        UNION ALL
        SELECT owner_id, 'shared'::text
            FROM resources WHERE owner_id = ANY (p_profile_ids)
        UNION ALL
        SELECT helper_id, 'help'::text
            FROM urgent_need_offers WHERE helper_id = ANY (p_profile_ids)
    ),
    pivoted AS (
        SELECT pid,
               COUNT(*) FILTER (WHERE kind = 'post')       AS posts_made,
               COUNT(*) FILTER (WHERE kind = 'like_recv')  AS likes_received,
               COUNT(*) FILTER (WHERE kind = 'comment')    AS comments_made,
               COUNT(*) FILTER (WHERE kind = 'like_given') AS likes_given,
               COUNT(*) FILTER (WHERE kind = 'rsvp')       AS rsvps,
               COUNT(*) FILTER (WHERE kind = 'volunteer')  AS volunteers,
               COUNT(*) FILTER (WHERE kind = 'shared')     AS items_shared,
               COUNT(*) FILTER (WHERE kind = 'help')       AS helps
        FROM events GROUP BY pid
    )
    SELECT
        u.pid,
        ROUND(LEAST(5.0, 3.0 + (COALESCE(v.likes_received, 0) * 0.02
                                + COALESCE(v.comments_made, 0) * 0.01
                                + COALESCE(v.helps, 0) * 0.05))::NUMERIC, 1),
        ROUND(LEAST(99, 70 + COALESCE(v.likes_received, 0) + COALESCE(v.helps, 0))::INTEGER),
        (COALESCE(v.posts_made, 0) + COALESCE(v.comments_made, 0) + COALESCE(v.likes_given, 0)
         + COALESCE(v.rsvps, 0) + COALESCE(v.volunteers, 0) + COALESCE(v.helps, 0))::INTEGER,
        (COALESCE(v.helps, 0) + COALESCE(v.likes_given, 0))::INTEGER,
        COALESCE(v.items_shared, 0)::INTEGER,
        (COALESCE(v.rsvps, 0) + COALESCE(v.volunteers, 0))::INTEGER,
        CASE
            WHEN COALESCE(v.likes_received, 0) >= 40 THEN 'Bora 5% ya Mtaa'
            WHEN COALESCE(v.likes_received, 0) >= 15 THEN 'Jirani wa Kuaminika'
            ELSE 'Mjanzee wa Mtaa'
        END
    FROM unnest(p_profile_ids) AS u(pid)
    LEFT JOIN pivoted v ON v.pid = u.pid;
$$;

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
    SELECT score, trust_pct, actions_count, favors_done, shared_items, barazas, rank_label
    FROM karma_summary_bulk(ARRAY[p_profile_id]);
$$;

REVOKE EXECUTE ON FUNCTION karma_summary_bulk(UUID[]) FROM anon;
GRANT EXECUTE ON FUNCTION karma_summary_bulk(UUID[]) TO authenticated;
