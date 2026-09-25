package com.cmpe172.rental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Boot auto-configures the DispatcherServlet, the front controller for every request.
@SpringBootApplication
public class EquipmentRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentRentalApplication.class, args);
    }
}
