# Operations: logging, health, metrics, and failure recovery

## Logging plan

The application logs through SLF4J with Spring Boot's defaults. It logs ids
(appointment, slot, customer, idempotency key), never emails, passwords, or
message bodies.

| Level | When | Logger |
| --- | --- | --- |
| INFO | A booking is made or cancelled | `BookingService` |
| INFO | A booking is refused because the slot is taken or closed (a normal outcome, not a fault) | `BookingService` |
| INFO | The mock service delivers a notification, or ignores a repeated key | `MockNotificationController` |
| WARN | A booking attempt timed out waiting for the slot's row lock and is being retried | `BookingService` |
| WARN | A notification attempt failed and will be retried | `NotificationGateway` |
| ERROR | A booking gave up after three lock timeouts (the client gets 503) | `BookingService` |
| ERROR | A notification failed all three attempts, or was rejected with a 4xx | `NotificationGateway` |

Examples captured from the running application. Booking and cancelling, with the
mock delivering both notifications (the `[traceId-spanId]` block is explained
below):

```
INFO  [c3b92519009c02c20eb8f09400f5bf72-cdc55476151f4dde] c.cmpe172.rental.service.BookingService  : Booked appointment 25 on slot 35 for customer 1
INFO  [c3b92519009c02c20eb8f09400f5bf72-570312626a7181f9] c.c.r.n.mock.MockNotificationController  : Delivered BOOKING_CONFIRMED notification appointment-25-BOOKING_CONFIRMED
INFO  [3acda784a29bc1dfc5133920e8dc67cc-28369f51f2a7e27c] c.cmpe172.rental.service.BookingService  : Cancelled appointment 25 for customer 1
INFO  [3acda784a29bc1dfc5133920e8dc67cc-d90bc0c40ceb9901] c.c.r.n.mock.MockNotificationController  : Delivered BOOKING_CANCELLED notification appointment-25-BOOKING_CANCELLED
```

A refused booking:

```
INFO  c.cmpe172.rental.service.BookingService  : Slot 1 not booked for customer 1: Slot 1 is already booked
```

The notification service is down. The booking still commits and the client
gets 201:

```
INFO  [5658c321e3bf159be64d4b9cd318445c-feda5037210fa0c2] c.cmpe172.rental.service.BookingService  : Booked appointment 46 on slot 55 for customer 1
WARN  [5658c321e3bf159be64d4b9cd318445c-feda5037210fa0c2] c.c.r.notification.NotificationGateway   : Notification appointment-46-BOOKING_CONFIRMED failed on attempt 1/3: java.nio.channels.ClosedChannelException
WARN  [5658c321e3bf159be64d4b9cd318445c-feda5037210fa0c2] c.c.r.notification.NotificationGateway   : Notification appointment-46-BOOKING_CONFIRMED failed on attempt 2/3: java.nio.channels.ClosedChannelException
WARN  [5658c321e3bf159be64d4b9cd318445c-feda5037210fa0c2] c.c.r.notification.NotificationGateway   : Notification appointment-46-BOOKING_CONFIRMED failed on attempt 3/3: java.nio.channels.ClosedChannelException
ERROR [5658c321e3bf159be64d4b9cd318445c-feda5037210fa0c2] c.c.r.notification.NotificationGateway   : Gave up on notification appointment-46-BOOKING_CONFIRMED; the booking change still stands
```

The lock-retry messages follow the same pattern:
`Lock wait on slot 8 timed out (attempt 1/3), retrying` (WARN) and
`Gave up booking slot 8 for customer 1 after 3 lock timeouts` (ERROR).

### Trace IDs

Every line carries `[traceId-spanId]`. The trace ID is created when a request
arrives and is passed to the notification service in a W3C `traceparent`
header, so the mock's "Delivered" line has the same trace ID as the booking that
caused it, with a different span ID. To follow one booking across the boundary,
search the logs for its trace ID.

### Format

Run locally (`mvn spring-boot:run`), the logs are the readable lines above. In
the Docker container, `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` switches Spring
Boot to Elastic Common Schema JSON, one object per line, which log tools can
index without parsing:

```json
{"@timestamp":"2026-10-28T04:52:36.095134359Z","log.level":"INFO","process.thread.name":"http-nio-8080-exec-7","service.name":"equipment-rental","log.logger":"com.cmpe172.rental.service.BookingService","message":"Booked appointment 47 on slot 56 for customer 1","traceId":"207f20c20bf9725cbdae9b6b7d4e4dc5","spanId":"e32928a552c7863f","ecs.version":"8.11"}
{"@timestamp":"2026-10-28T04:52:36.162744263Z","log.level":"INFO","process.thread.name":"http-nio-8080-exec-8","service.name":"equipment-rental","log.logger":"com.cmpe172.rental.notification.mock.MockNotificationController","message":"Delivered BOOKING_CONFIRMED notification appointment-47-BOOKING_CONFIRMED","traceId":"207f20c20bf9725cbdae9b6b7d4e4dc5","spanId":"2345e0d871414125","ecs.version":"8.11"}
```

