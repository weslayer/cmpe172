package com.cmpe172.rental.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmpe172.rental.dto.HomeSummaryDto;
import com.cmpe172.rental.service.HomeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeService service;

    @Test
    void returnsSummaryFromService() throws Exception {
        when(service.summary()).thenReturn(new HomeSummaryDto("Equipment Rental", 1, 6, 12, List.of()));

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("Equipment Rental"))
                .andExpect(jsonPath("$.providerCount").value(1))
                .andExpect(jsonPath("$.equipmentCount").value(6))
                .andExpect(jsonPath("$.availableSlotCount").value(12));
    }
}
