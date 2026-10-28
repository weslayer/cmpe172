package com.cmpe172.rental.notification.mock;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmpe172.rental.config.SecurityConfig;
import com.cmpe172.rental.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// No csrf() on any request: the mock is a server-to-server endpoint. Each test uses its own key (shared context).
@WebMvcTest(MockNotificationController.class)
@Import(SecurityConfig.class)
class MockNotificationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserRepository users;

    @Test
    void acceptsANewNotificationAndAcknowledgesARepeat() throws Exception {
        String body = notification("customer@example.com", "appointment-1-BOOKING_CONFIRMED");

        mvc.perform(send(body)).andExpect(status().isAccepted());
        mvc.perform(send(body)).andExpect(status().isOk());
    }

    @Test
    void rejectsANotificationWithoutARecipient() throws Exception {
        mvc.perform(send(notification("", "appointment-2-BOOKING_CONFIRMED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("recipient")));
    }

    @Test
    void onlyTheAppItselfMayCall() throws Exception {
        mvc.perform(send(notification("customer@example.com", "appointment-3-BOOKING_CONFIRMED"))
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.7");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    private static MockHttpServletRequestBuilder send(String body) {
        return post("/mock/notifications").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String notification(String recipient, String key) {
        return """
                {"recipient": "%s", "type": "BOOKING_CONFIRMED", "message": "Your rental is confirmed.", "idempotencyKey": "%s"}
                """.formatted(recipient, key);
    }
}
