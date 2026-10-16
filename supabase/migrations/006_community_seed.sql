-- SmartJirani seed content for the four designed screens (Sheng copy)
-- Run AFTER 004_community_features.sql and 005_community_rls.sql.
-- Neighbourhood-scoped rows are seeded for Kariakoo; user-owned rows are seeded for the
-- first existing auth user (skipped when no account exists yet).

-- ============================================================
-- 1. MTAA FEED
-- ============================================================
INSERT INTO street_status (neighborhood_id, temperature_c, power_status, gate_status, street_note, updated_at)
SELECT id, 24, 'NORMAL', 'WAZI', 'Asubuhi tulivu · Geti 3 wazi', NOW()
FROM neighborhoods WHERE id = 'a0000000-0000-0000-0000-000000000001'
ON CONFLICT (neighborhood_id) DO NOTHING;

INSERT INTO street_notices (neighborhood_id, eyebrow, body, authority, status_label, expires_at)
SELECT id,
       'TAARIFA YA NYUMBA KUMI',
       'Lori la maji limefika geti la tatu la Kariakoo, litakaa mpaka saa nane mchana. Tafadhali songeni magari kidogo kwa muda.',
       'Imethibitishwa na Mzee Brian',
       'Hai · hadi saa 2',
       NOW() + interval '6 hours'
FROM neighborhoods WHERE id = 'a0000000-0000-0000-0000-000000000001';

INSERT INTO polls (neighborhood_id, eyebrow, question, subtext, ends_label, ends_at)
SELECT id,
       'SAUTI YA MTAA',
       'Kufanya usafi wa njia ya mto Jumamosi — saa zipi zinakufaa wewe?',
       'Glavu na mifuko ya taka zimewekwa na Kileleshwa Green Ward.',
       'Inaisha siku 2',
       NOW() + interval '2 days'
FROM neighborhoods WHERE id = 'a0000000-0000-0000-0000-000000000001';

INSERT INTO poll_options (poll_id, label, position)
SELECT p.id, o.label, o.pos
FROM polls p
CROSS JOIN (VALUES
    ('Saa 1 asubuhi - Saa 3 asubuhi', 1),
    ('Saa 2 na nusu - Saa 4 asubuhi', 2)
) AS o(label, pos)
WHERE p.question LIKE 'Kufanya usafi wa njia ya mto%'
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. BARAZA
-- ============================================================
INSERT INTO baraza_events (
    neighborhood_id, council_label, committee_label, district_label, households_label,
    quorum_label, title, mode, day_label, date_label, time_label, venue, stream_note,
    agenda, attending_count, questions_queued, event_at
)
SELECT id,
       'Baraza la Mtaa — Kariakoo & Msimbazi',
       'Wajumbe 9: Wazee na Wakala wa Majengo',
       'Mtaa 4B',
       'Maka 142',
       '84% Salama',
       'Baraza ya Kila Mwezi',
       'HYBRID',
       'JUM',
       '18',
       'Saa 4 asubuhi',
       'Uwanja wa Gazebo + Stream ya Zoom',
       'Kiungo cha live kitatumwa dakika 60 kabla ya kuanza',
       '["Makadirio ya taa za barabarani na zabuni ya mkandarasi", "Kuhifadhi video za CCTV ya lango kwenye mawingu", "Mpango wa kusafisha taka kila baada ya wiki mbili"]'::jsonb,
       68,
       12,
       NOW() + interval '9 days'
FROM neighborhoods WHERE id = 'a0000000-0000-0000-0000-000000000001';

INSERT INTO forum_threads (neighborhood_id, author_id, tag, title, body, solved)
SELECT n.id, u.id,
       'MAOMBI',
       'Vipaumbele karibu na shule — maombi kwa halmashauri',
       'Virahani wenzangu, magari yanazidi kasi karibu na lango la shule asubuhi wakati watoto wakiingia. Maombi tayari yamethibitishwa na Mwenyekiti Omari, tunaomba tuipeleke kwa halmashauri ya mtaa.',
       FALSE
FROM neighborhoods n, auth.users u
WHERE n.id = 'a0000000-0000-0000-0000-000000000001'
ORDER BY u.created_at LIMIT 1;

INSERT INTO forum_threads (neighborhood_id, author_id, tag, title, body, solved)
SELECT n.id, u.id,
       'HUDUMA',
       'Mtaalamu wa fiber net anayezuriwa — namba yupo hapa',
       'Mistari ya Safaricom/Zuku ilikatika wakati wa kazi za mabomba Court 3 jana. Hii ndiyo namba ya mtaalamu wa mtaa wa leo, mtumie moja kwa moja.',
       TRUE
