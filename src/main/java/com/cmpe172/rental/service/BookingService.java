package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.model.Appointment;
import com.cmpe172.rental.repository.AppointmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final int MAX_ATTEMPTS = 3;

    private final BookingTransaction booking;
    private final AppointmentRepository appointments;

    public BookingService(BookingTransaction booking, AppointmentRepository appointments) {
        this.booking = booking;
        this.appointments = appointments;
    }

    // Retries only transient lock failures (lock timeout, deadlock); a taken slot is a final answer.
    public AppointmentDto book(long customerId, long slotId) {
        for (int attempt = 1; ; attempt++) {
            try {
                return booking.book(customerId, slotId);
            } catch (PessimisticLockingFailureException ex) {
                if (attempt == MAX_ATTEMPTS) {
                    throw ex;
                }
            }
        }
    }

    @Transactional
    public AppointmentDto cancel(long customerId, long appointmentId) {
        Appointment appointment = appointments.lock(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment " + appointmentId + " not found"));
        if (appointment.customerId() != customerId) {
            throw new ForbiddenException("Appointment " + appointmentId + " belongs to another customer");
        }
        if (!"BOOKED".equals(appointment.status()) || !appointment.startsAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("Appointment " + appointmentId + " can no longer be cancelled");
        }
        appointments.markCancelled(appointmentId);
        return appointments.get(appointmentId);
    }

    public List<AppointmentDto> customerAppointments(long customerId) {
        return appointments.findByCustomer(customerId);
    }
}
