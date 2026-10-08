package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.HomeSummaryDto;
import com.cmpe172.rental.repository.EquipmentRepository;
import com.cmpe172.rental.repository.SlotRepository;
import com.cmpe172.rental.repository.SummaryRepository;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    private static final int FEATURED_LIMIT = 4;

    private final SummaryRepository summaryRepository;
    private final SlotRepository slotRepository;
    private final EquipmentRepository equipmentRepository;

    public HomeService(SummaryRepository summaryRepository, SlotRepository slotRepository,
            EquipmentRepository equipmentRepository) {
        this.summaryRepository = summaryRepository;
        this.slotRepository = slotRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public HomeSummaryDto summary() {
        return new HomeSummaryDto(
                "Equipment Rental",
                summaryRepository.countProviders(),
                summaryRepository.countActiveEquipment(),
                slotRepository.countAvailable(null, null, null),
                equipmentRepository.findActive(null, FEATURED_LIMIT));
    }
}
