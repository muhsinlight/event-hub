---
name: event-hub
description: Event Hub (Spring Boot ticket API) architecture, layers, conventions, and where to look. Use for any code change in this repo so the agent does not rescan src. Also use when adding endpoints, entities, auth, orders, tickets, or QR.
---

# Event Hub

Do not explore the whole codebase first. Use this skill, then open only the files you will edit.

## Stack

- Spring Boot 4.1.1, Java 21, Maven (`pom.xml`)
- Spring Web MVC, Thymeleaf, Data JPA, Validation, Security
- JJWT 0.12.6, ZXing 3.5.3 (QR), PostgreSQL, springdoc-openapi 3.1.1, Resend HTTP API
- Docker Compose starts Postgres (`compose.yaml`); DB password, JWT secret, and Resend key come from env / `.env`, never from committed properties
- `spring.jpa.hibernate.ddl-auto=validate` with Flyway in `src/main/resources/db/migration`

Base package: `dev.takuma.event_hub`

## Layering

```
controller/   REST — validate, one service call, ApiResponse
web/          Thymeleaf controllers; Pages + Forms for redirects and validation
dto/          request/response records by area (`auth`, `user`, `event`, `order`, `ticket`, `common`)
entity/       JPA models; state changes live here (create, reserve, cancel)
config/       infrastructure beans (CORS, HTTP client, clock). Security filter chain stays in security/
service/      business interfaces. Controllers depend on these, not on impl
service/impl/ @Service implementations. Map entities to DTOs here
repository/   Spring Data JPA
service/support/  shared service helpers (e.g. EventAccess)
security/     JWT, RefreshTokens, filter, SecurityConfig
utils/        ApiResponse, ApiException (+ Database/Server), ApiExceptionHandler, TransactionHooks
```

Constructor injection only. Controllers stay thin and never see entities.

## Domain snapshot

`User` (email unique, BCrypt password, role `USER`, `SELLER`, or `ADMIN`) authenticates. Registration always creates `USER`. `ADMIN` passes every role gate and grants `SELLER`. Login issues a short-lived access JWT bound to an `AuthSession` plus a refresh token. `USER` buys tickets. `SELLER` creates events, adds ticket types, checks tickets in. Role rules live in `SecurityConfig`; ownership rules live on entities (`Event.ownedBy`, `Order.ownedBy`, `Ticket.visibleTo`).

`Event` (DRAFT | PUBLISHED | CANCELLED, owned by a `seller`) has `TicketType`s (price, quota, soldCount). DRAFT is visible only to its seller.

Purchase (JWT required): lock ticket type → event must be PUBLISHED → quota check → reserve `soldCount` → `Order` AWAITING_PAYMENT. `POST /api/orders/{id}/pay` checks the card (demo, no bank), then CONFIRMED → N `Ticket`s (UUID `code`, ISSUED) → after commit, email ticket codes via Resend. Card ending `0002` declines and keeps the reservation. Cancel of AWAITING_PAYMENT releases the quota; cancel of CONFIRMED cancels ISSUED tickets and decrements quota. Check-in: ISSUED → CHECKED_IN.

## When changing code

1. Match existing file in the same layer; copy its shape. New business rules go on the entity; the `*ServiceImpl` orchestrates the transaction and returns a DTO.
2. New REST method → `ApiResponse` + `SecurityConfig` if visibility changes.
3. New business failure → `ApiException` (403/404/409), not a raw exception. Hide resources the caller may not see with 404; use 403 when the caller is acting on something that exists but isn't theirs.
4. Quota / soldCount → `findByIdForUpdate` inside `@Transactional`.

## References

- Endpoints and payloads: [api.md](api.md)
- Entities and purchase/cancel/check-in rules: [domain.md](domain.md)
- JWT, SecurityFilterChain, public routes: [security.md](security.md)
