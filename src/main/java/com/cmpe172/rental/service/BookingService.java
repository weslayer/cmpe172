package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.repository.AppointmentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final BookingTransaction booking;
    private final AppointmentRepository appointments;
    private final MeterRegistry meters;

    public BookingService(BookingTransaction booking, AppointmentRepository appointments, MeterRegistry meters) {
        this.booking = booking;
        this.appointments = appointments;
        this.meters = meters;
    }

    public AppointmentDto book(long customerId, long slotId) {
        Timer.Sample sample = Timer.start(meters);
        String outcome = "failed";
        try {
            AppointmentDto booked = bookWithRetry(customerId, slotId);
            outcome = "booked";
            log.info("Booked appointment {} on slot {} for customer {}", booked.appointmentId(), slotId, customerId);
            return booked;
        } catch (ConflictException | DuplicateKeyException ex) {
            outcome = "conflict";
            log.info("Slot {} not booked for customer {}: {}", slotId, customerId, ex.getMessage());
            throw ex;
        } finally {
            sample.stop(Timer.builder("rental.bookings")
                    .description("Booking requests by outcome")
                    .tag("outcome", outcome)
                    .register(meters));
        }
    }

    public AppointmentDto cancel(long customerId, long appointmentId) {
        AppointmentDto cancelled = booking.cancel(customerId, appointmentId);
        log.info("Cancelled appointment {} for customer {}", appointmentId, customerId);
        return cancelled;
    }

    public List<AppointmentDto> customerAppointments(long customerId) {
        return appointments.findByCustomer(customerId);
    }

    public List<AppointmentDto> providerAppointments(long providerId) {
        return appointments.findByProvider(providerId);
    }

    // Retries only transient lock failures (lock timeout, deadlock); a taken slot is a final answer.
    private AppointmentDto bookWithRetry(long customerId, long slotId) {
        for (int attempt = 1; ; attempt++) {
            try {
                return booking.book(customerId, slotId);
            } catch (PessimisticLockingFailureException ex) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Gave up booking slot {} for customer {} after {} lock timeouts", slotId, customerId, MAX_ATTEMPTS);
                    throw ex;
                }
                log.warn("Lock wait on slot {} timed out (attempt {}/{}), retrying", slotId, attempt, MAX_ATTEMPTS);
            }
        }
    }
}
