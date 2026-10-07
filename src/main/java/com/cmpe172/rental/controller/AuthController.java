package com.cmpe172.rental.controller;

import com.cmpe172.rental.dto.UserDto;
import com.cmpe172.rental.model.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login and logout are handled by the security filter chain (see SecurityConfig).
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthenticatedUser user) {
        return UserDto.from(user);
    }
}
