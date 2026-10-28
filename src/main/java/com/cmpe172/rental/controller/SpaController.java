package com.cmpe172.rental.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Deep links into the React app (bundled into the Docker image) get index.html; React Router renders the page.
@Controller
public class SpaController {

    @GetMapping({"/slots", "/slots/{slotId}/book", "/login", "/me/appointments", "/provider"})
    public String index() {
        return "forward:/index.html";
    }
}
