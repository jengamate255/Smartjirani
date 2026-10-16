-- 019: Consolidate duplicate read/is_read columns on notifications
--
-- 001 created is_read; 004 added a redundant read column. The app (Room)
-- uses isRead, which maps naturally to is_read. Sync read -> is_read
-- defensively, then drop the duplicate.

UPDATE notifications SET is_read = TRUE WHERE read AND NOT is_read;

ALTER TABLE notifications DROP COLUMN IF EXISTS read;
