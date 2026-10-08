package com.cmpe172.rental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.model.Slot;
import com.cmpe172.rental.repository.AppointmentRepository;
import com.cmpe172.rental.repository.SlotRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingTransactionTest {

    private static final OffsetDateTime TOMORROW = OffsetDateTime.now().plusDays(1);

    @Mock
    private SlotRepository slots;

    @Mock
    private AppointmentRepository appointments;

    @InjectMocks
    private BookingTransaction booking;

    @Test
    void booksAnOpenFutureSlot() {
        AppointmentDto booked = mock(AppointmentDto.class);
        when(slots.lock(5)).thenReturn(Optional.of(new Slot(1, "OPEN", TOMORROW)));
        when(appointments.insertBooked(5, 1)).thenReturn(9L);
        when(appointments.get(9)).thenReturn(booked);

        assertThat(booking.book(1, 5)).isSameAs(booked);
    }

    @Test
    void missingSlotIsNotFound() {
        when(slots.lock(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> booking.book(1, 5)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void blockedSlotIsAConflict() {
        when(slots.lock(5)).thenReturn(Optional.of(new Slot(1, "BLOCKED", TOMORROW)));

        assertThatThrownBy(() -> booking.book(1, 5)).isInstanceOf(ConflictException.class);
    }

    @Test
    void slotThatAlreadyStartedIsAConflict() {
        when(slots.lock(5)).thenReturn(Optional.of(new Slot(1, "OPEN", OffsetDateTime.now().minusHours(1))));

        assertThatThrownBy(() -> booking.book(1, 5)).isInstanceOf(ConflictException.class);
    }

    @Test
    void bookedSlotIsAConflictAndNothingIsInserted() {
        when(slots.lock(5)).thenReturn(Optional.of(new Slot(1, "OPEN", TOMORROW)));
        when(appointments.existsBooked(5)).thenReturn(true);

        assertThatThrownBy(() -> booking.book(1, 5)).isInstanceOf(ConflictException.class);
        verify(appointments, never()).insertBooked(anyLong(), anyLong());
    }
}
