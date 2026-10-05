# Event Hub

Spring Boot ticket platform: events, quotas, JWT auth, demo card payments, QR tickets, email delivery, and seller check-in. Includes a **Thymeleaf** web UI and a **JSON REST API**.

## Features

- **Roles:** `USER` (buy), `SELLER` (events + check-in), `ADMIN` (user management, Swagger, actuator details)
- **Events:** draft → published; ticket types with price and quota
- **Orders:** reserve quota → pay (demo gateway) → issued tickets with QR codes
- **Email:** purchase confirmation via [Resend](https://resend.com)
- **Account:** the signed-in user's name in the header links to `/profile` (name, email, role, log out)
- **Security:** JWT access + refresh sessions, CSRF on HTML forms, rate-limited login/register. A failed login or log out revokes the current session and clears the cookie
- **Schema:** PostgreSQL + Flyway (`ddl-auto=validate`)

Payment is **simulated** (no real bank). Checkout needs a code from `checkout_codes` — seeded with `EH-OK-001`, `EH-OK-002`, `DEMO2026`. Card numbers ending in **`0002`** are declined.

## Stack

| | |
|---|---|
| Runtime | Java 21, Spring Boot 4.1 |
| Data | Spring Data JPA, PostgreSQL 16 |
| UI | Thymeleaf |
| API docs | springdoc — `/swagger-ui/index.html` |
| QR | ZXing |
| Build | Maven (`mvnw`) |

## Prerequisites

- **JDK 21**
- **Docker** — local Postgres (`compose.yaml`) and integration tests (Testcontainers)

## Local development

1. Copy env template and fill secrets (never commit `.env`):

   ```bash
   cp .env.example .env
   ```

   Required at minimum: `DB_PASSWORD`, `JWT_SECRET` (long random string), `RESEND_API_KEY`, `RESEND_FROM`.

2. Start Postgres:

   ```bash
   docker compose -f compose.yaml up -d
   ```

3. Run the app:

   ```bash
   ./mvnw spring-boot:run
   ```

   Windows: `mvnw.cmd spring-boot:run`

4. Open [http://localhost:8080](http://localhost:8080). Bootstrap admin uses `ADMIN_EMAIL` / `ADMIN_PASSWORD` from `.env` (see `.env.example`). The admin is only created when that email does not exist yet; changing `ADMIN_PASSWORD` later does not update an existing account.

### Try the flow

1. Log in as admin → **People** → **Make seller** on a registered account.
2. As the seller → **New event** (status `PUBLISHED`) → add a ticket type with price and quota.
3. As a buyer → open the event → buy → pay with any valid test card (e.g. `4242 4242 4242 4242`, `12/30`, `123`) and code `EH-OK-001`.
4. **Tickets** shows the codes and QR. The seller opens a ticket and presses **Check in**.

### Tests

```bash
./mvnw clean test
```

Docker must be running (Testcontainers PostgreSQL). Tests use `src/test/resources/application-test.properties`, but a local `.env` is still imported; CI has no `.env`, so a test that only passes with your `.env` will fail there.

## Configuration

Application defaults live in `src/main/resources/application.properties` (placeholders only). Secrets come from **environment variables** or a local **`.env`** file (`spring.config.import`).

| Variable | Purpose |
|----------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL |
| `JWT_SECRET` | HS256 signing (required) |
| `JWT_EXPIRATION_MS` | Access token TTL (default 15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh TTL (default 7 days) |
| `RESEND_API_KEY`, `RESEND_FROM` | Transactional email |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Startup admin user |
| `COOKIE_SECURE` | `true` behind HTTPS (Coolify sets this) |

See `.env.example` for a full local template.

## API overview

- Public: `POST /api/auth/**`, `GET /api/events/**`, `GET /api/tickets/{code}/qr`
- Authenticated: orders, tickets, seller event APIs
- Admin: `/api/users/**`, most `/actuator/**` (health is public)

All JSON responses use `{ "status", "message", "data" }`. Details: run the app and open Swagger, or see project docs under `.cursor/skills/event-hub/` for maintainers.

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`):

1. **`mvnw test`** on push/PR to `main` / `master`
2. On push to `main`, optional **Coolify deploy** if repo secret `COOLIFY_DEPLOY_WEBHOOK` is set

Tests run in the cloud; you do not need a local `mvn test` on every push.

## Deploy with Coolify (staging / private)

Use **`docker-compose.yml`** (not `compose.yaml`, which is Postgres-only for local dev).

1. New resource → GitHub repo → **Docker Compose** → compose file `docker-compose.yml`
2. Set environment variables in Coolify (`RESEND_*`, `ADMIN_*`; Postgres/JWT often auto-generated)
3. Restrict access (private URL, Basic Auth, or IP allowlist) if the app is not public yet
4. Optional: disable Coolify “deploy on every git push” and rely on the GitHub webhook secret so deploy runs only after green CI

The app image is built from the root **`Dockerfile`** (`mvn package` in build stage, JRE 21 at runtime).

## Project layout

```
src/main/java/dev/takuma/event_hub/
  controller/   REST API
  web/          Thymeleaf pages
  service/      interfaces + impl/
  entity/       domain + JPA
  security/     JWT, filters
src/main/resources/db/migration/   Flyway
```

## License

No license file yet — all rights reserved unless you add one.
