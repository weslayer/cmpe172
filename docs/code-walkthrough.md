# Code Walkthrough Script (Milestone 1)

Aim for about five and a half minutes. The requirement is at least five, so the
extra time keeps you safe. The first minute is the demo and the rest is walking
through the code. You don't have to read this word for word; it's here so you
know what to cover in each part.

Each step starts with a timestamp and where to go. Paths are from the repo root,
and the line numbers match the current code.

## Before you hit record

**Terminal 1** (leave the startup log visible):

```bash
docker compose down -v            # start from an empty database
docker compose up -d db
mvn spring-boot:run
```

**Terminal 2** is for the requests (Git Bash, or `curl.exe` in PowerShell).

**Editor tabs**, opened in this order:

1. `pom.xml`
2. `src/main/java/com/cmpe172/rental/EquipmentRentalApplication.java`
3. `src/main/resources/db/changelog/schema.sql`
4. `src/main/resources/db/changelog/db.changelog-master.yaml`
5. `src/main/resources/application.yml`
6. `src/main/java/com/cmpe172/rental/controller/SlotController.java`
7. `src/main/java/com/cmpe172/rental/config/GlobalExceptionHandler.java`
8. `src/main/java/com/cmpe172/rental/service/SlotService.java`
9. `src/main/java/com/cmpe172/rental/repository/SlotRepository.java`
10. `src/main/java/com/cmpe172/rental/service/HomeService.java`
11. `src/test/java/com/cmpe172/rental/controller/SlotControllerTest.java`
12. `src/test/java/com/cmpe172/rental/repository/SlotRepositoryTest.java`
13. `deliverables/milestone1/architecture.svg` (for the closing shot)

Expand the `src/main/java/com/cmpe172/rental/` folder in the file explorer so
the package folders are visible.

---

## 0:00 – 1:00 · Demo

**0:00 · Terminal 1**, scrolled up to the lines that start with `liquibase.ui : Running Changeset`.

> Hi, I'm [your name], and this is my Milestone 1 for CMPE 172, an equipment
> rental system built with Spring Boot, plain JDBC, and Postgres. I started the
> app on an empty database, and you can see Liquibase ran two changesets: the
> schema, then the seed data.

**0:17 · Terminal 2**

```bash
curl -s localhost:8080/api/home | python -m json.tool
```

> This is the home endpoint, reading from the database. One provider, six
> pieces of equipment, and twelve open slots.

**0:30 · Terminal 2**, run the first command, then the second

```bash
curl -s "localhost:8080/api/slots?page=0&size=5" | python -m json.tool
curl -s "localhost:8080/api/slots?page=1&size=5" | python -m json.tool
```

> The slots endpoint is paginated, so page zero gives me the first five and
> page one gives me the next five.

**0:45 · Terminal 2**

```bash
curl -s "localhost:8080/api/slots?serviceId=1" | python -m json.tool
```

> If I filter by the drill, I only get slots two and three. Slot one is
> missing because the seed data already has a confirmed booking on it.

**0:55 · Terminal 2**

```bash
curl -s "localhost:8080/api/slots?size=0"
```

> And something invalid, like a page size of zero, gets a 400. Okay, let's
> look at the code.

## 1:00 – 1:25 · Project setup

