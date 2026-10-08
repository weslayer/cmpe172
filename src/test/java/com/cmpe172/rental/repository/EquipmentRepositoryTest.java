package com.cmpe172.rental.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.cmpe172.rental.dto.EquipmentDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Runs against real Postgres; expected values come from seed.sql (six items, all provider 1).
@SpringBootTest
class EquipmentRepositoryTest {

    @Autowired
    private EquipmentRepository repository;

    @Test
    void honorsLimitInIdOrder() {
        List<EquipmentDto> featured = repository.findActive(null, 4);

        assertThat(featured).extracting(EquipmentDto::serviceId).containsExactly(1L, 2L, 3L, 4L);
        assertThat(featured).extracting(EquipmentDto::providerName).containsOnly("Bay Area Equipment Rentals");
    }

    @Test
    void filtersByProvider() {
        assertThat(repository.findActive(1L, 100)).hasSize(6).extracting(EquipmentDto::providerId).containsOnly(1L);
        assertThat(repository.findActive(99L, 100)).isEmpty();
    }

    @Test
    void findsTheOwningProvider() {
        assertThat(repository.findProviderId(1)).contains(1L);
        assertThat(repository.findProviderId(999)).isEmpty();
    }
}
