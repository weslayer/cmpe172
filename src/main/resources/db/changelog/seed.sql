-- Sample data (changeset 002-seed). Seed users share the bcrypt hash of "password".

-- Users: one customer, one provider, one admin.
INSERT INTO users (id, email, full_name, password_hash, role) VALUES
    (1, 'customer@example.com', 'Casey Customer', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CUSTOMER'),
    (2, 'provider@example.com', 'Pat Provider', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'PROVIDER'),
    (3, 'admin@example.com',    'Alex Admin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');

-- Provider profile for the provider user.
INSERT INTO providers (id, user_id, business_name, description) VALUES
    (1, 2, 'Bay Area Equipment Rentals', 'Tools and heavy equipment for construction and home projects.');

-- Equipment items (services), each with a unique asset tag.
INSERT INTO services (id, provider_id, name, category, description, asset_tag, daily_rate, status) VALUES
    (1, 1, 'DeWalt 20V Cordless Drill', 'Power Tools',     'Compact cordless drill with two batteries.',       'PT-DRILL-001', 15.00, 'ACTIVE'),
    (2, 1, 'Honda EU2200i Generator',   'Generators',      'Quiet 2200W inverter generator.',                  'GEN-HONDA-002', 45.00, 'ACTIVE'),
    (3, 1, '20ft Extension Ladder',     'Ladders',         'Aluminum extension ladder, 20 feet.',              'LAD-EXT-003',  12.50, 'ACTIVE'),
    (4, 1, 'Pressure Washer 3000 PSI',  'Cleaning',        'Gas pressure washer, 3000 PSI.',                   'CLN-PW-004',   35.00, 'ACTIVE'),
    (5, 1, 'Mini Excavator',            'Heavy Equipment', '1.5 ton mini excavator with operator training.',   'HEQ-EXC-005', 250.00, 'ACTIVE'),
    (6, 1, 'Scaffolding Set',           'Access',          'Modular scaffolding set with guardrails.',         'ACC-SCAF-006', 60.00, 'ACTIVE');

-- Availability slots (provider 1), spread across September 2026.
INSERT INTO availability_slots (id, service_id, provider_id, starts_at, ends_at, status) VALUES
    (1,  1, 1, '2026-09-05 09:00:00-07', '2026-09-05 17:00:00-07', 'OPEN'),
    (2,  1, 1, '2026-09-06 09:00:00-07', '2026-09-06 17:00:00-07', 'OPEN'),
    (3,  1, 1, '2026-09-07 09:00:00-07', '2026-09-07 17:00:00-07', 'OPEN'),
    (4,  2, 1, '2026-09-05 08:00:00-07', '2026-09-05 18:00:00-07', 'OPEN'),
    (5,  2, 1, '2026-09-08 08:00:00-07', '2026-09-08 18:00:00-07', 'OPEN'),
    (6,  3, 1, '2026-09-05 10:00:00-07', '2026-09-05 16:00:00-07', 'OPEN'),
    (7,  3, 1, '2026-09-10 10:00:00-07', '2026-09-10 16:00:00-07', 'OPEN'),
    (8,  4, 1, '2026-09-06 09:00:00-07', '2026-09-06 15:00:00-07', 'OPEN'),
    (9,  4, 1, '2026-09-09 09:00:00-07', '2026-09-09 15:00:00-07', 'OPEN'),
    (10, 5, 1, '2026-09-05 07:00:00-07', '2026-09-05 19:00:00-07', 'OPEN'),
    (11, 5, 1, '2026-09-12 07:00:00-07', '2026-09-12 19:00:00-07', 'OPEN'),
    (12, 6, 1, '2026-09-07 08:00:00-07', '2026-09-07 17:00:00-07', 'OPEN'),
    (13, 6, 1, '2026-09-11 08:00:00-07', '2026-09-11 17:00:00-07', 'OPEN');

-- Example appointment: confirmed booking on slot 1 (so slot 1 is not available).
INSERT INTO appointments (id, availability_slot_id, customer_id, service_id, status) VALUES
    (1, 1, 1, 1, 'CONFIRMED');

-- Advance identity sequences past the explicit IDs.
SELECT setval(pg_get_serial_sequence('users', 'id'),              (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('providers', 'id'),          (SELECT MAX(id) FROM providers));
SELECT setval(pg_get_serial_sequence('services', 'id'),           (SELECT MAX(id) FROM services));
SELECT setval(pg_get_serial_sequence('availability_slots', 'id'), (SELECT MAX(id) FROM availability_slots));
SELECT setval(pg_get_serial_sequence('appointments', 'id'),       (SELECT MAX(id) FROM appointments));
