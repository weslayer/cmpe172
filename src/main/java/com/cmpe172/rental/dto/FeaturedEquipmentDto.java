package com.cmpe172.rental.dto;

import java.math.BigDecimal;

public record FeaturedEquipmentDto(
        long serviceId,
        String name,
        String category,
        String assetTag,
        BigDecimal dailyRate,
        String providerName) {
}
