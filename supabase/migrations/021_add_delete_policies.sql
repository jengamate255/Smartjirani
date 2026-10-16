-- 021: Add missing DELETE policies (owner-scoped)
--
-- Live-database audit found 27 tables without any DELETE policy. This adds
-- policies where a clear row owner exists. Intentionally excluded:
--   harambee_contributions  (financial records stay immutable)
--   conferences / harambee_funds / street_status / neighborhoods
--                           (no user ownership column; admin-level, Phase 8)
--   call_requests / call_participants / conference_participants
--                           (ephemeral signaling; cleanup cron in Phase 9)

CREATE POLICY "Event creators can delete events"
    ON baraza_events FOR DELETE
    USING (created_by = auth.uid());

CREATE POLICY "Thread authors can delete threads"
    ON forum_threads FOR DELETE
    USING (author_id = auth.uid());

CREATE POLICY "Reply authors can delete replies"
    ON thread_replies FOR DELETE
    USING (author_id = auth.uid());

CREATE POLICY "Participants can delete conversations"
    ON conversations FOR DELETE
    USING (
        id IN (
            SELECT conversation_id FROM conversation_participants WHERE user_id = auth.uid()
        )
    );

CREATE POLICY "Senders can delete their messages"
    ON messages FOR DELETE
    USING (sender_id = auth.uid());

CREATE POLICY "Project creators can delete projects"
    ON harambee_projects FOR DELETE
    USING (created_by = auth.uid());

CREATE POLICY "Resource owners can delete resources"
    ON resources FOR DELETE
    USING (owner_id = auth.uid());

CREATE POLICY "Item owners can delete shared items"
    ON shared_items FOR DELETE
    USING (owner_id = auth.uid());

CREATE POLICY "Skill owners can delete their skills"
    ON skills_offered FOR DELETE
    USING (owner_id = auth.uid());

CREATE POLICY "Poll creators can delete polls"
    ON polls FOR DELETE
    USING (created_by = auth.uid());

CREATE POLICY "Notice creators can delete notices"
    ON street_notices FOR DELETE
    USING (created_by = auth.uid());

CREATE POLICY "Requesters can delete their urgent needs"
    ON urgent_needs FOR DELETE
    USING (requester_id = auth.uid());

CREATE POLICY "Members can remove themselves from households"
    ON household_members FOR DELETE
    USING (profile_id = auth.uid());

CREATE POLICY "Review authors can delete their reviews"
    ON profile_reviews FOR DELETE
    USING (author_id = auth.uid());

CREATE POLICY "Users can remove their security circle entries"
    ON security_circle FOR DELETE
    USING (profile_id = auth.uid());

CREATE POLICY "Callers can delete their call records"
    ON calls FOR DELETE
    USING (caller_id = auth.uid());
