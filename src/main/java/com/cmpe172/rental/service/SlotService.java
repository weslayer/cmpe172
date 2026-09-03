package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.PageResponse;
import com.cmpe172.rental.dto.SlotDto;
import com.cmpe172.rental.repository.SlotRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SlotService {

    private final SlotRepository repository;

    public SlotService(SlotRepository repository) {
        this.repository = repository;
    }

    public PageResponse<SlotDto> findAvailable(Long serviceId, Long providerId, LocalDate date, int page, int size) {
        List<SlotDto> content = repository.findAvailable(serviceId, providerId, date, page, size);
        long total = repository.countAvailable(serviceId, providerId, date);
        return new PageResponse<>(content, page, size, total);
    }
}
