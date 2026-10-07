package com.cmpe172.rental.model;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

// Session principal; the role and provider id come straight from the user's row.
public class AuthenticatedUser extends User {

    private final long id;
    private final String fullName;
    private final String role;
    private final Long providerId;

    public AuthenticatedUser(long id, String email, String passwordHash, String fullName, String role, Long providerId) {
        super(email, passwordHash, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.id = id;
        this.fullName = fullName;
        this.role = role;
        this.providerId = providerId;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public Long getProviderId() {
        return providerId;
    }
}
