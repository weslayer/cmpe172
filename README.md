# Equipment Rental — CMPE 172 Term Project

A layered Spring Boot application for renting equipment. Providers list rentable
items and availability; customers book time slots. Double-booking is prevented by
a row lock in the booking transaction, with a database constraint as the backstop.

Java + Spring Boot, **SQL over JDBC (no ORM)**, PostgreSQL, Liquibase migrations,
Spring Security session login, and a React single-page frontend. Booking and
cancelling send a confirmation through a mock notification service, the app
exposes a health check and Prometheus metrics, and the whole stack runs in Docker.

## API

Milestone 1 added the two public read endpoints; Milestone 2 added login, booking,
and the provider features; Milestone 3 added the notification boundary, health,
and metrics.

| Endpoint | Who | Returns |
| --- | --- | --- |
| `GET /api/home` | anyone | Summary counts and featured equipment |
| `GET /api/slots` | anyone | Open future slots, filterable and paginated |
| `GET /api/services` | anyone | Active equipment (optional `providerId`) |
| `POST /api/auth/login` | anyone | Form fields `email`, `password`; starts a session |
| `POST /api/auth/logout` | signed in | Ends the session (204) |
| `GET /api/auth/me` | signed in | The current user |
| `POST /api/appointments` | customer | Books `{"slotId": n}` (201, or 409 if taken) |
| `GET /api/appointments` | customer | My appointments: BOOKED, CANCELLED, COMPLETED |
| `POST /api/appointments/{id}/cancel` | customer (owner) | Cancels an upcoming booking |
| `POST /api/provider/slots` | provider | Creates a slot for one of my items |
| `DELETE /api/provider/slots/{id}` | provider (owner) | Removes an unbooked slot (204) |
| `GET /api/provider/appointments` | provider | Bookings on my equipment |
| `GET /health` | anyone | `UP` with component statuses, including the database; 503 `DOWN` when it is unreachable |
| `GET /metrics` | admin | All meters in Prometheus text format, including `rental_bookings_seconds{outcome}` |
| `POST /mock/notifications` | the app itself (loopback) | Mock notification service: 202 new, 200 repeated key |

`GET /api/slots` supports `serviceId`, `providerId`, `date` (YYYY-MM-DD),
`page` (default 0), and `size` (default 20, max 100). Pagination uses SQL
`LIMIT`/`OFFSET`.

Errors come back as `{"status", "error", "message"}`: 400 for invalid input,
401 when not signed in, 403 for the wrong role or someone else's record, 404 for
a missing record, and 409 for a slot conflict. Stack traces are never returned.

Every state-changing request needs the `XSRF-TOKEN` cookie value echoed in an
`X-XSRF-TOKEN` header. Any API response sets that cookie; the frontend handles it.

