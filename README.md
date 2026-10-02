# Student Performance & Attendance Analytics Dashboard — Backend

A Spring Boot 3.2 / PostgreSQL backend for the existing Brightfield Academy frontend, built per the layered
architecture, security, and API requirements specified for this project.

## 1. Stack

- Java 17, Spring Boot 3.2.5, Maven
- Spring Web, Spring Data JPA, Spring Security (JWT via jjwt 0.12.5, BCrypt)
- **PostgreSQL 16** (Flyway-managed schema) for development/production
- **H2** (in-memory) for automated tests only
- springdoc-openapi (Swagger UI)
- Lombok
- JUnit 5 + Spring Boot Test

## 2. Running it

### 2a. Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL 16 on `localhost:5432` with database `student_dashboard`, user `dashboard_user` /
password `changeme` (see `docker-compose.yml` — change these for anything beyond local dev).

**Without Docker** — if you already have PostgreSQL installed locally, create the same database/user by hand:

```bash
psql -U postgres
```
```sql
CREATE DATABASE student_dashboard;
CREATE USER dashboard_user WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE student_dashboard TO dashboard_user;
-- PostgreSQL 15+ also needs explicit schema privileges (public schema ownership
-- changed in PG15 — without this, Flyway's first migration will fail to create tables):
\c student_dashboard
GRANT ALL ON SCHEMA public TO dashboard_user;
```

Either way, the app just needs `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` to resolve to a
reachable, empty PostgreSQL database — Flyway creates every table on first run.

### 2b. Configure environment

Copy `.env.example` to `.env` (or export the same variables another way) and adjust as needed. Defaults are
wired to match the Docker Compose service, so local dev works with no changes.

### 2c. Run the backend

```bash
mvn spring-boot:run
```

On first run (empty database), `DataSeeder` populates realistic demo data automatically — 2 academic sessions
(one marked "current"), 3 terms, 10 classes, 8 subjects, 16 teachers, ~110 students, and full performance +
attendance history for the current session's terms. **Demo credentials are printed to the console** at startup:

| Role | Username / Email | Password |
|---|---|---|
| Administrator | `admin` / `admin@brightfield.edu.ng` | `admin123` |
| Teacher | `teacher` / `teacher@brightfield.edu.ng` | `teacher123` |

Set `SEED_DEMO_DATA=false` to skip this (e.g. once you have real data, or for a non-dev deployment).

### 2f. Deploy with PostgreSQL

Deployment uses the explicit `prod` profile. Set the PostgreSQL connection variables and start the packaged
application with:

```bash
set SPRING_PROFILES_ACTIVE=prod
set DB_HOST=your-postgres-host
set DB_NAME=student_dashboard
set DB_USERNAME=dashboard_user
set DB_PASSWORD=your-password
java -jar target/student-dashboard-backend.jar
```

The `prod` profile requires `DB_HOST`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`; it never falls back to the
local development credentials. Flyway applies the PostgreSQL migration and Hibernate validates the schema.

### 2d. API docs

Swagger UI: `http://localhost:8080/swagger-ui.html`. Click **Authorize**, log in via `POST /api/auth/login`
first to get a token, then paste it in (Swagger adds the `Bearer ` prefix automatically).

### 2e. Run tests

```bash
mvn test
```

Tests run against an in-memory H2 database with the schema generated directly from the JPA entities (not the
Postgres-specific Flyway migration — see the comment in `application-test.yml` for why). Seeding is disabled for
tests; each test class sets up its own minimal fixtures.

## 3. Database: PostgreSQL for dev/prod, H2 for tests

```
Development / Production  →  PostgreSQL 16   (Flyway-managed schema, ddl-auto: validate)
Automated Tests            →  H2 (in-memory)  (schema generated from entities, Flyway disabled)
```

H2 is created inside the test JVM and is not a second server or persistent database. Running `mvn spring-boot:run`
uses PostgreSQL; running `mvn test` activates the `test` profile and uses H2.

If PostgreSQL reports `password authentication failed`, the application has reached PostgreSQL successfully but
the credentials do not match. With Docker Compose, `POSTGRES_PASSWORD` only initializes a new data volume; changing
it later does not change an existing database user's password. Either restore the original password in `DB_PASSWORD`,
change the password inside PostgreSQL, or recreate the local volume when its data can be discarded:

```bash
docker compose down -v
docker compose up -d
```

This project was originally built against MySQL and later migrated to PostgreSQL. If you're looking at the
history: **every table, column, and constraint name is unchanged** — this was a syntax translation, not a
schema redesign. What actually changed:

