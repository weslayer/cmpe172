package com.cmpe172.rental.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmpe172.rental.config.SecurityConfig;
import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.model.AuthenticatedUser;
import com.cmpe172.rental.repository.UserRepository;
import com.cmpe172.rental.service.BookingService;
import com.cmpe172.rental.service.ConflictException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AppointmentController.class)
@Import(SecurityConfig.class)
class AppointmentControllerTest {

    private static final AuthenticatedUser CUSTOMER =
            new AuthenticatedUser(1, "customer@example.com", "x", "Casey Customer", "CUSTOMER", null);
    private static final AuthenticatedUser PROVIDER =
            new AuthenticatedUser(2, "provider@example.com", "x", "Pat Provider", "PROVIDER", 1L);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService service;

    @MockitoBean
    private UserRepository users;

    @Test
    void customerBooksASlot() throws Exception {
        when(service.book(1, 2)).thenReturn(new AppointmentDto(9, "BOOKED", 2, OffsetDateTime.now(),
                OffsetDateTime.now(), "Drill", "PT-DRILL-001", "Bay Area", "Casey Customer", "customer@example.com"));

        mockMvc.perform(book("{\"slotId\": 2}").with(user(CUSTOMER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void anonymousBookingIsUnauthorized() throws Exception {
        mockMvc.perform(book("{\"slotId\": 2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void providerCannotBook() throws Exception {
        mockMvc.perform(book("{\"slotId\": 2}").with(user(PROVIDER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void missingSlotIdIsBadRequest() throws Exception {
        mockMvc.perform(book("{}").with(user(CUSTOMER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("slotId: must not be null"));
    }

    @Test
    void takenSlotIsAConflict() throws Exception {
        when(service.book(1, 2)).thenThrow(new ConflictException("Slot 2 is already booked"));

        mockMvc.perform(book("{\"slotId\": 2}").with(user(CUSTOMER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Slot 2 is already booked"));
    }

    private static MockHttpServletRequestBuilder book(String body) {
        return post("/api/appointments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
