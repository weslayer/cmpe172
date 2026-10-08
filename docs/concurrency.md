# Concurrency and Transactions

How booking stays correct when two people try to book the same slot at the same
time. Code: [`BookingService`](../src/main/java/com/cmpe172/rental/service/BookingService.java),
[`BookingTransaction`](../src/main/java/com/cmpe172/rental/service/BookingTransaction.java),
[`SlotRepository.lock`](../src/main/java/com/cmpe172/rental/repository/SlotRepository.java),
and the test [`BookingConcurrencyTest`](../src/test/java/com/cmpe172/rental/service/BookingConcurrencyTest.java).

## The race

Casey and Jordan both have slot 3 (the cordless drill on September 7) open in
their browsers, and both click **Confirm booking** at the same moment. If booking
were just "check, then insert" with nothing coordinating the two requests, this
could happen:

| Time | Casey's request (T1) | Jordan's request (T2) |
| --- | --- | --- |
| 1 | Is slot 3 booked? No. | |
| 2 | | Is slot 3 booked? No. |
| 3 | Insert appointment for Casey | |
| 4 | | Insert appointment for Jordan |
| 5 | Commit, return 201 | Commit, return 201 |

Both checks ran before either insert, so both passed. Both customers get a
confirmation for the same drill on the same day. This is a classic
check-then-act race, and testing one request at a time never catches it.

## ACID applied to booking

- **Atomicity.** Locking the slot, checking it, and inserting the appointment
  happen in one transaction. If any step fails (the slot is taken, the lock times
  out, the database rejects the insert), the whole thing rolls back and nothing
  is half-written.
- **Consistency.** The database's own rules hold after every transaction: an
  appointment must point at a real slot and customer, its status must be BOOKED,
  CANCELLED, or COMPLETED, and the partial unique index allows at most one BOOKED
  appointment per slot.
- **Isolation.** Two bookings for the same slot behave as if one ran completely
  before the other. That is what the row lock below provides.
- **Durability.** The API answers 201 only after the transaction commits. Once
  Postgres has committed, the booking is in its write-ahead log and survives a
  crash or restart.

## Isolation level: READ COMMITTED

The booking transaction runs at READ COMMITTED, which is Postgres's default; the
code states it explicitly so the choice is visible:

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public AppointmentDto book(long customerId, long slotId) {
    Slot slot = slots.lock(slotId)
            .orElseThrow(() -> new NotFoundException("Slot " + slotId + " not found"));
    if (!"OPEN".equals(slot.status()) || !slot.startsAt().isAfter(OffsetDateTime.now())) {
        throw new ConflictException("Slot " + slotId + " is no longer available");
    }
    if (appointments.existsBooked(slotId)) {
        throw new ConflictException("Slot " + slotId + " is already booked");
    }
    return appointments.get(appointments.insertBooked(slotId, customerId));
}
```

Under READ COMMITTED, every statement sees all data committed before that
statement started. Combined with the row lock, this is exactly what booking
needs. The second request waits on the lock; when it finally gets the lock, its
next statement (the "is it booked?" check) sees the first request's committed
appointment and stops with a 409.

The other levels were worse fits:

- **REPEATABLE READ** takes one snapshot at the first query of the transaction,
  before the wait. After getting the lock, the check would still be looking at
  the old snapshot and would not see the winner's appointment. The unique index
  would still stop the double booking, but the main mechanism would be blind.
- **SERIALIZABLE** would catch the race without an explicit lock, but by
  aborting one of the transactions with a serialization failure. The code would
  have to retry every conflict, and Postgres has to track more read
  dependencies. A row lock on one slot is simpler and cheaper.

## Concurrency control: a pessimistic row lock

Booking locks the slot's row before checking it:

```sql
SET LOCAL lock_timeout = '2s';

