package com.cmpe172.rental.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cmpe172.rental.dto.AppointmentDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

// Real Postgres, rolled back after each test. Seed: appointment 1 books slot 1 (upcoming),
// and customer 1 has a second booking on a slot that already ended.
@SpringBootTest
@Transactional
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository repository;

    @Test
    void partialUniqueIndexRejectsASecondBookingForTheSameSlot() {
        assertThatThrownBy(() -> repository.insertBooked(1, 4)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void cancellingFreesTheSlotForAnotherBooking() {
        repository.markCancelled(1);

        assertThat(repository.existsBooked(1)).isFalse();
        assertThat(repository.get(repository.insertBooked(1, 4)).status()).isEqualTo("BOOKED");
    }

    @Test
    void pastBookingReadsAsCompleted() {
        assertThat(repository.findByCustomer(1))
                .extracting(AppointmentDto::status)
                .containsExactly("COMPLETED", "BOOKED");
    }
}
