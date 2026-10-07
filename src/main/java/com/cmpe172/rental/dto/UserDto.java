package com.cmpe172.rental.dto;

import com.cmpe172.rental.model.AuthenticatedUser;

public record UserDto(
        long id,
        String email,
        String fullName,
        String role,
        Long providerId) {

    public static UserDto from(AuthenticatedUser user) {
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getRole(), user.getProviderId());
    }
}
