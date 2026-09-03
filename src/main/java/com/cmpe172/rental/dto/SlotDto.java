package com.cmpe172.rental.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SlotDto(
        long slotId,
        long serviceId,
        String serviceName,
        String category,
        String assetTag,
        long providerId,
        String providerName,
        BigDecimal dailyRate,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt) {
}
