# Equipment Rental — CMPE 172 Term Project

A layered Spring Boot application for renting equipment. Providers list rentable
items and availability; customers book time slots. Double-booking is prevented by
a row lock in the booking transaction, with a database constraint as the backstop.

Java + Spring Boot, **SQL over JDBC (no ORM)**, PostgreSQL, Liquibase migrations,
Spring Security session login, and a React single-page frontend.

## API

Milestone 1 added the two public read endpoints; Milestone 2 added login, booking,
and the provider features.

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

`GET /api/slots` supports `serviceId`, `providerId`, `date` (YYYY-MM-DD),
`page` (default 0), and `size` (default 20, max 100). Pagination uses SQL
`LIMIT`/`OFFSET`.

Errors come back as `{"status", "error", "message"}`: 400 for invalid input,
401 when not signed in, 403 for the wrong role or someone else's record, 404 for
a missing record, and 409 for a slot conflict. Stack traces are never returned.

Every state-changing request needs the `XSRF-TOKEN` cookie value echoed in an
`X-XSRF-TOKEN` header. Any API response sets that cookie; the frontend handles it.

## Seed accounts

All seed accounts use the password `password`.

| Email | Role |
| --- | --- |
| `customer@example.com` | Customer (one upcoming and one completed rental) |
| `customer2@example.com` | Customer |
| `provider@example.com` | Provider (owns all six items) |
| `admin@example.com` | Admin (no admin features yet) |

## Prerequisites

- JDK 22
- Maven 3.9+
- Node.js 20.19+ (for the frontend)
- Docker (for local PostgreSQL)

## Run it

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
the same moment and checks that exactly one succeeds. CI runs the same suite
against a Postgres service container on every push and pull request, then builds
the frontend.

## Configuration

The datasource is read from environment variables, with local-dev defaults that
match `docker-compose.yml`. Export these to override (do not commit real
credentials); `.env.example` lists them.

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/rental` |
| `DB_USERNAME` | `rental` |
| `DB_PASSWORD` | `rental` |

The defaults are non-secret local values. For anything beyond local development,
set real values in the shell environment before running.

## Project structure

```
src/main/java/com/cmpe172/rental/
├── controller/   REST endpoints (routed by the DispatcherServlet front controller)
├── service/      business rules and transactions (booking, cancel, slots)
├── repository/   parameterized SQL over JDBC (NamedParameterJdbcTemplate)
├── dto/          API request and response records
├── model/        session principal and rows read under a lock
├── config/       security and global exception handling
└── notification/ notification boundary (populated in M3)

src/main/resources/db/changelog/
├── db.changelog-master.yaml   Liquibase master (the only DB initializer)
├── schema.sql                 tables, constraints, double-booking guard
├── seed.sql                   sample data
├── booking-status.sql         M2: BOOKED / CANCELLED / COMPLETED statuses
└── seed-m2.sql                M2: future slots, second customer, real password hashes

src/test/java/com/cmpe172/rental/   controller, service, repository, and concurrency tests

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
- [Architecture](docs/architecture.md)
- [ER diagram](docs/er-diagram.md)
- [Relational schema](docs/schema.md)
- [Request flow & Page vs Front Controller](docs/request-flow.md)
- [Concurrency and transactions](docs/concurrency.md)
- Wireframes — `docs/wireframes.html`

## Milestone 1 deliverables

Rendered submission files live in [`deliverables/milestone1/`](deliverables/milestone1/):
the report PDF, the ER diagram (PNG and SVG), and the wireframes (PDF and one PNG
per screen). The report source is `deliverables/milestone1/report.html`. To
regenerate the PDF, open that file in a browser and print it to PDF.