SELECT sv.provider_id, s.status, s.starts_at
FROM availability_slots s
JOIN services sv ON sv.id = s.service_id
WHERE s.id = :id
FOR UPDATE OF s;
```

`FOR UPDATE OF s` locks only the slot row, not the equipment row it joins to. A
second transaction asking for the same lock waits until the first commits or
rolls back. Bookings for *different* slots never block each other.

The race from the table above now plays out like this:

| Time | Casey (T1) | Jordan (T2) |
| --- | --- | --- |
| 1 | Lock slot 3 | |
| 2 | | Lock slot 3: **waits** |
| 3 | Not booked; insert; commit (lock released) | |
| 4 | | Gets the lock |
| 5 | | Is slot 3 booked? **Yes**: 409 Conflict |

The "is it booked?" check has to be its own statement after the lock. If it
were a subquery inside the `FOR UPDATE` query, Postgres would evaluate it with
that query's starting snapshot, which was taken before the wait, so it would
miss Casey's insert.

**Why pessimistic rather than an optimistic version check.** An optimistic design
would add a `version` column to slots, read it, and fail the update when another
transaction had already changed the version. That works, but it turns every
conflict into a failure that the code must catch and retry. With a row lock,
contenders simply queue, and the loser gets a clear answer on its first try.
Postgres supports `SELECT ... FOR UPDATE`, the lock is held for only a few
milliseconds, and only requests for the same slot ever wait.

The same lock is used by **provider slot removal**, so a provider removing a slot
and a customer booking it cannot interleave: whichever locks first wins, and the
other sees the result. **Cancelling** locks the appointment row
(`FOR UPDATE OF a`), so two cancels of the same appointment cannot both succeed.

## The database backstop

The unique constraint from Milestone 1 stays as a second line of defense:

```sql
CREATE UNIQUE INDEX uq_active_appointment_slot
    ON appointments (availability_slot_id)
    WHERE status = 'BOOKED';
```

Even if some future code path inserted an appointment without taking the lock,
Postgres would reject a second BOOKED row for the same slot. Spring reports that
as a `DuplicateKeyException`, and `GlobalExceptionHandler` turns it into a 409
without exposing the SQL error.

## Retry

```java
// Retries only transient lock failures (lock timeout, deadlock); a taken slot is a final answer.
public AppointmentDto book(long customerId, long slotId) {
    for (int attempt = 1; ; attempt++) {
        try {
            return booking.book(customerId, slotId);
        } catch (PessimisticLockingFailureException ex) {
            if (attempt == MAX_ATTEMPTS) {
                throw ex;
            }
        }
    }
}
```

Retrying only makes sense when the failure was temporary:

- **Retried (up to 3 attempts):** the lock wait passed `lock_timeout`
  (Postgres error 55P03), or Postgres picked this transaction as a deadlock
  victim (40P01). Spring reports both as `PessimisticLockingFailureException`.
  The next attempt might succeed.
- **Not retried:** "slot already booked" and "slot no longer available". Those
  are final answers, and retrying would only ask the same question again.

The retry loop lives in `BookingService`, and the transaction lives in a
separate bean, `BookingTransaction`. That split matters: Spring applies
`@Transactional` through a proxy, so each call into `BookingTransaction` starts a
fresh transaction. A retry inside the transaction would reuse a transaction that
Postgres has already aborted. If all three attempts fail, the API returns 503
("Busy, please try again").

## How it is tested

[`BookingConcurrencyTest`](../src/test/java/com/cmpe172/rental/service/BookingConcurrencyTest.java)
runs against the real Postgres database:

1. It creates a fresh future slot.
2. It starts two threads, one for each seed customer, held at a `CountDownLatch`.
3. It releases both at once; each calls `BookingService.book` on the same slot.
4. It asserts exactly one success, that the other thread got a
   `ConflictException`, and that the database holds exactly one BOOKED row for
   the slot.

The test is not `@Transactional`, because each thread must really commit. It
deletes its slot afterwards.

The `ConflictException` assertion is deliberate. To check that the test can
really fail, `FOR UPDATE` was temporarily removed: the test failed on every run,
because the losing thread got through the check and was stopped only by the
unique index (`DuplicateKeyException`). With the lock back in place, the loser
is always stopped by the check.

The same behavior holds over HTTP: two `curl` requests from two signed-in
customers fired in parallel at one slot returned one `201` and one `409`.

## Limits

- All of this relies on a single Postgres primary, which puts every booking in
  one order. If the database were ever replicated across regions with writes in
  more than one place, the lock would no longer be enough.
- Two *overlapping* slots on the same item (for example 9–5 and 10–12) are still
  possible; see the known limitation in [schema.md](schema.md).
