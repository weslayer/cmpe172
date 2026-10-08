package com.cmpe172.rental.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cmpe172.rental.dto.AppointmentDto;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// Two real transactions against Postgres race for one fresh slot. Not @Transactional: each thread must commit.
@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookings;

    @Autowired
    private JdbcTemplate jdbc;

    private long slotId;

    @BeforeEach
    void createSlot() {
        slotId = jdbc.queryForObject("""
                INSERT INTO availability_slots (service_id, starts_at, ends_at)
                VALUES (1, now() + INTERVAL '30 days', now() + INTERVAL '31 days')
                RETURNING id
                """, Long.class);
    }

    @AfterEach
    void removeSlot() {
        jdbc.update("DELETE FROM appointments WHERE availability_slot_id = ?", slotId);
        jdbc.update("DELETE FROM availability_slots WHERE id = ?", slotId);
    }

    @Test
    void twoCustomersBookingTheSameSlotAtOnceExactlyOneWins() throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<AppointmentDto>> attempts = new ArrayList<>();
        for (long customerId : new long[] {1, 4}) {
            attempts.add(pool.submit(() -> {
                go.await();
                return bookings.book(customerId, slotId);
            }));
        }
        go.countDown();

        int wins = 0;
        List<Throwable> losses = new ArrayList<>();
        for (Future<AppointmentDto> attempt : attempts) {
            try {
                attempt.get(10, TimeUnit.SECONDS);
                wins++;
            } catch (ExecutionException ex) {
                losses.add(ex.getCause());
            }
        }
        pool.shutdown();

        assertThat(wins).isEqualTo(1);
        // A ConflictException (not a DuplicateKeyException) means the row lock serialized them, not the backstop.
        assertThat(losses).singleElement().isInstanceOf(ConflictException.class);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM appointments WHERE availability_slot_id = ? AND status = 'BOOKED'",
                Integer.class, slotId)).isEqualTo(1);
    }
}
