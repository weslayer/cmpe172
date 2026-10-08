package com.cmpe172.rental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.model.Appointment;
import com.cmpe172.rental.repository.AppointmentRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final OffsetDateTime TOMORROW = OffsetDateTime.now().plusDays(1);

    @Mock
    private BookingTransaction booking;

    @Mock
    private AppointmentRepository appointments;

    @InjectMocks
    private BookingService service;

    @Test
    void retriesALockTimeoutThenSucceeds() {
        AppointmentDto booked = mock(AppointmentDto.class);
        when(booking.book(1, 5)).thenThrow(new CannotAcquireLockException("lock timeout")).thenReturn(booked);

        assertThat(service.book(1, 5)).isSameAs(booked);
        verify(booking, times(2)).book(1, 5);
    }

    @Test
    void givesUpAfterThreeLockTimeouts() {
        when(booking.book(1, 5)).thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> service.book(1, 5)).isInstanceOf(CannotAcquireLockException.class);
        verify(booking, times(3)).book(1, 5);
    }

    @Test
    void takenSlotIsNotRetried() {
        when(booking.book(1, 5)).thenThrow(new ConflictException("taken"));

        assertThatThrownBy(() -> service.book(1, 5)).isInstanceOf(ConflictException.class);
        verify(booking, times(1)).book(1, 5);
    }

    @Test
    void cancelsOwnUpcomingBooking() {
        AppointmentDto cancelled = mock(AppointmentDto.class);
        when(appointments.lock(7)).thenReturn(Optional.of(new Appointment(1, "BOOKED", TOMORROW)));
        when(appointments.get(7)).thenReturn(cancelled);

        assertThat(service.cancel(1, 7)).isSameAs(cancelled);
        verify(appointments).markCancelled(7);
    }

    @Test
    void cannotCancelAnotherCustomersBooking() {
        when(appointments.lock(7)).thenReturn(Optional.of(new Appointment(1, "BOOKED", TOMORROW)));

        assertThatThrownBy(() -> service.cancel(4, 7)).isInstanceOf(ForbiddenException.class);
        verify(appointments, never()).markCancelled(7);
    }

    @Test
    void cannotCancelTwiceOrAfterTheStart() {
        when(appointments.lock(7)).thenReturn(Optional.of(new Appointment(1, "CANCELLED", TOMORROW)));
        when(appointments.lock(8)).thenReturn(Optional.of(new Appointment(1, "BOOKED", OffsetDateTime.now().minusHours(1))));

        assertThatThrownBy(() -> service.cancel(1, 7)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.cancel(1, 8)).isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownAppointmentIsNotFound() {
        when(appointments.lock(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(1, 7)).isInstanceOf(NotFoundException.class);
    }
}
