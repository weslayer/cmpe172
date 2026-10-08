# Code Walkthrough Script (Milestone 2)

Aim for about six minutes. The requirement is at least five, and it has to explain
the code rather than demo the app, so the demo is kept to the first forty seconds.
You don't have to read this word for word; it's here so you know what to cover in
each part.

Each step starts with a timestamp and where to go. Paths are from the repo root,
and the line numbers match the current code.

## Before you hit record

**Terminal 1** (backend):

```bash
docker compose down -v            # start from the seed data
docker compose up -d db
mvn spring-boot:run
```

**Terminal 2** (frontend), then open <http://localhost:5173> in a browser:

```bash
cd frontend
npm run dev
```

**Browser:** two windows side by side. Sign in as `customer@example.com` in one and
as `customer2@example.com` in a private window. The password is `password`.

**Editor tabs**, opened in this order:

1. `src/main/resources/db/changelog/booking-status.sql`
2. `src/main/java/com/cmpe172/rental/config/SecurityConfig.java`
3. `src/main/java/com/cmpe172/rental/repository/UserRepository.java`
4. `frontend/src/pages/Book.jsx`
5. `frontend/src/api.js`
6. `src/main/java/com/cmpe172/rental/controller/AppointmentController.java`
7. `src/main/java/com/cmpe172/rental/service/BookingService.java`
8. `src/main/java/com/cmpe172/rental/service/BookingTransaction.java`
9. `src/main/java/com/cmpe172/rental/repository/SlotRepository.java`
10. `src/main/java/com/cmpe172/rental/service/SlotService.java`
11. `src/main/java/com/cmpe172/rental/config/GlobalExceptionHandler.java`
12. `src/test/java/com/cmpe172/rental/service/BookingConcurrencyTest.java`
13. `src/test/java/com/cmpe172/rental/service/BookingServiceTest.java`

---

## 0:00 – 0:40 · Short demo

**0:00 · Browser, Casey's window.** Go to Browse, filter Equipment to the cordless
drill, and click **Book** on the September 7 slot. Leave the confirm page open.

> Hi, I'm [your name], and this is Milestone 2 for CMPE 172. Last time I had the
> skeleton; now there's login, booking, cancelling, and the provider side, with
> a React frontend. Casey is about to book this drill.

**0:15 · Jordan's window.** Book the same slot and confirm it. **Then Casey's
window:** click **Confirm booking**.

> Jordan grabs it first. When Casey confirms, she gets a conflict: the slot is
> already booked. Preventing exactly that is most of this milestone, so let's
> look at how it works.

**0:30 · Casey's window.** Click **My rentals**.

> Her rentals are split into upcoming and history, and the old one shows as
> completed. Okay, on to the code.

## 0:40 – 1:05 · Schema change

