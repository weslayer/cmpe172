package com.cmpe172.rental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.dto.NotificationRequest;
import com.cmpe172.rental.notification.NotificationGateway;
import com.cmpe172.rental.repository.AppointmentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

    @Mock
    private NotificationGateway notifications;

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(booking, appointments, notifications, meters);
    }

    @Test
    void confirmsABookingOnlyAfterItsTransactionReturns() {
        when(booking.book(1, 5)).thenReturn(appointment("BOOKED"));

        service.book(1, 5);

        InOrder order = inOrder(booking, notifications);
        order.verify(booking).book(1, 5);
        NotificationRequest sent = sent(order);
        assertThat(sent.type()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(sent.idempotencyKey()).isEqualTo("appointment-9-BOOKING_CONFIRMED");
        assertThat(sent.recipient()).isEqualTo("customer@example.com");
        assertThat(bookings("booked")).isEqualTo(1);
    }

    @Test
    void notifiesACancellationAfterItsTransactionReturns() {
        when(booking.cancel(1, 9)).thenReturn(appointment("CANCELLED"));

        service.cancel(1, 9);

        InOrder order = inOrder(booking, notifications);
        order.verify(booking).cancel(1, 9);
        NotificationRequest sent = sent(order);
        assertThat(sent.type()).isEqualTo("BOOKING_CANCELLED");
        assertThat(sent.idempotencyKey()).isEqualTo("appointment-9-BOOKING_CANCELLED");
    }

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
        verifyNoInteractions(notifications);
        assertThat(bookings("failed")).isEqualTo(1);
    }

    @Test
    void takenSlotIsNotRetriedOrNotified() {
        when(booking.book(1, 5)).thenThrow(new ConflictException("taken"));

        assertThatThrownBy(() -> service.book(1, 5)).isInstanceOf(ConflictException.class);
        verify(booking, times(1)).book(1, 5);
        verifyNoInteractions(notifications);
        assertThat(bookings("conflict")).isEqualTo(1);
    }

    private NotificationRequest sent(InOrder order) {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        order.verify(notifications).send(captor.capture());
        return captor.getValue();
    }

    private long bookings(String outcome) {
        return meters.get("rental.bookings").tag("outcome", outcome).timer().count();
    }

    private static AppointmentDto appointment(String status) {
        return new AppointmentDto(9, status, 5, TOMORROW, TOMORROW.plusHours(4), "Cordless Drill", "PT-DRILL-001",
                "Bay Area Tools", "Casey Customer", "customer@example.com");
    }
}
