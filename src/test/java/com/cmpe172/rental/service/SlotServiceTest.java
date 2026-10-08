package com.cmpe172.rental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cmpe172.rental.dto.CreateSlotRequest;
import com.cmpe172.rental.dto.PageResponse;
import com.cmpe172.rental.dto.SlotDto;
import com.cmpe172.rental.model.Slot;
import com.cmpe172.rental.repository.AppointmentRepository;
import com.cmpe172.rental.repository.EquipmentRepository;
import com.cmpe172.rental.repository.SlotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    private static final OffsetDateTime TOMORROW = OffsetDateTime.now().plusDays(1);
    private static final CreateSlotRequest NEW_SLOT = new CreateSlotRequest(3L, TOMORROW, TOMORROW.plusHours(8));

    @Mock
    private SlotRepository repository;

    @Mock
    private EquipmentRepository equipment;

    @Mock
    private AppointmentRepository appointments;

    @InjectMocks
    private SlotService service;

    @Test
    void wrapsRepositoryResultsInAPageResponse() {
        LocalDate date = LocalDate.of(2026, 9, 6);
        SlotDto slot = new SlotDto(2, 1, "Drill", "Power Tools", "PT-DRILL-001",
                1, "Bay Area Equipment Rentals", new BigDecimal("15.00"),
                OffsetDateTime.parse("2026-09-06T09:00:00-07:00"),
                OffsetDateTime.parse("2026-09-06T17:00:00-07:00"));
        when(repository.findAvailable(1L, null, date, 0, 20)).thenReturn(List.of(slot));
        when(repository.countAvailable(1L, null, date)).thenReturn(1L);

        PageResponse<SlotDto> result = service.findAvailable(1L, null, date, 0, 20);

        assertThat(result.content()).containsExactly(slot);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void createsASlotOnOwnEquipment() {
        when(equipment.findProviderId(3)).thenReturn(Optional.of(1L));
        when(repository.insert(3, NEW_SLOT.startsAt(), NEW_SLOT.endsAt())).thenReturn(20L);

        service.createSlot(1, NEW_SLOT);

        verify(repository).get(20);
    }

    @Test
    void cannotCreateASlotOnAnotherProvidersEquipment() {
        when(equipment.findProviderId(3)).thenReturn(Optional.of(2L));

        assertThatThrownBy(() -> service.createSlot(1, NEW_SLOT)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).insert(anyLong(), any(), any());
    }

    @Test
    void unknownEquipmentIsNotFound() {
        when(equipment.findProviderId(3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSlot(1, NEW_SLOT)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removesAnUnbookedSlotByBlockingIt() {
        when(repository.lock(5)).thenReturn(Optional.of(new Slot(1, "OPEN", TOMORROW)));

        service.removeSlot(1, 5);

        verify(repository).block(5);
    }

    @Test
    void cannotRemoveAnotherProvidersSlot() {
        when(repository.lock(5)).thenReturn(Optional.of(new Slot(2, "OPEN", TOMORROW)));

        assertThatThrownBy(() -> service.removeSlot(1, 5)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).block(5);
    }

    @Test
    void cannotRemoveABookedSlot() {
        when(repository.lock(5)).thenReturn(Optional.of(new Slot(1, "OPEN", TOMORROW)));
        when(appointments.existsBooked(5)).thenReturn(true);

        assertThatThrownBy(() -> service.removeSlot(1, 5)).isInstanceOf(ConflictException.class);
        verify(repository, never()).block(5);
    }
}
