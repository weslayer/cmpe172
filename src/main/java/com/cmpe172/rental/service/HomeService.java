package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.HomeSummaryDto;
import com.cmpe172.rental.repository.SummaryRepository;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    private static final int FEATURED_LIMIT = 4;

    private final SummaryRepository repository;

    public HomeService(SummaryRepository repository) {
        this.repository = repository;
    }

    public HomeSummaryDto summary() {
        return new HomeSummaryDto(
                "Equipment Rental",
                repository.countProviders(),
                repository.countActiveEquipment(),
                repository.countAvailableSlots(),
                repository.findFeatured(FEATURED_LIMIT));
    }
}