## Health check

`GET /health` is public and comes from Spring Boot Actuator. The `db`
component borrows a connection from the pool and checks that it is valid, so it
fails when PostgreSQL is unreachable. The response lists each component's status
but no details:

```json
{"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},"ping":{"status":"UP"},"ssl":{"status":"UP"}}}
```

When the database is down the endpoint answers **503** with:

```json
{"status":"DOWN","components":{"db":{"status":"DOWN"},"diskSpace":{"status":"UP"},"ping":{"status":"UP"},"ssl":{"status":"UP"}}}
```

The connection pool waits at most 5 s for a connection
(`spring.datasource.hikari.connection-timeout`), so a dead database shows up as
DOWN in about 5 s rather than 30. Docker Compose calls this endpoint every 10 s,
so `docker compose ps` shows the app as `healthy` or `unhealthy`.

A down notification service does not make `/health` DOWN. The app still books
and cancels correctly without it; only the confirmations are lost.

## Metrics

`GET /metrics` serves every meter in Prometheus text format. It requires the
ADMIN role (sign in as `admin@example.com`). The application's own metric is
`rental.bookings`, a timer recorded around each booking request (lock retries
included, the notification excluded), tagged by `outcome`:

| Outcome | Meaning |
| --- | --- |
| `booked` | The booking was made |
| `conflict` | The slot was taken or no longer open (409) |
| `failed` | Anything else: unknown slot (404), lock retries exhausted (503), an unexpected error |

A sample after one booking and one conflict:

```
# HELP rental_bookings_seconds Booking requests by outcome
# TYPE rental_bookings_seconds summary
rental_bookings_seconds_count{outcome="booked"} 1
rental_bookings_seconds_sum{outcome="booked"} 0.008878627
rental_bookings_seconds_count{outcome="conflict"} 1
rental_bookings_seconds_sum{outcome="conflict"} 0.016025371
# HELP rental_bookings_seconds_max Booking requests by outcome
# TYPE rental_bookings_seconds_max gauge
rental_bookings_seconds_max{outcome="booked"} 0.008878627
rental_bookings_seconds_max{outcome="conflict"} 0.016025371
```

From that one meter:

- **Bookings per hour:** the change in `rental_bookings_seconds_count{outcome="booked"}`
  over an hour (in PromQL, `increase(...[1h])`).
- **Failed bookings:** `count` for `conflict` plus `failed`.
- **Average booking latency:** `sum / count` for an outcome; `max` gives the
  slowest recent request.

The endpoint is ready for a Prometheus server to scrape; none is part of this
project. Spring Boot also publishes JVM, connection-pool, and per-endpoint HTTP
meters (`http_server_requests_seconds`) on the same page.

## Failure scenario: the database goes down

**What happens.** PostgreSQL stops while the app is running. Measured against
the Docker stack with `docker compose stop db`:

- `/health` answers 503 `DOWN` after 5.0 s, and the `db` component shows DOWN.
- API requests that need the database fail after the same 5 s with a 500 from
  Spring Boot's error handler. No stack trace is returned.
- Boot logs `WARN DataSource health check failed`, and Compose marks the
  container `unhealthy` after its retries.

**Recovery.** No restart is needed. After `docker compose start db`, the
connection pool opens new connections on the next request; `/health` returned UP
about 1 s after Postgres was back, and Compose marked the app `healthy` again.
Committed bookings are safe throughout, because a booking either commits as a
whole or not at all.

## Degradation: the notification service is down or slow

The booking or cancellation commits first, then the gateway tries the
notification three times and gives up with the ERROR line shown above. Measured
against the Docker image:

| Notifier | Book response | Added time |
| --- | --- | --- |
| Up | 201 | negligible |
| Down (connection refused) | 201 | about 0.8 s |
| Hangs (never answers in time) | 201 | about 3.75 s (three 1 s read timeouts plus backoff) |

The lost notification is visible in the logs, with its idempotency key, but it is
not re-sent. A transactional outbox would be the upgrade if every notification
must arrive (see [architecture.md](architecture.md#alternatives-considered)).

## Docker and CI

```bash
docker compose up --build
```

This builds the image (SPA, then jar, then JRE runtime as a non-root user) and
starts Postgres and the app; the site is at <http://localhost:8080>. GitHub
Actions runs on every push: it starts a Postgres service container, runs
`mvn test`, then builds the Docker image, so a broken Dockerfile fails the build
too.
