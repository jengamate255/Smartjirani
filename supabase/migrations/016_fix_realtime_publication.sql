-- 016: Fix realtime configuration for messaging and notifications

-- Messages table was missing from supabase_realtime publication (014 set REPLICA IDENTITY FULL but skipped this)
ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;

-- Conversations need realtime for live conversation list updates (new conversations, last_message_at changes)
ALTER TABLE conversations REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE public.conversations;

-- Notifications need realtime for live notification bell updates
ALTER TABLE notifications REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