**1:00 · Go to** [`pom.xml`, lines 24–62](../pom.xml#L24-L62) (the `<dependencies>` block)

> In the pom I've got Spring Web, Spring JDBC, validation, Liquibase, and the
> Postgres driver. There's no JPA or Hibernate, so all the SQL is written by
> hand.

**1:10 · Go to** the file explorer at `src/main/java/com/cmpe172/rental/` and point at `controller/`, `service/`, `repository/`, and `dto/`

> The packages are split by layer: controllers, services, repositories,
> and DTOs, and calls only go downward.

**1:17 · Go to** [`src/main/java/com/cmpe172/rental/EquipmentRentalApplication.java`, lines 6–7](../src/main/java/com/cmpe172/rental/EquipmentRentalApplication.java#L6-L7)

> Every request comes in through Spring's DispatcherServlet first. That's the
> front controller, and it hands the request off to the right controller
> method.

## 1:25 – 2:45 · Database

**1:25 · Go to** [`src/main/resources/db/changelog/schema.sql`](../src/main/resources/db/changelog/schema.sql#L4). Scroll through line 4 (`users`), 15 (`providers`), 24 (`services`, with `asset_tag` on line 30), 42 (`availability_slots`), and 58 (`appointments`).

> Here's schema.sql. There are five tables. Users, then providers, which is
> basically a business profile attached to a user. Services, which in my case
> means one piece of equipment, each with a unique asset tag. Then availability
> slots, and appointments, which are the actual reservations.

**1:45 · Same file.** Point at line 11 (`chk_users_role`), line 50 (`chk_slot_time_order`), and line 51 (`uq_slot_service_time`).

> All the foreign keys are NOT NULL, and roles and statuses have CHECK
> constraints so you can't put random values in. A slot has to end after it
> starts, and the same item can't have the same time slot listed twice.

**2:00 · Same file, lines 41–44.** The comment says the provider is reached through the service, and the table has only `service_id`.

> The schema's also normalized. A slot doesn't store its provider; you get
> there through the equipment, so the two can't end up disagreeing.

**2:10 · Same file, lines 70–73** (`uq_active_appointment_slot`)

> This index down here is kind of the main point of the whole design. It's a
> partial unique index that only applies to pending or confirmed appointments,
> so a slot can only have one active booking. Cancelled ones don't count, so if
> someone cancels, the slot opens back up. And since the database enforces it,
> if two people book the same slot at the exact same time, only one of them
> gets it. The other insert just fails, and in Milestone 2 that turns into a
> 409 Conflict.

**2:35 · Terminal 2** (optional, about 10 seconds)

```bash
docker exec rental-postgres psql -U rental -d rental -c "INSERT INTO appointments (availability_slot_id, customer_id, status) VALUES (1, 1, 'PENDING');"
```

> Just to show it, if I try to put a second booking on slot one directly...
> yeah, Postgres rejects it.

## 2:45 – 3:05 · Migrations

**2:45 · Go to** [`src/main/resources/db/changelog/db.changelog-master.yaml`](../src/main/resources/db/changelog/db.changelog-master.yaml#L4). Point at line 4 (`001-schema`, which runs `schema.sql` on line 9) and line 15 (`002-seed`, which runs `seed.sql` on line 20).

> For migrations I'm using Liquibase, and it's the only thing that creates the
> schema. The changelog runs schema.sql and then seed.sql. It keeps track of
> what it's already run, so restarting the app doesn't run them again.

**2:58 · Go to** [`src/main/resources/application.yml`, lines 14–21](../src/main/resources/application.yml#L14-L21) (the `liquibase` block and `mode: never` on line 21)

> I also turned off Spring's built-in SQL setup so the two don't conflict.

## 3:05 – 3:30 · Controller

**3:05 · Go to** [`src/main/java/com/cmpe172/rental/controller/SlotController.java`, lines 27–35](../src/main/java/com/cmpe172/rental/controller/SlotController.java#L27-L35). `@Validated` is on line 18, and `@Min`/`@Max` are on lines 32–33.

> Now I'll follow a request to the slots endpoint. The controller is pretty
> small. Spring converts the query parameters into the right types for me, and
> the Min and Max annotations reject things like a negative page or a page size
> over a hundred before my code even runs.

**3:22 · Go to** [`src/main/java/com/cmpe172/rental/config/GlobalExceptionHandler.java`, lines 14–30](../src/main/java/com/cmpe172/rental/config/GlobalExceptionHandler.java#L14-L30)

> Those errors go to the global exception handler, so they all come back in the
> same JSON format.

## 3:30 – 3:40 · Service

**3:30 · Go to** [`src/main/java/com/cmpe172/rental/service/SlotService.java`, lines 19–23](../src/main/java/com/cmpe172/rental/service/SlotService.java#L19-L23)

> The service is simple. It asks the repository for one page of results and
> the total count with the same filters, and puts both into a PageResponse.

## 3:40 – 4:40 · Repository

**3:40 · Go to** [`src/main/java/com/cmpe172/rental/repository/SlotRepository.java`, lines 20–30](../src/main/java/com/cmpe172/rental/repository/SlotRepository.java#L20-L30) (`FROM_AVAILABLE`; the `NOT EXISTS` is on line 26)

> The repository is where all the SQL lives. This part right here is what
> "available" means: the slot is open, and there's no pending or confirmed
> appointment on it. It's one query with joins, so I'm not doing a separate
> lookup for every row.

**3:57 · Same file, lines 66–81** (`filters`; the named parameters are on lines 69, 73, and 77)

> The filters are optional. If you pass one in, it adds a small piece of SQL
> with a named parameter, like colon serviceId. I never paste the user's input
> straight into the query, so there's no SQL injection.

**4:10 · Same file, lines 50–57** (`findAvailable`; `LIMIT`/`OFFSET` on line 53, `page * size` on line 55)

> Pagination happens in the database with LIMIT and OFFSET, where the offset is
> just page times size, and the ORDER BY keeps the pages consistent.

**4:20 · Same file, lines 32–42** (`MAPPER`)

> Then the row mapper turns each row into a SlotDto. Nothing outside this class
> touches the JDBC stuff; everything else just works with the DTOs.

**4:30 · Go to** [`src/main/java/com/cmpe172/rental/service/HomeService.java`, line 26](../src/main/java/com/cmpe172/rental/service/HomeService.java#L26)

> The home page calls the same count method, so both endpoints always agree on
> what counts as available.

## 4:40 – 5:10 · Tests

**4:40 · Go to** [`src/test/java/com/cmpe172/rental/controller/SlotControllerTest.java`, lines 42–61](../src/test/java/com/cmpe172/rental/controller/SlotControllerTest.java#L42-L61) (the `rejects…` tests)

> I've got fifteen tests. The controller tests use MockMvc to check the routing
> and the validation errors, and the service tests mock out the repository.

**4:50 · Go to** [`src/test/java/com/cmpe172/rental/repository/SlotRepositoryTest.java`](../src/test/java/com/cmpe172/rental/repository/SlotRepositoryTest.java#L25). Point at line 26 (`excludesSlotsWithAnActiveAppointment`), line 33 (`filtersByServiceAndDate`), and line 48 (`paginatesWithLimitAndOffset`).

> The repository tests run against a real Postgres database, so the SQL
> actually gets executed. Those check that the booked slot is hidden, that the
> filters work, and that pages don't overlap.

**5:05 · Go to** [`.github/workflows/ci.yml`, line 44](../.github/workflows/ci.yml#L44), or the repo's Actions tab on GitHub showing a green run

> GitHub Actions runs all of them on every push.

## 5:10 – 5:30 · Wrap-up

**5:10 · Go to** [`deliverables/milestone1/architecture.svg`](../deliverables/milestone1/architecture.svg)

> So that's Milestone 1. The DispatcherServlet is the front controller, the
> layers stay thin, all the SQL lives in the repositories, and the database
> itself stops double-booking. Next milestone I'm adding login and actual
> booking on top of this. Thanks for watching.
