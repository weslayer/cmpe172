# Code-Walkthrough Outline (≥ 5 min)

An outline for the Milestone 1 walkthrough video. Explain the code and how it
works — do not just demo the app.

1. **Project shape (30s)** — layered packages
   (`controller/service/repository/dto/config`), the no-ORM / JDBC choice, and
   Liquibase as the only DB initializer. Point at `pom.xml` dependencies.

2. **Database design (90s)** — walk
   [`schema.sql`](../src/main/resources/db/changelog/schema.sql): the five
   tables, keys, CHECK constraints, and especially the partial unique index
   `uq_active_appointment_slot`. Explain *why* it prevents double-booking and how
   cancelled rows drop out of the index. Reference [schema.md](schema.md).

3. **Migrations & seed (45s)** — `db.changelog-master.yaml` runs `schema.sql`
   then `seed.sql`; changesets are tracked in `DATABASECHANGELOG` and never
   re-run. Show the seeded provider/equipment/slots.

4. **The read path (2 min)** — trace `GET /api/slots`:
   - [`SlotController`](../src/main/java/com/cmpe172/rental/controller/SlotController.java)
     — param binding + `@Validated` (`@Min`/`@Max`).
   - [`SlotService`](../src/main/java/com/cmpe172/rental/service/SlotService.java)
     — page + count → `PageResponse`.
   - [`SlotRepository`](../src/main/java/com/cmpe172/rental/repository/SlotRepository.java)
     — the "available" SQL (`OPEN` and `NOT EXISTS` active appointment), dynamic
     filters, and `LIMIT`/`OFFSET`.
   Then briefly show `GET /api/home` (`SummaryRepository` counts + featured).

5. **Live check (45s)** — call both endpoints; show pagination (page 0 vs 1) and
   a validation `400`. Optionally show a second active-appointment insert being
   rejected by the database.

6. **Front Controller (30s)** — `DispatcherServlet` as the single entry point;
   REST/JSON for an SPA, not server-rendered pages. Reference
   [request-flow.md](request-flow.md).
