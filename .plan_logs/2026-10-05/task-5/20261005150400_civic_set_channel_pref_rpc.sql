create or replace function public.civic_set_channel_pref(p_pref text)
returns void
language sql security definer set search_path = public as $$
    update public.profiles
    set civic_channel_pref = p_pref
    where id = auth.uid();
$$;

grant execute on function public.civic_set_channel_pref(text) to authenticated;
