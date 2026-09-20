# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Equipment-rental booking system, built as a CMPE 172 term project but developed
under professional engineering discipline. Java 22, Spring Boot 3.4.x, Maven,
PostgreSQL accessed via **plain JDBC** (`JdbcTemplate` / `NamedParameterJdbcTemplate`
— no JPA/Hibernate/ORM), Liquibase as the sole schema initializer, Docker Compose
for local Postgres. REST API, `DispatcherServlet` as front controller, no
server-rendered views.

See `README.md` for run instructions and `docs/` for architecture, ER diagram,
schema, and request-flow docs. Don't restate that material here — read it there.

@.claude/slides.md

## Scope discipline (read this before adding anything)

This is graded milestone work. Each milestone has an enumerated set of
requirements — see `docs/proposal.md` and the milestone in progress. Implement
exactly that, at production quality. Do not add:

- Entities, columns, constraints, or endpoints beyond what the current milestone
  specifies, even if they'd be "more correct" or "more complete."
- Speculative abstractions (generic repository base classes, plugin/strategy
  layers, config toggles) for requirements that don't exist yet.
- Frameworks or dependencies beyond what's already in `pom.xml` without asking
  first.

"Enterprise" here means rigor and craftsmanship *within* the assigned scope —
correct transactions, real validation, clean layering, tests for what you
write — not maximal feature coverage. When in doubt, implement the smaller
thing well rather than the larger thing speculatively.

## Architecture

Strict layering, one direction of dependency:

```
controller → service → repository → (JDBC / Postgres)
                ↓
               dto (returned to callers; never leak JDBC/ResultSet types out of repository)
```

- `controller/` — thin. Request mapping, `@Validated` param constraints,
  delegates to a service. No SQL, no business logic.
- `service/` — orchestration and business rules. No `ResultSet`/JDBC types.
- `repository/` — all SQL lives here via `NamedParameterJdbcTemplate`. Use
  named parameters (`:name`), never string-concatenated values. `RowMapper`s
  are `private static final` fields. Multi-line SQL uses text blocks (`"""`).
- `dto/` — Java `record`s for API request/response shapes.
- `model/` — domain types (populated as later milestones require them).
- `config/` — cross-cutting config and `@RestControllerAdvice` exception
  handling.
- `notification/` — notification boundary (populated in M3).

Every package has a `package-info.java` describing its role — add one for any
new package.

## Conventions

- Constructor injection only; no field `@Autowired`.
- DTOs are immutable `record`s.
- Validate at the boundary: `@Validated` + `jakarta.validation.constraints`
  on controller params/DTOs. Don't re-validate the same thing deeper in the
  stack.
- Errors funnel through `GlobalExceptionHandler` (`config/`) into a consistent
  JSON error shape (`status`, `error`, `message`). Add new `@ExceptionHandler`
  methods there rather than handling exceptions ad hoc in controllers.
- No secrets or credentials in code, `application.yml`, or commits. Config
  comes from environment variables with local-dev defaults matching
  `docker-compose.yml` (see `.env.example`). Liquibase is the only DB
  initializer — never re-enable Spring's `sql.init`.
- SQL: parameterize everything; never build queries by string-concatenating
  user input.
- Comments: one short line where genuinely non-obvious (a workaround, a
  invariant that isn't visible from the code). No multi-paragraph doc
  comments, no narrating what the code already says.

## Testing

Add tests for the code you write in each milestone (JUnit 5 /
`spring-boot-starter-test`; add the dependency if introducing the first test).
Repository tests can run against the Dockerized Postgres; service/controller
logic should be unit-testable without a live DB. Don't backfill tests for
future milestones' code, and don't chase coverage numbers — test the behavior
that matters (filters, pagination bounds, conflict/error paths).

## Commands

```bash
docker compose up -d db          # local Postgres
mvn spring-boot:run              # run the app (Liquibase migrates + seeds on first run)
mvn test                         # run tests
mvn -q -DskipTests package       # build only
```

## Git workflow

- Small, logical commits — don't lump unrelated changes together.
- Before every `git commit`: show the full diff (and any build/test output),
  then stop and wait for explicit approval. Never commit unapproved.
- Commit subject lines: short, lowercase, human-written style (e.g.
  `add liquibase schema and seed`) — no AI-generated-sounding filler.
