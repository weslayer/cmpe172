package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.repository.AppointmentRepository;
import java.util.List;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

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

    public AppointmentDto cancel(long customerId, long appointmentId) {
        return booking.cancel(customerId, appointmentId);
    }

    public List<AppointmentDto> customerAppointments(long customerId) {
        return appointments.findByCustomer(customerId);
    }

    public List<AppointmentDto> providerAppointments(long providerId) {
        return appointments.findByProvider(providerId);
    }
}