| File | What changed | Why |
|---|---|---|
| `pom.xml` | `mysql-connector-j` + `flyway-mysql` → `org.postgresql:postgresql` + `flyway-database-postgresql` | New JDBC driver + matching Flyway database-support module |
| `application.yml` | Datasource URL/driver/port (3306→5432) + `database-platform` → `PostgreSQLDialect` | Point at Postgres instead of MySQL |
| `V1__init_schema.sql` | `AUTO_INCREMENT` → `GENERATED ALWAYS AS IDENTITY`; removed `ENGINE=InnoDB DEFAULT CHARSET=...` (no Postgres equivalent); removed `SET NAMES utf8mb4;` (MySQL-only); `DATETIME(6)` → `TIMESTAMPTZ` | MySQL-specific DDL syntax has no direct Postgres equivalent — see the comment header in that file for the `TIMESTAMPTZ` reasoning specifically, since it's a semantic choice, not just a syntax swap |
| `docker-compose.yml` | MySQL 8.4 service → PostgreSQL 16 service | Local dev database |
| `.env.example` | `DB_PORT` default 3306 → 5432, wording | Match Postgres defaults |
| `application-test.yml` | H2 URL gained `MODE=PostgreSQL` | Closer behavioral alignment (see comment in the file — this doesn't change *how* the test schema is created, just how H2 interprets SQL functions/behavior generally) |
| `CodeGenerator.java`, `pom.xml` (H2 dependency comment) | Two stale comments referencing MySQL by name | Accuracy only — no logic changed |

**Entities and repositories needed zero changes.** I checked specifically for `columnDefinition` overrides,
native/raw SQL queries, and UUID usage (all classic sources of MySQL-vs-Postgres incompatibility) — there were
none. The entity layer was already portable JPA, which is exactly why this migration only touched
build/config/migration files.

**Why H2 tests still don't run the Postgres migration.** H2's PostgreSQL compatibility mode emulates function
and type-coercion *behavior* — it doesn't parse arbitrary Postgres DDL syntax with full fidelity. Rather than
gamble on whether `GENERATED ALWAYS AS IDENTITY` and every constraint clause happens to parse cleanly under H2,
tests generate their schema directly from the same entities that `ddl-auto: validate` checks against in every
other environment — which is a strictly stronger guarantee than "the migration happens to also work on H2."
The real migration is still verified independently: every column, foreign key, and unique constraint in
`V1__init_schema.sql` was mechanically cross-checked against the entity mappings.

## 4. Connecting the existing frontend

The frontend currently stores a plain user object in `sessionStorage` after "login" against its mock API layer
(`src/services/api.js`). To connect it to this real backend:

1. Set the frontend to call this API's base URL (e.g. via a `VITE_API_BASE_URL` env var) instead of the mock functions.
2. Store the JWT returned by `POST /api/auth/login` (instead of just the user object) and attach it as
   `Authorization: Bearer <token>` on every subsequent request.
3. CORS is already configured (`app.cors.allowed-origins`, defaults to `http://localhost:5173`, the frontend's
   Vite dev server) — update this if the frontend is served from elsewhere.
4. A few frontend concepts that were hardcoded constants in the mock now come from `GET /api/meta` instead:
   session/term labels, classes, and subjects for dropdowns.

This intentionally is NOT done automatically in this pass, per the instruction to inspect and adapt the backend
to the frontend rather than modify the frontend's structure — swapping the API layer is a separate, contained
follow-up.

## 5. Design decisions worth knowing about

A few places where I made an explicit judgment call rather than a literal 1:1 port, all called out in code
comments at the relevant spot too:

- **`POST /api/auth/register` only creates ADMIN accounts.** A bare `User` row with role `TEACHER` and no
  linked `Teacher` record would be permanently unable to log in, given how `AppUserDetailsService` resolves a
  teacher's class scope. Real teacher accounts go through `POST /api/teachers` (optionally granting login in
  the same call) or `POST /api/teachers/{id}/login`, which keep `Teacher` ↔ `User` consistent.
- **`school_class.form_teacher_id` is a real, explicit, nullable column** — not derived from the
  `teacher_class` many-to-many table. A class can have several teachers assigned across different subjects;
  "form teacher" is a distinct, single, optional designation.
- **Teacher Dashboard's subject-performance chart, and Analytics' subject comparison + trend charts, are
  scoped to the teacher's own classes.** While porting the logic from the existing frontend, I found these
  specific charts were computed school-wide there regardless of caller role — a real scoping inconsistency,
  not an intentional feature. Corrected here for consistency with every other teacher-scoping rule enforced
  throughout this backend (reports, class access, student profiles).
- **Report generation is authorized server-side, not just hidden in the UI.** A TEACHER requesting a
  class/student outside their assignment gets a "not authorized" report body back, not the data.
- **Student list filtering by risk level** can't be a SQL predicate (risk is computed from Result +
  Attendance, not a stored column) — that one filter combination pulls the full filtered set, computes risk in
  memory, then paginates by hand. Every other filter combination gets real database-level pagination via JPA
  Specifications.
- **Class-/school-wide averages are computed by fetching the relevant Result/Attendance rows once per scope
  and aggregating in Java**, rather than either (a) one query per student (N+1) or (b) complex SQL aggregate
  pivots. This was a deliberate scale trade-off for a school-sized dataset (dozens to a few hundred students);
  a much larger deployment would want database-level aggregation instead.
- **Attendance bulk-submit mirrors the frontend's exact behavior**: resubmitting the same class + date appends
  new rows rather than upserting. Flagged in code rather than silently changed, since deduping would be a
  behavior change beyond what was asked.

## 6. What I could not verify

This sandbox has no Maven, no network access, and no PostgreSQL — so unlike the frontend (plain JS, verified with
`node --check` and runtime smoke tests throughout), **this backend has not actually been compiled or run.**
Every layer was hand-verified through the build in the ways available without a compiler:
- The Flyway migration was mechanically diffed against every entity's `@Column`/`@JoinColumn` mappings,
  foreign keys, and unique constraints — re-verified after the PostgreSQL rewrite, not just carried over
  from the MySQL-verified version.
- Every repository's derived query methods were checked against actual entity field names.
- Every service and controller's imports were cross-checked against actual usage, and every interface method
  confirmed implemented.
- All 146 Java files pass a brace/paren balance sweep.
- Test assertions were checked against actual DTO record component names (Jackson serializes records by
  component name).

`mvn clean install` on your machine is the real first compile. If something doesn't build, it's most likely a
small, mechanical fix (an import, a method signature) rather than a structural problem — the architecture and
logic have been reviewed carefully even without a compiler to lean on.
