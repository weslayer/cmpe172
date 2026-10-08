package com.cmpe172.rental.service;

import com.cmpe172.rental.dto.CreateSlotRequest;
import com.cmpe172.rental.dto.EquipmentDto;
import com.cmpe172.rental.dto.PageResponse;
import com.cmpe172.rental.dto.SlotDto;
import com.cmpe172.rental.model.Slot;
import com.cmpe172.rental.repository.AppointmentRepository;
import com.cmpe172.rental.repository.EquipmentRepository;
import com.cmpe172.rental.repository.SlotRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {

    // Feeds the filter dropdowns and the create-slot form, so it is capped rather than paged.
    private static final int MAX_EQUIPMENT = 100;

    private final SlotRepository repository;
    private final EquipmentRepository equipment;
    private final AppointmentRepository appointments;

    public SlotService(SlotRepository repository, EquipmentRepository equipment, AppointmentRepository appointments) {
        this.repository = repository;
        this.equipment = equipment;
        this.appointments = appointments;
    }

    public PageResponse<SlotDto> findAvailable(Long serviceId, Long providerId, LocalDate date, int page, int size) {
        List<SlotDto> content = repository.findAvailable(serviceId, providerId, date, page, size);
        long total = repository.countAvailable(serviceId, providerId, date);
        return new PageResponse<>(content, page, size, total);
    }

    public List<EquipmentDto> equipment(Long providerId) {
        return equipment.findActive(providerId, MAX_EQUIPMENT);
    }

    @Transactional
    public SlotDto createSlot(long providerId, CreateSlotRequest request) {
        long owner = equipment.findProviderId(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Equipment " + request.serviceId() + " not found"));
        if (owner != providerId) {
            throw new ForbiddenException("Equipment " + request.serviceId() + " belongs to another provider");
        }
        return repository.get(repository.insert(request.serviceId(), request.startsAt(), request.endsAt()));
    }

    // Takes the same row lock as booking, so a removal and a booking of one slot cannot interleave.
    @Transactional
    public void removeSlot(long providerId, long slotId) {
        Slot slot = repository.lock(slotId)
                .orElseThrow(() -> new NotFoundException("Slot " + slotId + " not found"));
        if (slot.providerId() != providerId) {
            throw new ForbiddenException("Slot " + slotId + " belongs to another provider");
        }
        if (appointments.existsBooked(slotId)) {
            throw new ConflictException("Slot " + slotId + " is booked and cannot be removed");
        }
        repository.block(slotId);
    }
}
