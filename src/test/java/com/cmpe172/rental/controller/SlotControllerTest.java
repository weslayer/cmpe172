package com.cmpe172.rental.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmpe172.rental.dto.PageResponse;
import com.cmpe172.rental.dto.SlotDto;
import com.cmpe172.rental.service.SlotService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SlotController.class)
class SlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlotService service;

    @Test
    void returnsPageFromService() throws Exception {
        when(service.findAvailable(isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(new PageResponse<SlotDto>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsSizeAboveMax() throws Exception {
        mockMvc.perform(get("/api/slots").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/slots").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedDate() throws Exception {
        mockMvc.perform(get("/api/slots").param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void passesFiltersThroughToTheService() throws Exception {
        when(service.findAvailable(eq(1L), eq(2L), any(), eq(0), eq(20)))
                .thenReturn(new PageResponse<SlotDto>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/slots")
                        .param("serviceId", "1")
                        .param("providerId", "2")
                        .param("date", "2026-09-06"))
                .andExpect(status().isOk());
    }
}
