package com.cmpe172.rental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cmpe172.rental.dto.EquipmentDto;
import com.cmpe172.rental.dto.HomeSummaryDto;
import com.cmpe172.rental.repository.EquipmentRepository;
import com.cmpe172.rental.repository.SlotRepository;
import com.cmpe172.rental.repository.SummaryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private HomeService service;

    @Test
    void assemblesSummaryFromRepositoryCounts() {
        EquipmentDto featured = new EquipmentDto(
                1, "Drill", "Power Tools", "PT-DRILL-001", new BigDecimal("15.00"), 1, "Bay Area Equipment Rentals");
        when(summaryRepository.countProviders()).thenReturn(1L);
        when(summaryRepository.countActiveEquipment()).thenReturn(6L);
        when(slotRepository.countAvailable(null, null, null)).thenReturn(12L);
        when(equipmentRepository.findActive(null, 4)).thenReturn(List.of(featured));

        HomeSummaryDto result = service.summary();

        assertThat(result.application()).isEqualTo("Equipment Rental");
        assertThat(result.providerCount()).isEqualTo(1);
        assertThat(result.equipmentCount()).isEqualTo(6);
        assertThat(result.availableSlotCount()).isEqualTo(12);
        assertThat(result.featuredEquipment()).containsExactly(featured);
    }
}