**0:40 · Go to** [`booking-status.sql`, lines 7–16](../src/main/resources/db/changelog/booking-status.sql#L7-L16)

> The assignment wants the statuses booked, cancelled, and completed, so I added
> a new Liquibase changeset instead of editing the old one. It moves the
> existing rows over and rebuilds the unique index so it only covers booked
> appointments. That index is still the database's own guarantee that a slot
> can't have two active bookings. Completed isn't stored: a booked appointment
> whose slot already ended is just reported as completed when you read it.

## 1:05 – 2:05 · Login and role-based access

**1:05 · Go to** [`SecurityConfig.java`, lines 30–37](../src/main/java/com/cmpe172/rental/config/SecurityConfig.java#L30-L37)

> Login uses Spring Security. Passwords are stored as BCrypt hashes, never plain
> text, and this lambda is how Spring finds a user: by email, through my own
> JDBC repository.

**1:18 · Go to** [`UserRepository.java`, lines 27–34](../src/main/java/com/cmpe172/rental/repository/UserRepository.java#L27-L34)

> That's a plain SQL query. The role comes straight from the user's row, and the
> left join picks up the provider id if the user is a provider.

**1:28 · Back to** [`SecurityConfig.java`, lines 44–49](../src/main/java/com/cmpe172/rental/config/SecurityConfig.java#L44-L49)

> These are the access rules. Browsing is public. Anything under
> /api/appointments needs the customer role, and anything under /api/provider
> needs the provider role. A request with the wrong role is rejected with a 403
> before it ever reaches a controller.

**1:42 · Same file, lines 50–59**

> Login is a form post to /api/auth/login. On success Spring creates a
> server-side session and puts a session cookie in the browser, and I send the
> user back as JSON so the frontend knows who's signed in.

**1:52 · Same file, lines 72–79**

> Since the session is a cookie, I kept CSRF protection on. The server puts a
> token in a cookie, and the frontend has to copy it into a header on every
> post, which a forged request from another site can't do.

## 2:05 – 2:40 · Frontend to backend

**2:05 · Go to** [`frontend/src/pages/Book.jsx`, lines 13–24](../frontend/src/pages/Book.jsx#L13-L24)

> Here's the frontend side of a booking. When you click confirm, it calls my api
> helper with the slot id. If the server says 409, it shows the "someone just
> booked it" message.

**2:15 · Go to** [`frontend/src/api.js`, lines 16–38](../frontend/src/api.js#L16-L38)

> The helper is a small wrapper around fetch. It adds the CSRF header from the
> cookie and turns error responses into exceptions. The request goes to
> /api on the Vite dev server, which forwards it to Spring Boot on port 8080,
> so the browser only ever sees one origin.

**2:28 · Go to** [`AppointmentController.java`, lines 30–34](../src/main/java/com/cmpe172/rental/controller/AppointmentController.java#L30-L34)

> On the server, the controller validates the body, so a missing slot id is a
> 400, and it takes the customer id from the session, not from the request. A
> customer can't book in someone else's name.

## 2:40 – 4:20 · The booking transaction

**2:40 · Go to** [`BookingTransaction.java`, lines 24–36](../src/main/java/com/cmpe172/rental/service/BookingTransaction.java#L24-L36)

> This is the important part. The race is two people booking the same slot at
> the same time: if both check "is it free?" before either one inserts, both
> checks pass and you get a double booking. So the whole booking is one
> transaction, marked @Transactional, at READ COMMITTED.

**2:58 · Go to** [`SlotRepository.java`, lines 78–88](../src/main/java/com/cmpe172/rental/repository/SlotRepository.java#L78-L88)

> The first thing it does is lock the slot's row with SELECT FOR UPDATE. That's
> a pessimistic lock. If Jordan's transaction already has the lock, Casey's
> waits right here until Jordan commits. It only locks that one slot, so other
> bookings aren't slowed down, and the wait is capped at two seconds.

**3:18 · Back to** [`BookingTransaction.java`, lines 29–35](../src/main/java/com/cmpe172/rental/service/BookingTransaction.java#L29-L35)

> After it has the lock, it checks the slot is open and still in the future,
> and then asks whether there's already a booking. That check has to be a
> separate query after the lock. Under READ COMMITTED every statement sees what's
> been committed up to that point, so once Casey gets the lock, this query sees
> Jordan's booking and she gets a conflict. I picked READ COMMITTED for exactly
> that. With REPEATABLE READ the check would still be looking at the old
> snapshot from before the wait, and SERIALIZABLE would work too, but by
> aborting transactions that then all need retries.
>
> For ACID: it's atomic because the lock, check, and insert commit or roll back
> together; consistent because the database constraints still hold afterwards;
> isolated because the lock makes the two bookings run one after the other; and
> durable because I only send the 201 after Postgres commits.

**3:55 · Go to** [`BookingService.java`, lines 25–36](../src/main/java/com/cmpe172/rental/service/BookingService.java#L25-L36)

> Retry lives one level up. If the lock wait times out or Postgres reports a
> deadlock, it tries again, up to three times. It never retries a slot that's
> already taken, because that answer won't change. This loop is in a different
> class from the transaction on purpose: Spring's @Transactional works through a
> proxy, so every attempt gets a brand-new transaction.

**4:10 · Go to** [`booking-status.sql`, lines 14–16](../src/main/resources/db/changelog/booking-status.sql#L14-L16)

> And the unique index is still the backstop. If some future code skipped the
> lock, Postgres would still refuse a second booked row.

## 4:20 – 4:50 · Cancel and the provider side

**4:20 · Go to** [`BookingService.java`, lines 38–50](../src/main/java/com/cmpe172/rental/service/BookingService.java#L38-L50)

> Cancel locks the appointment row, checks that it's yours (otherwise 403),
> checks it hasn't started or been cancelled already, and sets it to cancelled.
> The index ignores cancelled rows, so the slot opens up again.

**4:35 · Go to** [`SlotService.java`, lines 53–64](../src/main/java/com/cmpe172/rental/service/SlotService.java#L53-L64)

> Providers can create and remove slots on their own equipment. Removing takes
> the same row lock as booking, so a removal and a booking can't interleave, and
> a booked slot can't be removed. Removing just marks the slot blocked, so old
> appointments still point at it.

## 4:50 – 5:10 · Errors

**4:50 · Go to** [`GlobalExceptionHandler.java`, lines 49–79](../src/main/java/com/cmpe172/rental/config/GlobalExceptionHandler.java#L49-L79)

> All errors go through one handler and come back in the same JSON shape: 401
> when you're not signed in, 403 for the wrong role or someone else's booking,
> 404 for something that doesn't exist, and 409 for a conflict. If the database
> index fires, that's a 409 too, and the SQL error message is never sent to the
> client. No stack traces either.

## 5:10 – 5:55 · Tests

**5:10 · Go to** [`BookingConcurrencyTest.java`, lines 48–79](../src/test/java/com/cmpe172/rental/service/BookingConcurrencyTest.java#L48-L79)

> This is the concurrency test. It runs against the real database: it makes a
> new slot, starts two threads for two different customers, holds them at a
> latch, and releases them at the same moment. Then it checks that exactly one
> booking succeeded, the other got a conflict, and the database has exactly one
> booked row.
>
> To make sure the test could actually fail, I took out the FOR UPDATE and ran
> it again. It failed every time, because the second thread got past the check
> and was only stopped by the unique index. With the lock back, it passes.

**5:35 · Go to** [`BookingServiceTest.java`, lines 37–95](../src/test/java/com/cmpe172/rental/service/BookingServiceTest.java#L37-L95)

> The service logic also has plain unit tests with mocked repositories: the
> retry, that a taken slot isn't retried, owner-only cancel, and so on. There
> are also controller tests for the 401s, 403s, and 400s, and integration tests
> for login. Everything runs in CI on every push.

## 5:55 – 6:10 · Wrap-up

> So that's Milestone 2: session login with BCrypt and role checks, the booking
> features, and double-booking prevented by a transaction with a row lock, with
> the unique index as the backstop. Next milestone is the mock notification
> service and monitoring. Thanks for watching.
