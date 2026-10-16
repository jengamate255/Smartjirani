-- SmartJirani Seed Data - Tanzanian Neighborhoods
-- Run this AFTER migrations and RLS policies

-- ============================================================
-- DEFAULT NEIGHBORHOODS - DAR ES SALAAM
-- ============================================================
INSERT INTO neighborhoods (id, name, city, region, country, latitude, longitude, radius) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Kariakoo', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.7924, 39.2083, 2000),
    ('a0000000-0000-0000-0000-000000000002', 'Masaki', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.7731, 39.2743, 3000),
    ('a0000000-0000-0000-0000-000000000003', 'Oysterbay', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.7690, 39.2850, 2500),
    ('a0000000-0000-0000-0000-000000000004', 'Upanga', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.7800, 39.2650, 2000),
    ('a0000000-0000-0000-0000-000000000005', 'Kinondoni', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.7700, 39.2400, 4000),
    ('a0000000-0000-0000-0000-000000000006', 'Temeke', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.8500, 39.2800, 5000),
    ('a0000000-0000-0000-0000-000000000007', 'Kigamboni', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.8300, 39.3000, 5000),
    ('a0000000-0000-0000-0000-000000000008', 'Mikocheni', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.7750, 39.2550, 3000),
    ('a0000000-0000-0000-0000-000000000009', 'Sinza', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.7650, 39.2350, 2500),
    ('a0000000-0000-0000-0000-000000000010', 'Mwananyamala', 'Dar es Salaam', 'Dar es Salaam', 'Tanzania', -6.7600, 39.2300, 3000),

-- MOSHI
    ('a0000000-0000-0000-0000-000000000011', 'Rau', 'Moshi', 'Kilimanjaro', 'Tanzania', -3.3500, 37.3300, 3000),
    ('a0000000-0000-0000-0000-000000000012', 'Kiusa', 'Moshi', 'Kilimanjaro', 'Tanzania', -3.3400, 37.3200, 2500),

-- ARUSHA
    ('a0000000-0000-0000-0000-000000000013', 'Sekei', 'Arusha', 'Arusha', 'Tanzania', -3.3600, 36.6800, 3000),
    ('a0000000-0000-0000-0000-000000000014', 'Olasiti', 'Arusha', 'Arusha', 'Tanzania', -3.3700, 36.6900, 2500),
    ('a0000000-0000-0000-0000-000000000015', 'Njiro', 'Arusha', 'Arusha', 'Tanzania', -3.3800, 36.7000, 3500),

-- DODOMA
    ('a0000000-0000-0000-0000-000000000016', 'Majengo', 'Dodoma', 'Dodoma', 'Tanzania', -6.1700, 35.7500, 3000),
    ('a0000000-0000-0000-0000-000000000017', 'Iyumbu', 'Dodoma', 'Dodoma', 'Tanzania', -6.1600, 35.7400, 2500),

-- ZANZIBAR
    ('a0000000-0000-0000-0000-000000000018', 'Stone Town', 'Zanzibar City', 'Zanzibar', 'Tanzania', -6.1620, 39.1920, 2000),
    ('a0000000-0000-0000-0000-000000000019', 'Ng''ambo', 'Zanzibar City', 'Zanzibar', 'Tanzania', -6.1650, 39.2050, 3000),

-- MBEYA
    ('a0000000-0000-0000-0000-000000000020', 'Sinde', 'Mbeya', 'Mbeya', 'Tanzania', -8.9000, 33.4500, 3000)

ON CONFLICT (id) DO NOTHING;
