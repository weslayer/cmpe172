# Relational Schema

Source of truth: [`schema.sql`](../src/main/resources/db/changelog/schema.sql).
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
| provider_id | bigint | NOT NULL, FK → providers(id) |
| starts_at | timestamptz | NOT NULL |
| ends_at | timestamptz | NOT NULL |
| status | varchar(20) | NOT NULL, CHECK ∈ {OPEN, BLOCKED} |
| created_at | timestamptz | NOT NULL, default now() |

Table constraints: `CHECK (ends_at > starts_at)` and `UNIQUE (service_id, starts_at, ends_at)`.

**appointments** — a customer's reservation of a slot.

| Column | Type | Constraints |
| --- | --- | --- |
| id | bigint | PK |
| availability_slot_id | bigint | NOT NULL, FK → availability_slots(id) |
| customer_id | bigint | NOT NULL, FK → users(id) |
| service_id | bigint | NOT NULL, FK → services(id) |
| status | varchar(20) | NOT NULL, CHECK ∈ {PENDING, CONFIRMED, CANCELLED, COMPLETED} |
| created_at | timestamptz | NOT NULL, default now() |

Plus the partial unique index described below.

## Relationships and cardinality

- **users 1 — 0..1 providers.** A user may have at most one provider profile
  (`providers.user_id` is UNIQUE). Customers have none.
- **providers 1 — many services.** A provider owns many equipment items; each
  item belongs to exactly one provider.
- **services 1 — many availability_slots.** Each item has many bookable periods;
  each period belongs to one item.
- **availability_slots 1 — 0..1 *active* appointments.** A slot can be reserved by
  at most one active (PENDING/CONFIRMED) appointment at a time — this is the
  double-booking guard. Over its lifetime a slot may accumulate several
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
    WHERE status IN ('PENDING', 'CONFIRMED');
```

A **partial unique index** allows at most one row per `availability_slot_id`
among rows whose status is `PENDING` or `CONFIRMED`. Cancelled and completed
appointments are excluded from the index, so a slot becomes bookable again after
a cancellation. Because the guarantee lives in the database, two concurrent
booking requests for the same slot cannot both succeed — the second commit fails
with a unique-violation, which the application surfaces as **409 Conflict** (M2).
This is the final line of defense against double-booking, independent of any
application-level check.

## Known limitation (in scope for the assignment)

`UNIQUE (service_id, starts_at, ends_at)` prevents *identical* duplicate slots,
and the partial index prevents double-booking a *single* slot. Neither prevents
a provider from creating two **overlapping** slots for the same item (e.g.
09:00–17:00 and 10:00–12:00), which could then both be booked. Closing that gap
would require a range exclusion constraint (PostgreSQL `EXCLUDE USING gist` with
`btree_gist`); it is beyond the constraints this project requires and is noted
here as a deliberate boundary.
