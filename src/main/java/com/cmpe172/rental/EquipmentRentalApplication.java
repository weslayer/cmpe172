package com.cmpe172.rental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the equipment-rental booking system.
 *
 * <p>Spring Boot's embedded {@code DispatcherServlet} acts as the Front Controller:
 * every HTTP request first hits the DispatcherServlet, which routes it to the
 * matching {@code @RestController} handler.
 */
@SpringBootApplication
public class EquipmentRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentRentalApplication.class, args);
    }
}
