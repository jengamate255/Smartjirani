-- 015: Add call signaling tables for voice/video calls via LiveKit

-- Call requests table for signaling
CREATE TABLE IF NOT EXISTS public.call_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caller_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    callee_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    call_type TEXT NOT NULL CHECK (call_type IN ('voice', 'video', 'conference')),
    room_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ringing' CHECK (status IN ('ringing', 'active', 'ended', 'missed', 'declined')),
    conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ
);

-- Index for efficient lookups
CREATE INDEX IF NOT EXISTS idx_call_requests_caller ON public.call_requests(caller_id);
CREATE INDEX IF NOT EXISTS idx_call_requests_callee ON public.call_requests(callee_id);
CREATE INDEX IF NOT EXISTS idx_call_requests_status ON public.call_requests(status);
CREATE INDEX IF NOT EXISTS idx_call_requests_conversation ON public.call_requests(conversation_id);

-- Enable realtime for call requests
ALTER PUBLICATION supabase_realtime ADD TABLE public.call_requests;

-- RLS policies
ALTER TABLE public.call_requests ENABLE ROW LEVEL SECURITY;

-- Users can see calls they initiated or received
CREATE POLICY "Users can view their own calls"
    ON public.call_requests FOR SELECT
    to authenticated
    USING (auth.uid() = caller_id OR auth.uid() = callee_id);

-- Users can create call requests (to initiate calls)
CREATE POLICY "Users can create call requests"
    ON public.call_requests FOR INSERT
    to authenticated
    WITH CHECK (auth.uid() = caller_id);

-- Users can update calls they are involved in (to accept/decline/end)
CREATE POLICY "Users can update their own calls"
    ON public.call_requests FOR UPDATE
    to authenticated
    USING (auth.uid() = caller_id OR auth.uid() = callee_id)
    WITH CHECK (auth.uid() = caller_id OR auth.uid() = callee_id);

-- Function to get active call for the current user
CREATE OR REPLACE FUNCTION get_active_call_for_user()
RETURNS TABLE (
    id UUID,
    caller_id UUID,
    callee_id UUID,
    call_type TEXT,
    room_name TEXT,
    status TEXT,
    created_at TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY INVOKER
AS $$
BEGIN
    RETURN QUERY
    SELECT cr.id, cr.caller_id, cr.callee_id, cr.call_type, cr.room_name, cr.status, cr.created_at
    FROM public.call_requests cr
    WHERE (cr.caller_id = auth.uid() OR cr.callee_id = auth.uid())
      AND cr.status IN ('ringing', 'active')
    ORDER BY cr.created_at DESC
    LIMIT 1;
END;
$$;
