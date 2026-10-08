package com.cmpe172.rental.model;

import java.time.OffsetDateTime;

// A slot as read under its row lock, with the owning provider resolved through the service.
public record Slot(long providerId, String status, OffsetDateTime startsAt) {
}