FROM neighborhoods n, auth.users u
WHERE n.id = 'a0000000-0000-0000-0000-000000000001'
ORDER BY u.created_at LIMIT 1;

INSERT INTO harambee_projects (
    neighborhood_id, status, status_label, phase_label, category_label, title, subtitle,
    target_kes, raised_kes, progress_pct, progress_label, volunteer_slots, volunteers_filled,
    volunteer_note
)
SELECT id, 'ACTIVE', 'Kazi Inaendelea', 'Hatua ya 2', '', 'Kuweka taa za jua barabarani',
       'Kariakoo Mtaa 4 hadi 12 • sehemu yenye watu wengi usiku',
       320000, 240000, 75, 'Mfuko na vifaa vimepatikana', 15, 12,
       'Nafasi 12 kati ya 15 za kujitolea zimejaa'
FROM neighborhoods WHERE id = 'a0000000-0000-0000-0000-000000000001';

INSERT INTO harambee_projects (
    neighborhood_id, status, status_label, phase_label, category_label, title, subtitle,
    target_kes, raised_kes, progress_pct, progress_label, volunteer_slots, volunteers_filled,
    volunteer_note
)
SELECT id, 'SCHEDULED', 'Jumamosi Ijayo', '', 'Vijana na Familia', 'Kupanda miti na kusafisha mifereji',
       'Kingo za mifereji ya Msimbazi + mishamba 150 ya asili',
       0, 0, 0, '', 60, 42,
       'Jitolee - glavu nzito na mishamba zimewekwa'
FROM neighborhoods WHERE id = 'a0000000-0000-0000-0000-000000000001';

-- ============================================================
-- 3. UJAMAA HUB
-- ============================================================
INSERT INTO urgent_needs (
    neighborhood_id, title, body, requester_label, requester_meta, distance_label, status_label, is_open
)
SELECT id,
       'Oksijeni ya kupumua inahitajika kwa saa mbili',
       'Mtoto wa jirani ana mshindo wa ghafla wa kikohozi. Anaombwa na Dkt. Wanjiru (Kariakoo Mtaa, Nyumba 14C).',
       'Dkt. Grace Wanjiru',
       'Mzazi wa miaka 4 • Mtoaji wa Kwanza wa Msaada',
       'Umbali 80m',
       'Hai Sasa',
       TRUE
FROM neighborhoods WHERE id = 'a0000000-0000-0000-0000-000000000001';

INSERT INTO resources (
    neighborhood_id, owner_id, category, tag_label, title, description, distance_label,
    history_label, availability_note, deposit_note, cta_label, terms_label, available_until
)
SELECT n.id, u.id, 'TOOL', 'Zana', 'Foli za Bosch na Bits',
       'Seti ya drill ya mawe na mbao + betri ya ziada. Inapatikana kwa saa 24.',
       'Umbali 120m', 'Mikopo 14 imekamilika', 'Iko hadi Ijumaa', 'Amana ya Nia/Niama',
       'Omba Foli', 'Bila malipo', 'Ijumaa'
FROM neighborhoods n, auth.users u
WHERE n.id = 'a0000000-0000-0000-0000-000000000001'
ORDER BY u.created_at LIMIT 1;

INSERT INTO resources (
    neighborhood_id, owner_id, category, tag_label, title, description, distance_label,
    history_label, availability_note, deposit_note, cta_label, terms_label, available_until
)
SELECT n.id, u.id, 'GEAR', 'Vifaa', 'Ngazi ya kupaa (mita 3.5)',
       'Alumini imara, inafaa kwa taa za uwanja au kupitia mabamba ya maji.',
       'Umbali 50m', 'Msemaji wa Wazee wa Mtaa', 'Iko Sasa', 'Kuchukua kwenye geti',
       'Omba Ngazi', 'Bila malipo', 'Leo'
FROM neighborhoods n, auth.users u
WHERE n.id = 'a0000000-0000-0000-0000-000000000001'
ORDER BY u.created_at LIMIT 1;

INSERT INTO resources (
    neighborhood_id, owner_id, category, tag_label, title, description, distance_label,
    history_label, availability_note, deposit_note, cta_label, terms_label, available_until, meta
)
SELECT n.id, u.id, 'CARPOOL', 'Usafiri', 'Njia ya Upper Hill (Jumatatu-Ijumaa)',
       'Panda nasi kwenda Upper Hill, tunapita Ring Road na kituo cha basi cha mtaa.',
       'Jirani wa njia moja', 'Mfuatiliaji wa kila siku', 'Asubuhi 7:15', 'Kuchangia mafuta',
       'Jiunge na Carpool', 'Kubadilishana mazuri', 'Ijumaa',
       '{"route":"Upper Hill (Jumatatu-Ijumaa)","seats":"Viti 2 Bure","departure":"Asubuhi 7:15"}'::jsonb
