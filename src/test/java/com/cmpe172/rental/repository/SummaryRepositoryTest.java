package com.cmpe172.rental.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.cmpe172.rental.dto.FeaturedEquipmentDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

// Runs against real Postgres; expected values come from seed.sql.
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class SummaryRepositoryTest {

    @Autowired
    private SummaryRepository repository;

    @Test
    void countsSeededProvidersAndActiveEquipment() {
        assertThat(repository.countProviders()).isEqualTo(1);
        assertThat(repository.countActiveEquipment()).isEqualTo(6);
    }

    @Test
    void featuredEquipmentHonorsLimitInIdOrder() {
        List<FeaturedEquipmentDto> featured = repository.findFeatured(4);

        assertThat(featured).extracting(FeaturedEquipmentDto::serviceId).containsExactly(1L, 2L, 3L, 4L);
        assertThat(featured).extracting(FeaturedEquipmentDto::providerName)
                .containsOnly("Bay Area Equipment Rentals");
    }
}
