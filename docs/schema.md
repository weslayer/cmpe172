# Relational Schema

Source of truth: [`schema.sql`](../src/main/resources/db/changelog/schema.sql), plus
the Milestone 2 migration [`booking-status.sql`](../src/main/resources/db/changelog/booking-status.sql).
All timestamps are `TIMESTAMPTZ`; identifiers are `BIGINT` identity columns.

## Tables

**users** — customer, provider, and admin accounts.

| Column | Type | Constraints |
| --- | --- | --- |
| id | bigint | PK |
| email | varchar(255) | NOT NULL, UNIQUE |
| full_name | varchar(255) | NOT NULL |
| password_hash | varchar(255) | NOT NULL |
| role | varchar(20) | NOT NULL, CHECK ∈ {CUSTOMER, PROVIDER, ADMIN} |
| created_at | timestamptz | NOT NULL, default now() |

**providers** — an equipment-manager profile attached to a user.

| Column | Type | Constraints |
| --- | --- | --- |
| id | bigint | PK |
| user_id | bigint | NOT NULL, UNIQUE, FK → users(id) |
| business_name | varchar(255) | NOT NULL |
| description | text | |
| created_at | timestamptz | NOT NULL, default now() |

**services** — individually rentable equipment items.

| Column | Type | Constraints |
| --- | --- | --- |
| id | bigint | PK |
| provider_id | bigint | NOT NULL, FK → providers(id) |
| name | varchar(255) | NOT NULL |
| category | varchar(100) | |
| description | text | |
| asset_tag | varchar(100) | NOT NULL, UNIQUE |
| daily_rate | numeric(10,2) | NOT NULL, CHECK ≥ 0 |
| status | varchar(20) | NOT NULL, CHECK ∈ {ACTIVE, INACTIVE, MAINTENANCE} |
| created_at | timestamptz | NOT NULL, default now() |

**availability_slots** — bookable time periods for an item.

| Column | Type | Constraints |
| --- | --- | --- |
| id | bigint | PK |
| service_id | bigint | NOT NULL, FK → services(id) |
| starts_at | timestamptz | NOT NULL |
| ends_at | timestamptz | NOT NULL |
| status | varchar(20) | NOT NULL, CHECK ∈ {OPEN, BLOCKED}; a provider "removing" a slot sets BLOCKED |
| created_at | timestamptz | NOT NULL, default now() |

Table constraints: `CHECK (ends_at > starts_at)` and `UNIQUE (service_id, starts_at, ends_at)`.

**appointments** — a customer's reservation of a slot.

| Column | Type | Constraints |
| --- | --- | --- |
| id | bigint | PK |
| availability_slot_id | bigint | NOT NULL, FK → availability_slots(id) |
| customer_id | bigint | NOT NULL, FK → users(id) |
| status | varchar(20) | NOT NULL, default BOOKED, CHECK ∈ {BOOKED, CANCELLED, COMPLETED} |
| created_at | timestamptz | NOT NULL, default now() |

Plus the partial unique index described below. COMPLETED is not written by the
application: a BOOKED appointment whose slot has already ended is reported as
COMPLETED when it is read.

## Normalization

The schema is in third normal form: every non-key column depends only on its
table's key. Facts that can be derived are not stored twice. A slot's provider
comes from `availability_slots → services → providers`, and an appointment's
equipment comes from `appointments → availability_slots → services`. Because
these are joins rather than copied columns, a slot can never disagree with its
equipment's provider, and an appointment can never disagree with its slot.

## Relationships and cardinality

- **users 1 — 0..1 providers.** A user may have at most one provider profile
  (`providers.user_id` is UNIQUE). Customers have none.
- **providers 1 — many services.** A provider owns many equipment items; each
  item belongs to exactly one provider.
- **services 1 — many availability_slots.** Each item has many bookable periods;
  each period belongs to one item.
- **availability_slots 1 — 0..1 *active* appointments.** A slot can be reserved by
  at most one BOOKED appointment at a time — this is the double-booking guard. Over its lifetime a slot may accumulate several
  cancelled rows plus one active one.
- **users (customer) 1 — many appointments.** A customer can hold many
  reservations.

## Weak entities

There are no weak entities — every table has its own surrogate key and can be
identified independently. `availability_slots` and `appointments` are
existence-dependent on their parents (enforced by NOT NULL foreign keys), but
they are not identifier-dependent, so they are regular entities rather than weak
ones.

## Double-booking guard

```sql
CREATE UNIQUE INDEX uq_active_appointment_slot
    ON appointments (availability_slot_id)
    WHERE status = 'BOOKED';
```

A **partial unique index** allows at most one row per `availability_slot_id`
among rows whose status is `BOOKED`. Cancelled appointments are excluded from the
index, so a slot becomes bookable again after a cancellation. Because the
guarantee lives in the database, two concurrent booking requests for the same
slot cannot both succeed: the second insert fails with a unique violation, which
the application surfaces as **409 Conflict**.

In Milestone 2 the booking transaction also takes a row lock on the slot, so the
index is the backstop rather than the main mechanism. See
[concurrency.md](concurrency.md).

## Migrations

| Changeset | File | What it does |
| --- | --- | --- |
| `001-schema` | `schema.sql` | The five tables, constraints, and the guard index |
| `002-seed` | `seed.sql` | Sample users, provider, equipment, slots, one appointment |
| `003-booking-status` | `booking-status.sql` | Statuses become BOOKED / CANCELLED / COMPLETED; the guard index is rebuilt on `status = 'BOOKED'` |
| `004-seed-m2` | `seed-m2.sql` | Moves the seed slots to September 2027, adds a second customer and a past (completed) rental, and replaces the seed password hash with a real BCrypt hash of `password` |

Applied changesets are never edited; each schema change is a new changeset.

## Known limitation (in scope for the assignment)

`UNIQUE (service_id, starts_at, ends_at)` prevents *identical* duplicate slots,
and the partial index prevents double-booking a *single* slot. Neither prevents
a provider from creating two **overlapping** slots for the same item (e.g.
09:00–17:00 and 10:00–12:00), which could then both be booked. Closing that gap
would require a range exclusion constraint (PostgreSQL `EXCLUDE USING gist` with
`btree_gist`); it is beyond the constraints this project requires and is noted
here as a deliberate boundary.
