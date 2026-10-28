package com.cmpe172.rental.notification;

import com.cmpe172.rental.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

// The app's only way to reach the notification service. Timeouts come from spring.http.client.* in application.yml.
@Component
public class NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGateway.class);
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient http;

    public NotificationGateway(RestClient.Builder builder, @Value("${notification.base-url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    // Never throws: the caller has already committed, so a lost notification must not fail its request.
    public void send(NotificationRequest notification) {
        String key = notification.idempotencyKey();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                http.post().uri("/mock/notifications").body(notification).retrieve().toBodilessEntity();
                return;
            } catch (HttpClientErrorException ex) {
                log.error("Notification {} rejected with {}, not retrying", key, ex.getStatusCode());
                return;
            } catch (RuntimeException ex) {
                log.warn("Notification {} failed on attempt {}/{}: {}", key, attempt, MAX_ATTEMPTS,
                        NestedExceptionUtils.getMostSpecificCause(ex).toString());
            }
            if (attempt < MAX_ATTEMPTS && !pause(200L * attempt)) {
                break;
            }
        }
        log.error("Gave up on notification {}; the booking change still stands", key);
    }

    // False if interrupted: stop retrying and keep the interrupt for the caller.
    private static boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
