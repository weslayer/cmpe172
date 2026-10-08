package com.cmpe172.rental.controller;

import com.cmpe172.rental.dto.AppointmentDto;
import com.cmpe172.rental.dto.BookingRequest;
import com.cmpe172.rental.model.AuthenticatedUser;
import com.cmpe172.rental.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Customer-only (see SecurityConfig).
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final BookingService service;

    public AppointmentController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentDto book(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody BookingRequest request) {
        return service.book(user.getId(), request.slotId());
    }

    @GetMapping
    public List<AppointmentDto> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.customerAppointments(user.getId());
    }

    @PostMapping("/{id}/cancel")
    public AppointmentDto cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return service.cancel(user.getId(), id);
    }
}
