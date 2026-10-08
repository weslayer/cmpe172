package com.cmpe172.rental.model;

import java.time.OffsetDateTime;

// A slot as read under its row lock.
public record Slot(String status, OffsetDateTime startsAt) {
}
