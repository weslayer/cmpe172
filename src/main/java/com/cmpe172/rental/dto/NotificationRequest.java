package com.cmpe172.rental.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// One coarse-grained message across the notification boundary; the key makes retries safe to repeat.
public record NotificationRequest(
        @NotBlank @Email String recipient,
        @NotBlank String type,
        @NotBlank String message,
        @NotBlank String idempotencyKey) {
}
