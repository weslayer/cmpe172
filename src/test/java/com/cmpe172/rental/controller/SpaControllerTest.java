package com.cmpe172.rental.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cmpe172.rental.config.SecurityConfig;
import com.cmpe172.rental.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpaController.class)
@Import(SecurityConfig.class)
class SpaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserRepository users;

    @Test
    void deepLinksServeTheReactAppWithoutSigningIn() throws Exception {
        for (String path : new String[] {"/slots", "/slots/3/book", "/me/appointments", "/provider"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        }
    }

    // Without loginPage("/login"), Spring Security's generated login form would answer here instead.
    @Test
    void loginRouteBelongsToTheReactApp() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }
}