After a booking or cancellation commits, the app sends the customer a
confirmation through `NotificationGateway` (1 s timeouts, three attempts, then a
logged fallback). The booking succeeds even when the notification service is
down. See [docs/architecture.md](docs/architecture.md#distribution-boundary) and
[docs/operations.md](docs/operations.md).

## Seed accounts

All seed accounts use the password `password`.

| Email | Role |
| --- | --- |
| `customer@example.com` | Customer (one upcoming and one completed rental) |
| `customer2@example.com` | Customer |
| `provider@example.com` | Provider (owns all six items) |
| `admin@example.com` | Admin (can read `/metrics`) |

## Prerequisites

- Docker (for PostgreSQL, or to run everything)
- For local development: JDK 22, Maven 3.9+, and Node.js 20.19+ or 22.12+ (for the frontend)

## Run everything in Docker

```bash
docker compose up --build
```

This builds the app image (the React SPA is bundled into the Spring Boot jar)
and starts it with PostgreSQL. Open <http://localhost:8080>. Logs come out as
JSON (`docker compose logs app`), and `docker compose ps` shows the app's health.
Stop any locally running app first, since both use port 8080.

## Run it locally

1. Start PostgreSQL:

   ```bash
   docker compose up -d db
   ```

2. Start the application (Liquibase builds the schema and seed data on first run):

   ```bash
   mvn spring-boot:run
   ```

3. In a second terminal, start the frontend and open <http://localhost:5173>:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   The Vite dev server forwards `/api` to the backend on port 8080, so the
   browser sees one origin and the session cookie just works.

The public endpoints can also be called directly:

```bash
curl "http://localhost:8080/api/home"
curl "http://localhost:8080/api/slots?page=0&size=3"
curl "http://localhost:8080/api/slots?serviceId=1&date=2027-09-06"
curl "http://localhost:8080/health"
```

If a database volume was created from an older version of the schema, Liquibase
refuses to start because the changeset checksums differ. The volume only holds
seed data, so recreate it:

```bash
docker compose down -v
```

## Tests

```bash
mvn test
```

Controller and service tests run without a database. Repository, login, and
concurrency tests run against Postgres (start `db` first) and assert against the
seed data. `BookingConcurrencyTest` starts two threads that book the same slot at
the same moment and checks that exactly one succeeds.
`NotificationDownIntegrationTest` points the gateway at a port where nothing
listens and checks that a booking still returns 201 and is saved. CI runs the
same suite against a Postgres service container on every push, then builds the
Docker image.

## Configuration

The datasource is read from environment variables, with local-dev defaults that
match `docker-compose.yml`. Export these to override (do not commit real
credentials); `.env.example` lists them.

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/rental` |
| `DB_USERNAME` | `rental` |
| `DB_PASSWORD` | `rental` |
| `NOTIFICATION_URL` | `http://localhost:8080` (the mock inside this app) |

The defaults are non-secret local values. For anything beyond local development,
set real values in the shell environment before running.

## Project structure

```
src/main/java/com/cmpe172/rental/
├── controller/   REST endpoints (routed by the DispatcherServlet front controller), SPA deep links
├── service/      business rules and transactions (booking, cancel, slots), commit-then-notify
├── repository/   parameterized SQL over JDBC (NamedParameterJdbcTemplate)
├── dto/          API request and response records
├── model/        session principal and rows read under a lock
├── config/       security and global exception handling
└── notification/ NotificationGateway (timeout, retry, fallback)
    └── mock/     the mock notification service (POST /mock/notifications)

src/main/resources/db/changelog/
├── db.changelog-master.yaml   Liquibase master (the only DB initializer)
├── schema.sql                 tables, constraints, double-booking guard
├── seed.sql                   sample data
├── booking-status.sql         M2: BOOKED / CANCELLED / COMPLETED statuses
└── seed-m2.sql                M2: future slots, second customer, real password hashes

src/test/java/com/cmpe172/rental/   controller, service, repository, notification, and concurrency tests

Dockerfile                    SPA build → jar build → JRE runtime (non-root)
docker-compose.yml            PostgreSQL, plus the app with a /health healthcheck

frontend/                     React + Vite single-page app
├── src/api.js                fetch wrapper (session cookie, CSRF header, error JSON)
├── src/App.jsx               routes and navigation
└── src/pages/                Home, Slots, Book, Login, MyAppointments, Provider
```

## Database

Liquibase is the only database initializer (Spring's SQL init is disabled).
Changesets are tracked in `DATABASECHANGELOG` and run once. The double-booking
backstop is a partial unique index on `appointments`; see [docs/schema.md](docs/schema.md)
and [docs/concurrency.md](docs/concurrency.md).

## Documentation

- [Proposal](docs/proposal.md)
- [Architecture and distribution boundary](docs/architecture.md)
- [ER diagram](docs/er-diagram.md)
- [Relational schema](docs/schema.md)
- [Request flow & Page vs Front Controller](docs/request-flow.md)
- [Concurrency and transactions](docs/concurrency.md)
- [Operations: logging, health, metrics, failure recovery](docs/operations.md)
- Wireframes — `docs/wireframes.html`

## Milestone 1 deliverables

Rendered submission files live in [`deliverables/milestone1/`](deliverables/milestone1/):
the report PDF, the ER diagram (PNG and SVG), and the wireframes (PDF and one PNG
per screen). The report source is `deliverables/milestone1/report.html`. To
regenerate the PDF, open that file in a browser and print it to PDF.

## Milestone 2 deliverables

The report lives in [`deliverables/milestone2/`](deliverables/milestone2/) as
`CMPE172_Milestone2_Report.docx` and the matching PDF. The concurrency design it
describes is also in [docs/concurrency.md](docs/concurrency.md).

## Milestone 3 deliverables

The report and the distribution diagram live in
[`deliverables/milestone3/`](deliverables/milestone3/) as
`CMPE172_Milestone3_Report.docx`, the matching PDF, and
`distribution-diagram.png`. The designs it describes are also in
[docs/architecture.md](docs/architecture.md#distribution-boundary) and
[docs/operations.md](docs/operations.md).
