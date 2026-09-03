package com.cmpe172.rental.controller;

import com.cmpe172.rental.dto.HomeSummaryDto;
import com.cmpe172.rental.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final HomeService service;

    public HomeController(HomeService service) {
        this.service = service;
    }

    @GetMapping("/home")
    public HomeSummaryDto home() {
        return service.summary();
    }
}
