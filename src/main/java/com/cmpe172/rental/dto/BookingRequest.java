package com.cmpe172.rental.dto;

import jakarta.validation.constraints.NotNull;

public record BookingRequest(@NotNull Long slotId) {
}
