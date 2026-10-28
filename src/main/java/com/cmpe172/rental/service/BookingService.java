package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.dto.NotificationRequest;
import com.cmpe172.rental.notification.NotificationGateway;
import com.cmpe172.rental.repository.AppointmentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

// Commit first, then notify: each BookingTransaction call has committed before the remote call starts.
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final BookingTransaction booking;
    private final AppointmentRepository appointments;
    private final NotificationGateway notifications;
    private final MeterRegistry meters;

    public BookingService(BookingTransaction booking, AppointmentRepository appointments,
            NotificationGateway notifications, MeterRegistry meters) {
        this.booking = booking;
        this.appointments = appointments;
        this.notifications = notifications;
        this.meters = meters;
    }

    public AppointmentDto book(long customerId, long slotId) {
        AppointmentDto booked = timedBooking(customerId, slotId);
        notifications.send(notice(booked, "BOOKING_CONFIRMED", "is confirmed"));
        return booked;
    }

    public AppointmentDto cancel(long customerId, long appointmentId) {
        AppointmentDto cancelled = booking.cancel(customerId, appointmentId);
        log.info("Cancelled appointment {} for customer {}", appointmentId, customerId);
        notifications.send(notice(cancelled, "BOOKING_CANCELLED", "has been cancelled"));
        return cancelled;
    }

    public List<AppointmentDto> customerAppointments(long customerId) {
        return appointments.findByCustomer(customerId);
    }

    public List<AppointmentDto> providerAppointments(long providerId) {
        return appointments.findByProvider(providerId);
    }

    // Times the booking itself; the notification afterwards is not part of booking latency.
    private AppointmentDto timedBooking(long customerId, long slotId) {
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

    // The key is stable across retries, and each appointment is confirmed and cancelled at most once.
    private static NotificationRequest notice(AppointmentDto appointment, String type, String status) {
        return new NotificationRequest(
                appointment.customerEmail(),
                type,
                "Your rental of %s (%s) starting %s %s.".formatted(
                        appointment.serviceName(), appointment.assetTag(), appointment.startsAt(), status),
                "appointment-" + appointment.appointmentId() + "-" + type);
    }
}
