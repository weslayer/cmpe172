-- Appointment statuses become BOOKED / CANCELLED / COMPLETED (changeset 003-booking-status).

DROP INDEX uq_active_appointment_slot;

ALTER TABLE appointments DROP CONSTRAINT chk_appt_status;

UPDATE appointments SET status = 'BOOKED' WHERE status IN ('PENDING', 'CONFIRMED');

ALTER TABLE appointments
    ADD CONSTRAINT chk_appt_status CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED')),
    ALTER COLUMN status SET DEFAULT 'BOOKED';

-- Double-booking guard: at most one BOOKED appointment per slot.
CREATE UNIQUE INDEX uq_active_appointment_slot
    ON appointments (availability_slot_id)
    WHERE status = 'BOOKED';
