package com.cmpe172.rental.controller;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.dto.CreateSlotRequest;
import com.cmpe172.rental.dto.SlotDto;
import com.cmpe172.rental.model.AuthenticatedUser;
import com.cmpe172.rental.service.BookingService;
import com.cmpe172.rental.service.SlotService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Provider-only (see SecurityConfig).
@RestController
@RequestMapping("/api/provider")
public class ProviderController {

    private final SlotService slots;
    private final BookingService bookings;

    public ProviderController(SlotService slots, BookingService bookings) {
        this.slots = slots;
        this.bookings = bookings;
    }

    @PostMapping("/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotDto createSlot(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateSlotRequest request) {
        return slots.createSlot(user.getProviderId(), request);
    }

    @DeleteMapping("/slots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSlot(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        slots.removeSlot(user.getProviderId(), id);
    }

    @GetMapping("/appointments")
    public List<AppointmentDto> appointments(@AuthenticationPrincipal AuthenticatedUser user) {
        return bookings.providerAppointments(user.getProviderId());
    }
}
