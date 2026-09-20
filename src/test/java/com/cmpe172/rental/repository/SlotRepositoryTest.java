package com.cmpe172.rental.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.cmpe172.rental.dto.SlotDto;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Runs against a real Postgres (local docker-compose db, or the CI service
 * container) so Liquibase applies schema.sql and seed.sql. Assertions rely on
 * the fixed seed data in seed.sql — see that file for the slot/appointment
 * layout being asserted on here.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class SlotRepositoryTest {

    @Autowired
    private SlotRepository repository;

    @Test
    void excludesSlotsWithAnActiveAppointment() {
        List<SlotDto> slots = repository.findAvailable(1L, null, null, 0, 20);

        assertThat(slots).extracting(SlotDto::slotId).doesNotContain(1L).contains(2L, 3L);
    }

    @Test
    void filtersByServiceAndDate() {
        List<SlotDto> slots = repository.findAvailable(1L, null, LocalDate.of(2026, 9, 6), 0, 20);

        assertThat(slots).extracting(SlotDto::slotId).containsExactly(2L);
    }

    @Test
    void paginatesWithLimitAndOffset() {
        List<SlotDto> firstPage = repository.findAvailable(null, 1L, null, 0, 5);
        List<SlotDto> secondPage = repository.findAvailable(null, 1L, null, 1, 5);

        assertThat(firstPage).hasSize(5);
        assertThat(secondPage).isNotEmpty();
        assertThat(firstPage).doesNotContainAnyElementsOf(secondPage);
    }

    @Test
    void countMatchesFindAvailableSizeWhenPageCoversAllResults() {
        long count = repository.countAvailable(1L, null, null);

        assertThat(count).isEqualTo(repository.findAvailable(1L, null, null, 0, 20).size());
    }
}
