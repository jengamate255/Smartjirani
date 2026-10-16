-- 018: Add missing indexes on FK columns without a leading-column index
-- Ground truth from pg_constraint/pg_index diff on live database.
-- All statements idempotent.

-- Reverse-direction user lookups (UNIQUE(a,b) covers only leading column)
CREATE INDEX IF NOT EXISTS idx_baraza_rsvps_user ON baraza_rsvps(user_id);
CREATE INDEX IF NOT EXISTS idx_thread_votes_user ON thread_votes(user_id);
CREATE INDEX IF NOT EXISTS idx_project_volunteers_user ON project_volunteers(user_id);
CREATE INDEX IF NOT EXISTS idx_resource_loans_borrower ON resource_loans(borrower_id);
CREATE INDEX IF NOT EXISTS idx_urgent_need_offers_helper ON urgent_need_offers(helper_id);
CREATE INDEX IF NOT EXISTS idx_harambee_contributions_contributor ON harambee_contributions(contributor_id);
CREATE INDEX IF NOT EXISTS idx_poll_votes_user ON poll_votes(user_id);

-- Ownership / authorship lookups
CREATE INDEX IF NOT EXISTS idx_baraza_events_created_by ON baraza_events(created_by);
CREATE INDEX IF NOT EXISTS idx_harambee_projects_created_by ON harambee_projects(created_by);
CREATE INDEX IF NOT EXISTS idx_polls_created_by ON polls(created_by);
CREATE INDEX IF NOT EXISTS idx_profile_reviews_author ON profile_reviews(author_id);
CREATE INDEX IF NOT EXISTS idx_thread_replies_author ON thread_replies(author_id);
CREATE INDEX IF NOT EXISTS idx_resources_owner ON resources(owner_id);
CREATE INDEX IF NOT EXISTS idx_urgent_needs_requester ON urgent_needs(requester_id);
CREATE INDEX IF NOT EXISTS idx_street_notices_created_by ON street_notices(created_by);

-- Neighborhood-scoped listing filters
CREATE INDEX IF NOT EXISTS idx_shared_items_neighborhood ON shared_items(neighborhood_id);
CREATE INDEX IF NOT EXISTS idx_skills_offered_neighborhood ON skills_offered(neighborhood_id);
CREATE INDEX IF NOT EXISTS idx_polls_neighborhood ON polls(neighborhood_id);

-- Self-referencing FK (orphan lookups)
CREATE INDEX IF NOT EXISTS idx_messages_reply_to ON messages(reply_to_id);

-- Hot-path composite indexes
-- Notification feed pagination (newest first per user)
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);
-- Active/incoming call lookup (callee + status filter)
CREATE INDEX IF NOT EXISTS idx_call_requests_callee_status ON call_requests(callee_id, status);

-- Drop duplicate indexes created in 012 that shadow 001 indexes
DROP INDEX IF EXISTS idx_posts_author_id_fkey;
DROP INDEX IF EXISTS idx_post_comments_author_id_fkey;
