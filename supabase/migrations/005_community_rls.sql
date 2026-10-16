-- SmartJirani RLS policies for community features
-- Run AFTER 004_community_features.sql

-- ============================================================
-- HELPER: are we a member of this neighbourhood?
-- ============================================================
CREATE OR REPLACE FUNCTION is_neighborhood_member(n_id UUID)
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER AS $$
    SELECT EXISTS (
        SELECT 1 FROM neighborhood_members
        WHERE neighborhood_id = n_id AND user_id = auth.uid()
    );
$$;

-- ============================================================
-- ENABLE RLS
-- ============================================================
ALTER TABLE street_status ENABLE ROW LEVEL SECURITY;
ALTER TABLE street_notices ENABLE ROW LEVEL SECURITY;
ALTER TABLE polls ENABLE ROW LEVEL SECURITY;
ALTER TABLE poll_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE poll_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE baraza_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE baraza_rsvps ENABLE ROW LEVEL SECURITY;
ALTER TABLE forum_threads ENABLE ROW LEVEL SECURITY;
ALTER TABLE thread_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE thread_replies ENABLE ROW LEVEL SECURITY;
ALTER TABLE harambee_projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_volunteers ENABLE ROW LEVEL SECURITY;
ALTER TABLE resources ENABLE ROW LEVEL SECURITY;
ALTER TABLE resource_loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE urgent_needs ENABLE ROW LEVEL SECURITY;
ALTER TABLE urgent_need_offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE harambee_funds ENABLE ROW LEVEL SECURITY;
ALTER TABLE harambee_contributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE shared_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE skills_offered ENABLE ROW LEVEL SECURITY;
ALTER TABLE profile_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_circle ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE household_members ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- MTAA FEED
-- ============================================================
CREATE POLICY "Street status readable by members" ON street_status
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Street status writable by members" ON street_status
    FOR INSERT WITH CHECK (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Street status updatable by members" ON street_status
    FOR UPDATE USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Notices readable by members" ON street_notices
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Notices insertable by members" ON street_notices
    FOR INSERT WITH CHECK (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Polls readable by members" ON polls
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Polls insertable by members" ON polls
    FOR INSERT WITH CHECK (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Poll options readable by everyone" ON poll_options
    FOR SELECT USING (true);

CREATE POLICY "Poll options insertable by poll owner" ON poll_options
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM polls p WHERE p.id = poll_id AND p.created_by = auth.uid())
    );

CREATE POLICY "Users read own votes" ON poll_votes
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Members cast votes" ON poll_votes
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Members retract votes" ON poll_votes
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- BARAZA
-- ============================================================
CREATE POLICY "Baraza events readable by members" ON baraza_events
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Baraza events insertable by members" ON baraza_events
    FOR INSERT WITH CHECK (is_neighborhood_member(neighborhood_id));

CREATE POLICY "RSVPs readable by everyone" ON baraza_rsvps
    FOR SELECT USING (true);

CREATE POLICY "Users RSVP as themselves" ON baraza_rsvps
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users cancel own RSVP" ON baraza_rsvps
    FOR DELETE USING (auth.uid() = user_id);

CREATE POLICY "Threads readable by members" ON forum_threads
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Members create threads" ON forum_threads
    FOR INSERT WITH CHECK (auth.uid() = author_id AND is_neighborhood_member(neighborhood_id));

CREATE POLICY "Authors update own threads" ON forum_threads
    FOR UPDATE USING (auth.uid() = author_id);

CREATE POLICY "Thread votes readable by everyone" ON thread_votes
    FOR SELECT USING (true);

CREATE POLICY "Members vote on threads" ON thread_votes
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Members remove own thread votes" ON thread_votes
    FOR DELETE USING (auth.uid() = user_id);

CREATE POLICY "Replies readable by everyone" ON thread_replies
    FOR SELECT USING (true);

CREATE POLICY "Members reply" ON thread_replies
    FOR INSERT WITH CHECK (auth.uid() = author_id);

CREATE POLICY "Projects readable by members" ON harambee_projects
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Projects insertable by members" ON harambee_projects
    FOR INSERT WITH CHECK (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Projects updatable by members" ON harambee_projects
    FOR UPDATE USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Volunteers readable by everyone" ON project_volunteers
    FOR SELECT USING (true);

CREATE POLICY "Users volunteer as themselves" ON project_volunteers
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users withdraw own volunteering" ON project_volunteers
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- UJAMAA HUB
-- ============================================================
CREATE POLICY "Resources readable by members" ON resources
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Owners create resources" ON resources
    FOR INSERT WITH CHECK (auth.uid() = owner_id AND is_neighborhood_member(neighborhood_id));

CREATE POLICY "Owners update resources" ON resources
    FOR UPDATE USING (auth.uid() = owner_id);

CREATE POLICY "Loans readable by involved parties" ON resource_loans
    FOR SELECT USING (
        auth.uid() = borrower_id
        OR EXISTS (SELECT 1 FROM resources r WHERE r.id = resource_id AND r.owner_id = auth.uid())
    );

CREATE POLICY "Borrowers request loans" ON resource_loans
    FOR INSERT WITH CHECK (auth.uid() = borrower_id);

CREATE POLICY "Borrowers update own loans" ON resource_loans
    FOR UPDATE USING (auth.uid() = borrower_id);

CREATE POLICY "Needs readable by members" ON urgent_needs
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Members raise needs" ON urgent_needs
    FOR INSERT WITH CHECK (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Need offers readable by everyone" ON urgent_need_offers
    FOR SELECT USING (true);

CREATE POLICY "Helpers offer help" ON urgent_need_offers
    FOR INSERT WITH CHECK (auth.uid() = helper_id);

CREATE POLICY "Helpers withdraw offers" ON urgent_need_offers
    FOR DELETE USING (auth.uid() = helper_id);

CREATE POLICY "Funds readable by members" ON harambee_funds
    FOR SELECT USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Funds updatable by members" ON harambee_funds
    FOR UPDATE USING (is_neighborhood_member(neighborhood_id));

CREATE POLICY "Contributions readable by their author" ON harambee_contributions
    FOR SELECT USING (auth.uid() = contributor_id);

CREATE POLICY "Users create own contributions" ON harambee_contributions
    FOR INSERT WITH CHECK (auth.uid() = contributor_id);

CREATE POLICY "Users update own pending contributions" ON harambee_contributions
    FOR UPDATE USING (auth.uid() = contributor_id);

-- ============================================================
-- RESIDENT PROFILE
-- ============================================================
CREATE POLICY "Shared items readable by everyone" ON shared_items
    FOR SELECT USING (true);

CREATE POLICY "Owners manage own shared items" ON shared_items
    FOR INSERT WITH CHECK (auth.uid() = owner_id);

CREATE POLICY "Owners update own shared items" ON shared_items
    FOR UPDATE USING (auth.uid() = owner_id);

CREATE POLICY "Skills readable by everyone" ON skills_offered
    FOR SELECT USING (true);

CREATE POLICY "Owners manage own skills" ON skills_offered
    FOR INSERT WITH CHECK (auth.uid() = owner_id);

CREATE POLICY "Owners update own skills" ON skills_offered
    FOR UPDATE USING (auth.uid() = owner_id);

CREATE POLICY "Reviews readable by everyone" ON profile_reviews
    FOR SELECT USING (true);

CREATE POLICY "Users write reviews as themselves" ON profile_reviews
    FOR INSERT WITH CHECK (auth.uid() = author_id);

CREATE POLICY "Security circle readable by owner" ON security_circle
    FOR SELECT USING (auth.uid() = profile_id);

CREATE POLICY "Security circle insertable by owner" ON security_circle
    FOR INSERT WITH CHECK (auth.uid() = profile_id);

CREATE POLICY "Security circle updatable by owner" ON security_circle
    FOR UPDATE USING (auth.uid() = profile_id);

CREATE POLICY "Privacy settings readable by owner" ON privacy_settings
    FOR SELECT USING (auth.uid() = profile_id);

CREATE POLICY "Privacy settings insertable by owner" ON privacy_settings
    FOR INSERT WITH CHECK (auth.uid() = profile_id);

CREATE POLICY "Privacy settings updatable by owner" ON privacy_settings
    FOR UPDATE USING (auth.uid() = profile_id);

CREATE POLICY "Household members readable by owner" ON household_members
    FOR SELECT USING (auth.uid() = profile_id);

CREATE POLICY "Household members insertable by owner" ON household_members
    FOR INSERT WITH CHECK (auth.uid() = profile_id);
