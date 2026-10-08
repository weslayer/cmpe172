package com.cmpe172.rental.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Runs against real Postgres; expected values come from seed.sql.
@SpringBootTest
class SummaryRepositoryTest {

    @Autowired
    private SummaryRepository repository;

    @Test
    void countsSeededProvidersAndActiveEquipment() {
        assertThat(repository.countProviders()).isEqualTo(1);
        assertThat(repository.countActiveEquipment()).isEqualTo(6);
    }
}
