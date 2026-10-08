package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.model.Slot;
import com.cmpe172.rental.repository.AppointmentRepository;
import com.cmpe172.rental.repository.SlotRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

// One booking attempt = one transaction. BookingService retries it from outside the transaction.
@Service
public class BookingTransaction {

    private final SlotRepository slots;
    private final AppointmentRepository appointments;

    public BookingTransaction(SlotRepository slots, AppointmentRepository appointments) {
        this.slots = slots;
        this.appointments = appointments;
    }

    // READ COMMITTED: every statement after the lock takes a fresh snapshot, so the booked check sees the winner.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentDto book(long customerId, long slotId) {
        Slot slot = slots.lock(slotId)
                .orElseThrow(() -> new NotFoundException("Slot " + slotId + " not found"));
        if (!"OPEN".equals(slot.status()) || !slot.startsAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("Slot " + slotId + " is no longer available");
        }
        if (appointments.existsBooked(slotId)) {
            throw new ConflictException("Slot " + slotId + " is already booked");
        }
        return appointments.get(appointments.insertBooked(slotId, customerId));
    }
}
