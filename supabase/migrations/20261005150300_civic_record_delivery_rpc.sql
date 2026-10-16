-- Server-side insert helper for per-channel delivery rows.
-- Callable by:
--   * service_role (cron / system actor)
--   * the recipient themselves (FCM delivery ack from the app)
--   * SYSTEM_ADMIN (ops backfill)

create or replace function public.civic_record_delivery(
    p_notification_id uuid,
    p_channel text,
    p_provider text,
    p_recipient_user_id uuid,
    p_payload jsonb,
    p_status text,
    p_provider_response jsonb default null,
    p_error text default null,
    p_delivered_at timestamptz default null
)
returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_uid uuid := auth.uid();
    v_role text;
    v_id uuid;
begin
    if v_uid is null then
        -- Allow privileged callers: service_role JWT (PostgREST sets `auth.role() = 'service_role'`),
        -- or a Postgres-privileged session (pg_cron / direct admin). Anon JWT has
        -- `auth.role() = 'anon'` (not in allow list) and `session_user = 'authenticator'`
        -- (not in allow list) -> rejected.
        if auth.role() is distinct from 'service_role'
           and session_user not in ('postgres', 'service_role') then
            raise exception 'unauthenticated' using errcode = '42501';
        end if;
    end if;

    -- Real BYPASSRLS bypass: pg_cron (postgres role) and service_role Edge Functions
    -- (service_role) reach this RPC without a JWT. auth.uid() is null for them,
    -- and the guard above already admits them. No further role check is needed for
    -- the privileged path; they can insert for any recipient. The non-privileged
    -- paths below (self-ack, SYSTEM_ADMIN) continue to work exactly as before.
    if v_uid is not null then
        if v_uid = p_recipient_user_id then
            -- self-ack path
            null;
        else
            select role::text into v_role
            from public.user_roles
            where user_id = v_uid and is_active
            order by case role
                when 'SYSTEM_ADMIN' then 0
                when 'COUNCIL_ADMIN' then 1
                when 'MUNICIPAL_OFFICER' then 2
                else 9
            end
            limit 1;
            if v_role is distinct from 'SYSTEM_ADMIN' then
                raise exception 'not_authorized' using errcode = '42501';
            end if;
        end if;
    end if;

    insert into public.notification_deliveries(
        notification_id, channel, provider, recipient_user_id,
        payload, status, provider_response, error, delivered_at
    ) values (
        p_notification_id, p_channel, p_provider, p_recipient_user_id,
        p_payload, p_status, p_provider_response, p_error, p_delivered_at
    )
    returning id into v_id;

    return v_id;
end;
$$;

grant execute on function public.civic_record_delivery(
    uuid, text, text, uuid, jsonb, text, jsonb, text, timestamptz
) to authenticated;
