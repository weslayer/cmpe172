package com.cmpe172.rental.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmpe172.rental.model.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

// Nothing listens on port 1, so every notification attempt fails; the booking must still commit and return 201.
@SpringBootTest(properties = "notification.base-url=http://localhost:1")
@AutoConfigureMockMvc
class NotificationDownIntegrationTest {

    private static final AuthenticatedUser CUSTOMER =
            new AuthenticatedUser(1, "customer@example.com", "x", "Casey Customer", "CUSTOMER", null);

    @Autowired
    private MockMvc mvc;

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
    void bookingSucceedsWhenTheNotificationServiceIsDown() throws Exception {
        mvc.perform(post("/api/appointments").with(user(CUSTOMER)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\": " + slotId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM appointments WHERE availability_slot_id = ? AND status = 'BOOKED'",
                Integer.class, slotId)).isEqualTo(1);
    }
}
