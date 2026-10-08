package com.cmpe172.rental.model;

import java.time.OffsetDateTime;

// An appointment as read under its row lock, with its slot's start time.
public record Appointment(long customerId, String status, OffsetDateTime startsAt) {
}