FROM neighborhoods n, auth.users u
WHERE n.id = 'a0000000-0000-0000-0000-000000000001'
ORDER BY u.created_at LIMIT 1;

INSERT INTO harambee_funds (
    neighborhood_id, name, subtitle, audit_label, target_kes, raised_kes, progress_pct,
    purpose_label, remaining_label
)
SELECT id,
       'Mfuko wa Dharura na Walinda',
       'Harambee ya Kikundi cha Kariakoo',
       'Imepelelekwa',
       50000, 42500, 85,
       'Taa za geti na dawa za kwanza za mlinzi wa usiku',
       'KSh 7,500 zimebaki'
FROM neighborhoods WHERE id = 'a0000000-0000-0000-0000-000000000001';

-- ============================================================
-- 4. RESIDENT PROFILE (needs an existing account)
-- ============================================================
DO $$
DECLARE
    v_user UUID;
    v_nbhd UUID := 'a0000000-0000-0000-0000-000000000001';
BEGIN
    SELECT id INTO v_user FROM auth.users ORDER BY created_at ASC LIMIT 1;
    IF v_user IS NULL THEN
        RAISE NOTICE 'No auth user yet - skipping resident profile seed';
        RETURN;
    END IF;

    INSERT INTO shared_items (owner_id, neighborhood_id, title, subtitle, state_label, is_available, loans_count) VALUES
        (v_user, v_nbhd, 'Mashine ya kusafisha kwa shinikizo', 'Imekopeshwa mara 9 • Iko sasa', 'Tayari', TRUE, 9),
        (v_user, v_nbhd, 'Hema ya watu 4 (maji yote)', 'Imekopeshwa mara 4 • Ipo na Juma (Court 2)', 'Imeazimwa', FALSE, 4),
        (v_user, v_nbhd, 'Mashine ya kukata nyasi', 'Imekopeshwa mara 6 • Iko sasa', 'Tayari', TRUE, 6)
    ON CONFLICT DO NOTHING;

    INSERT INTO skills_offered (owner_id, neighborhood_id, title, description, testimonial, endorsement_count) VALUES
        (v_user, v_nbhd, 'Kurekebisha mabomba madogo', 'Kubadilisha mifuniko ya mabomba, kurekebisha matembezi ya maji', 'Alisaidia kurekebisha pampu ya bustani wakati wa matatizo ya maji.', 1),
        (v_user, v_nbhd, 'Tafsiri ya Kiswahili / Kifaransa', 'Kusoma nyaraka, mazungumzo ya kila siku', 'Alisaidia majirani wapya kuingia na kukaa vizuri Court 1.', 1)
    ON CONFLICT DO NOTHING;

    INSERT INTO profile_reviews (subject_id, author_id, author_label, body, rating) VALUES
        (v_user, NULL, 'Kamau Njoroge (Court 3)', 'Alinishirikisha mashine yake ya kusafisha na hata kunonyoosha namna ya kusafisha matundu. Roho halisi ya Ujamaa!', 5),
        (v_user, NULL, 'Zainab Patel (Court 4)', 'Daima yupo barazani na ana haraka kujibu arifa za dharura za lango.', 5)
    ON CONFLICT DO NOTHING;

    INSERT INTO security_circle (profile_id, cluster_label, designation_label, sos_enabled, contacts) VALUES
        (v_user, 'Kikundi cha Mtaa 3', 'Kikundi cha Usalama', TRUE,
         '[{"label":"Mzee Brian (Kiongozi wa Kikundi)","icon":"person"},{"label":"Ulinzi wa Geti la Kariakoo","icon":"security"}]'::jsonb)
    ON CONFLICT (profile_id) DO NOTHING;

    INSERT INTO privacy_settings (profile_id, visibility_label, visibility_radius_m, house_number_visibility, household_label, status_label, show_house_number) VALUES
        (v_user, 'Majirani ndani ya mita 500 tu', 500, 'Majirani waliothibitishwa tu', 'Wakazi 3 wamesajiliwa - Mtaa 4, Nyumba H8', 'Imelindwa', FALSE)
    ON CONFLICT (profile_id) DO NOTHING;

    INSERT INTO household_members (profile_id, member_name, relation_label) VALUES
        (v_user, 'Mama Neema', 'Mama'),
        (v_user, 'Baraka M.', 'Ndugu'),
        (v_user, 'Zawadi K.', 'Dada')
    ON CONFLICT DO NOTHING;
END $$;
