package com.cmpe172.rental.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Real Postgres: logs in with the seed accounts from seed.sql (password "password").
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void loginWithTheCsrfCookieStartsASession() throws Exception {
        MvcResult login = mvc.perform(login("provider@example.com", "password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROVIDER"))
                .andExpect(jsonPath("$.providerId").value(1))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("provider@example.com"));
    }

    @Test
    void logoutHandsOutAFreshCsrfCookieSoTheNextLoginWorks() throws Exception {
        MvcResult login = mvc.perform(login("customer@example.com", "password")).andExpect(status().isOk()).andReturn();
        Cookie rotated = lastXsrfCookie(login.getResponse());

        MockHttpServletResponse logout = mvc.perform(post("/api/auth/logout")
                        .session((MockHttpSession) login.getRequest().getSession(false))
                        .cookie(rotated)
                        .header("X-XSRF-TOKEN", rotated.getValue()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        Cookie fresh = lastXsrfCookie(logout);
        assertThat(fresh.getValue()).isNotEmpty();
        mvc.perform(post("/api/auth/login").cookie(fresh).header("X-XSRF-TOKEN", fresh.getValue())
                        .param("email", "customer@example.com").param("password", "password"))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/login").param("email", "customer@example.com").param("password", "password"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(login("customer@example.com", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void meRequiresLogin() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // Same flow as the SPA: any GET hands out the XSRF-TOKEN cookie, which is echoed back as a header.
    private MockHttpServletRequestBuilder login(String email, String password) throws Exception {
        Cookie xsrf = mvc.perform(get("/api/home")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        return post("/api/auth/login")
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .param("email", email)
                .param("password", password);
    }

    // A response can clear the old token and set a new one; like a browser, keep the last cookie.
    private static Cookie lastXsrfCookie(MockHttpServletResponse response) {
        return Arrays.stream(response.getCookies()).filter(c -> c.getName().equals("XSRF-TOKEN"))
                .reduce((first, second) -> second).orElseThrow();
    }
}
