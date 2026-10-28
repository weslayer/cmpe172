package com.cmpe172.rental.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withAccepted;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;

import com.cmpe172.rental.dto.NotificationRequest;
import java.net.ConnectException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// An unexpected extra request makes the mock server throw an AssertionError, which send() does not swallow.
class NotificationGatewayTest {

    private static final String URL = "http://notifier/mock/notifications";
    private static final String KEY = "appointment-9-BOOKING_CONFIRMED";
    private static final NotificationRequest NOTICE =
            new NotificationRequest("customer@example.com", "BOOKING_CONFIRMED", "Your rental is confirmed.", KEY);

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final NotificationGateway gateway = new NotificationGateway(builder, "http://notifier");

    @Test
    void postsTheNotificationAsJson() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.recipient").value("customer@example.com"))
                .andExpect(jsonPath("$.type").value("BOOKING_CONFIRMED"))
                .andExpect(jsonPath("$.idempotencyKey").value(KEY))
                .andRespond(withAccepted());

        gateway.send(NOTICE);
        server.verify();
    }

    @Test
    void retriesConnectionFailuresAndServerErrorsWithTheSameKey() {
        server.expect(requestTo(URL)).andExpect(jsonPath("$.idempotencyKey").value(KEY))
                .andRespond(withException(new ConnectException("Connection refused")));
        server.expect(requestTo(URL)).andExpect(jsonPath("$.idempotencyKey").value(KEY))
                .andRespond(withServiceUnavailable());
        server.expect(requestTo(URL)).andExpect(jsonPath("$.idempotencyKey").value(KEY))
                .andRespond(withAccepted());

        gateway.send(NOTICE);
        server.verify();
    }

    @Test
    void fallsBackWithoutThrowingWhenTheNotifierStaysDown() {
        server.expect(times(3), requestTo(URL)).andRespond(withServiceUnavailable());

        assertThatCode(() -> gateway.send(NOTICE)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void doesNotRetryARejectedNotification() {
        server.expect(once(), requestTo(URL)).andRespond(withBadRequest());

        assertThatCode(() -> gateway.send(NOTICE)).doesNotThrowAnyException();
        server.verify();
    }
}
