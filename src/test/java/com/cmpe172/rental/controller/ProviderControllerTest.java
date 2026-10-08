package com.cmpe172.rental.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmpe172.rental.config.SecurityConfig;
import com.cmpe172.rental.dto.CreateSlotRequest;
import com.cmpe172.rental.dto.SlotDto;
import com.cmpe172.rental.model.AuthenticatedUser;
import com.cmpe172.rental.repository.UserRepository;
import com.cmpe172.rental.service.BookingService;
import com.cmpe172.rental.service.SlotService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ProviderController.class)
@Import(SecurityConfig.class)
class ProviderControllerTest {

    private static final AuthenticatedUser PROVIDER =
            new AuthenticatedUser(2, "provider@example.com", "x", "Pat Provider", "PROVIDER", 1L);
    private static final AuthenticatedUser CUSTOMER =
            new AuthenticatedUser(1, "customer@example.com", "x", "Casey Customer", "CUSTOMER", null);
    private static final String START = OffsetDateTime.now().plusDays(1).withNano(0).toString();
    private static final String END = OffsetDateTime.now().plusDays(1).plusHours(8).withNano(0).toString();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlotService slots;

    @MockitoBean
    private BookingService bookings;

    @MockitoBean
    private UserRepository users;

    @Test
    void providerCreatesASlot() throws Exception {
        when(slots.createSlot(eq(1L), any(CreateSlotRequest.class))).thenReturn(new SlotDto(20, 3, "Ladder", "Ladders",
                "LAD-EXT-003", 1, "Bay Area", new BigDecimal("12.50"), OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(createSlot(START, END).with(user(PROVIDER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotId").value(20));
    }

    @Test
    void customerCannotCreateSlots() throws Exception {
        mockMvc.perform(createSlot(START, END).with(user(CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void endBeforeStartIsBadRequest() throws Exception {
        mockMvc.perform(createSlot(END, START).with(user(PROVIDER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endAfterStart: endsAt must be after startsAt"));
    }

    @Test
    void startInThePastIsBadRequest() throws Exception {
        mockMvc.perform(createSlot("2020-01-01T09:00:00Z", "2020-01-01T17:00:00Z").with(user(PROVIDER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startsAt: must be a future date"));
    }

    @Test
    void providerRemovesASlot() throws Exception {
        mockMvc.perform(delete("/api/provider/slots/5").with(csrf()).with(user(PROVIDER)))
                .andExpect(status().isNoContent());

        verify(slots).removeSlot(1, 5);
    }

    private static MockHttpServletRequestBuilder createSlot(String startsAt, String endsAt) {
        return post("/api/provider/slots").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceId\": 3, \"startsAt\": \"" + startsAt + "\", \"endsAt\": \"" + endsAt + "\"}");
    }
}
