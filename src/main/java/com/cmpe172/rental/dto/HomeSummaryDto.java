package com.cmpe172.rental.dto;

import java.util.List;

public record HomeSummaryDto(
        String application,
        long providerCount,
        long equipmentCount,
        long availableSlotCount,
        List<FeaturedEquipmentDto> featuredEquipment) {
}
