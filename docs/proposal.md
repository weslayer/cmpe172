# Project Proposal — Equipment Rental

## Scenario

An **equipment-rental** booking system. Providers list individually rentable
equipment (drills, generators, ladders, excavators, …), publish the time periods
each item is available, and customers reserve an item for a specific period. The
database structurally prevents the same item/period from being booked twice.

## User roles

- **Customer** — browses available equipment, books a period, views and cancels
  their own rentals.
- **Provider / admin** — manages equipment and availability, and views the
  reservations booked against their equipment.

## Main entities

| Entity | Purpose |
| --- | --- |
| `users` | Customer, provider, and admin accounts |
| `providers` | Equipment managers (a profile attached to a user) |
| `services` | Individually rentable equipment items |
| `availability_slots` | Bookable time periods for an item |
| `appointments` | A customer's reservation of a slot |

See [er-diagram.md](er-diagram.md) and [schema.md](schema.md) for the full data model.

## Core features (12)

Milestone 1 builds only the skeleton — the two read endpoints below. The rest
are scheduled for later milestones.

1. Customer log in — *M2*
2. Browse, filter, and paginate available slots (by provider, service, and/or date; SQL `LIMIT`/`OFFSET`) — *M1 skeleton (`GET /api/slots`)*
3. Book a slot for a chosen service — *M2*
4. View my rentals (upcoming + history) — *M2*
5. Cancel my rental (owner-only) — *M2*
6. Provider log in — *M2*
7. Provider: create / remove availability slots — *M2*
8. Provider: view reservations booked with me — *M2*
9. Send a confirmation via a mock external service on book/cancel — *M3*
10. Logging, health endpoint, and at least one metric — *M3*
11. AI agents: a conversational booking agent and an autonomous agent — *M4*
12. Grounded (retrieval-augmented) Q&A over a small knowledge base — *M4*

The home summary (`GET /api/home`) is the second required read endpoint.

## Architecture summary

Client (SPA / HTTP) → Spring Boot application (Controller → Service → Repository →
JDBC) → PostgreSQL. A mock external confirmation service sits behind a
notification boundary and is added in Milestone 3. The full block diagram is in
[architecture.md](architecture.md).

## Technology statement

The application is built with **Java + Spring Boot**, and all database access is
**SQL over JDBC — no ORM** (no JPA/Hibernate). Liquibase manages the schema.

The project realizes classic enterprise standards on a modern stack: **Spring
Boot in place of J2EE**, and **REST in place of CORBA / distributed objects**.
