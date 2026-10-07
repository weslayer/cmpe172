-- Milestone 2 sample data (changeset 004-seed-m2).

-- The original slots were dated September 2026, which is now in the past; move them a year out.
UPDATE availability_slots
SET starts_at = starts_at + INTERVAL '1 year',
    ends_at   = ends_at + INTERVAL '1 year'
WHERE id BETWEEN 1 AND 13;

-- The hash in seed.sql never matched "password"; give every seed account a real BCrypt("password").
UPDATE users SET password_hash = '$2a$10$2gaB1zeBCL7tjShQUgFEj.O1OurLGZSl1LSXGKp6FD3rqeuL5YyZy';

-- A second customer (id 4), so owner-only cancel and two-user booking can be shown.
INSERT INTO users (email, full_name, password_hash, role) VALUES
    ('customer2@example.com', 'Jordan Customer', '$2a$10$2gaB1zeBCL7tjShQUgFEj.O1OurLGZSl1LSXGKp6FD3rqeuL5YyZy', 'CUSTOMER');

-- A past rental for customer 1, so appointment history shows a COMPLETED entry.
WITH past AS (
    INSERT INTO availability_slots (service_id, starts_at, ends_at)
    VALUES (2, '2026-09-20 08:00:00-07', '2026-09-20 18:00:00-07')
    RETURNING id)
INSERT INTO appointments (availability_slot_id, customer_id)
SELECT id, 1 FROM past;
