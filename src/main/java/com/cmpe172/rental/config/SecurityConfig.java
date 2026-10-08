package com.cmpe172.rental.config;

import com.cmpe172.rental.dto.UserDto;
import com.cmpe172.rental.model.AuthenticatedUser;
import com.cmpe172.rental.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/home", "/api/slots", "/api/services").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/appointments/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/provider/**").hasRole("PROVIDER")
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("email")
                        .successHandler((req, res, auth) -> {
                            loadCsrfToken(req); // login rotates the token; send the new cookie now
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            json.writeValue(res.getOutputStream(), UserDto.from((AuthenticatedUser) auth.getPrincipal()));
                        })
                        .failureHandler((req, res, ex) -> resolver.resolveException(req, res, null, ex))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> {
                            // logout clears the token; hand out a fresh one so the next sign-in can post
                            csrfTokens.saveToken(csrfTokens.generateToken(req), req, res);
                            res.setStatus(HttpStatus.NO_CONTENT.value());
                        }))
                // 401/403 go through the MVC resolver so GlobalExceptionHandler writes the usual error JSON.
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> resolver.resolveException(req, res, null, ex))
                        .accessDeniedHandler((req, res, ex) -> resolver.resolveException(req, res, null, ex)))
                // SPA-style CSRF: the browser reads the XSRF-TOKEN cookie and echoes it in X-XSRF-TOKEN.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter((req, res, chain) -> {
                    loadCsrfToken(req);
                    chain.doFilter(req, res);
                }, CsrfFilter.class);
        return http.build();
    }

    // The token is deferred; reading it is what makes the repository write the cookie.
    private static void loadCsrfToken(ServletRequest req) {
        ((CsrfToken) req.getAttribute(CsrfToken.class.getName())).getToken();
    }
}
