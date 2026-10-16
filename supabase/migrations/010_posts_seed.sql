-- SmartJirani post seeds
-- The posts table was never seeded, so the Mtaa Feed's post list was empty
-- even though status/notices/polls had rows. Seed a handful of Kariakoo posts
-- using the earliest auth user as author (same pattern as forum_threads).

INSERT INTO posts (author_id, neighborhood_id, content, image_url, post_type, created_at, updated_at)
SELECT u.id,
       'a0000000-0000-0000-0000-000000000001',
       v.content,
       NULL,
       v.pt::post_type,
       NOW() - make_interval(mins => v.ago),
       NOW() - make_interval(mins => v.ago)
FROM (
    VALUES
        ('Maji yamekatika tangu asubuhi Court 3–7. Maji ni kutoka mabomba ya mtaa, tayari tumetoa taarifa kwa mamlaka. Subiri mpaka jioni.',
         'ALERT', 18),
        ('Mtumba bora sokoni leo! Viti, mashine ya kufulia na jiko la gesi — bei poa kwa wale wanaohitaji. Wasiliana DM.',
         'BUY_SELL', 55),
        ('Kumbusho: Usafi wa mtaa wiki hii ni Jumamosi saa 4 asubuhi. Kila nyumba itatuma mtu mmoja. Tuko pamoja!',
         'ANNOUNCEMENT', 140),
        ('Ninatafuta fundi wa umeme wa kesho asubuhi, umeme wangu umepotea kwa mara ya tatu. Mnajua yule anayejulikana Court 5?',
         'GENERAL', 260),
        ('Baraza la usalama linaelekea kukutana Ijumaa saa 3 asubuhi. Ajenda: taa za barabarani na milio ya pikipiki usiku.',
         'EVENT', 420),
        ('Nimepoteza mkufu wa dhahabu karibu na duka la Mama Neema jana jioni. Tafadhali mnionyeshe ukiona, shukrani.',
         'LOST_FOUND', 610)
) AS v(content, pt, ago)
CROSS JOIN (
    SELECT id FROM auth.users ORDER BY created_at ASC LIMIT 1
) u
WHERE EXISTS (SELECT 1 FROM auth.users);
