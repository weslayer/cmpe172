package com.cmpe172.rental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cmpe172.rental.dto.PageResponse;
import com.cmpe172.rental.dto.SlotDto;
import com.cmpe172.rental.repository.SlotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock
    private SlotRepository repository;

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
}
