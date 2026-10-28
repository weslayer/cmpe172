package com.cmpe172.rental.notification.mock;

import com.cmpe172.rental.dto.NotificationRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Loopback-only (see SecurityConfig). A repeated key is acknowledged but not delivered twice.
@RestController
@RequestMapping("/mock/notifications")
public class MockNotificationController {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationController.class);

    // In memory and unbounded: fine for a mock, lost on restart.
    private final Map<String, NotificationRequest> delivered = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody NotificationRequest notification) {
        if (delivered.putIfAbsent(notification.idempotencyKey(), notification) != null) {
            log.info("Duplicate notification {} ignored", notification.idempotencyKey());
            return ResponseEntity.ok().build();
        }
        log.info("Delivered {} notification {}", notification.type(), notification.idempotencyKey());
        return ResponseEntity.accepted().build();
    }
}
