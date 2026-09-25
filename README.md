# Equipment Rental — CMPE 172 Term Project

A layered Spring Boot application for renting equipment. Providers list rentable
items and availability; customers book time slots. The database structurally
prevents double-booking.

Java + Spring Boot, **SQL over JDBC (no ORM)**, PostgreSQL, Liquibase migrations.

## Milestone 1 scope

The running skeleton with two read endpoints:

| Endpoint | Returns |
| --- | --- |
| `GET /api/home` | Application summary (counts) + featured equipment |
| `GET /api/slots` | Available slots as DTOs, filterable and paginated |

`GET /api/slots` supports `serviceId`, `providerId`, `date` (YYYY-MM-DD),
`page` (default 0), and `size` (default 20, max 100). Pagination uses SQL
`LIMIT`/`OFFSET`.

## Prerequisites

- JDK 22
- Maven 3.9+
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

3. Call the endpoints:

   ```bash
   curl "http://localhost:8080/api/home"
   curl "http://localhost:8080/api/slots?page=0&size=3"
   curl "http://localhost:8080/api/slots?serviceId=1&date=2026-09-06"
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

Controller and service tests run without a database. Repository tests run
against Postgres (start `db` first) and assert against the seed data. CI runs
the same suite against a Postgres service container on every push and pull
request.

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
├── service/      business logic, DTO assembly
├── repository/   parameterized SQL over JDBC (NamedParameterJdbcTemplate)
├── dto/          API response records
├── model/        domain types (populated in M2)
├── config/       exception handling and config
└── notification/ notification boundary (populated in M3)

src/main/resources/db/changelog/
├── db.changelog-master.yaml   Liquibase master (the only DB initializer)
├── schema.sql                 tables, constraints, double-booking guard
└── seed.sql                   sample data

src/test/java/com/cmpe172/rental/   controller, service, and repository tests
```

## Database

Liquibase is the only database initializer (Spring's SQL init is disabled).
Changesets are tracked in `DATABASECHANGELOG` and run once. The double-booking
guard is a partial unique index on `appointments`; see [docs/schema.md](docs/schema.md).

## Documentation

- [Proposal](docs/proposal.md)
- [Architecture](docs/architecture.md)
- [ER diagram](docs/er-diagram.md)
- [Relational schema](docs/schema.md)
- [Request flow & Page vs Front Controller](docs/request-flow.md)
- [Code-walkthrough script](docs/code-walkthrough.md)
- Wireframes — `docs/wireframes.html`

## Milestone 1 deliverables

Rendered submission files live in [`deliverables/milestone1/`](deliverables/milestone1/):
the report PDF, the ER diagram (PNG and SVG), and the wireframes (PDF and one PNG
per screen). The report source is `deliverables/milestone1/report.html`. To
regenerate the PDF, open that file in a browser and print it to PDF.
