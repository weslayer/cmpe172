package com.cmpe172.rental.dto;

import java.time.OffsetDateTime;

public record AppointmentDto(
        long appointmentId,
        String status,
        long slotId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String serviceName,
        String assetTag,
        String providerName,
        String customerName,
        String customerEmail) {
}
