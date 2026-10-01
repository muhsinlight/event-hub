# REST API

Envelope (all JSON except QR PNG):

```json
{ "status": 200, "message": "OK", "data": {} }
```

Built via `ApiResponse.builder()`. Auth header: `Authorization: Bearer <jwt>` (access token).

Paged lists use `PageResponse`: `{ content, page, size, totalElements, totalPages }`. `page` is zero-based. `size` is capped at 100.

Swagger UI: `/swagger-ui/index.html`. OpenAPI JSON: `/v3/api-docs`.

## Auth — `/api/auth`

| Method | Path | Auth | Body | Data |
|---|---|---|---|---|
| POST | `/register` | public | `{ name, email, password }` password min 8. Role is always `USER` | `UserResponse` (`id`, `name`, `email`, `role`) 201 |
| POST | `/login` | public | `{ email, password }` | `{ token, refreshToken, user }` |
| POST | `/refresh` | public | `{ refreshToken }` | `{ token, refreshToken, user }` (old refresh is invalid). 401 `Invalid refresh token` |
| POST | `/logout` | Bearer access | — | message `Logged out`. Revokes the session so the access token stops working. 401 without a valid access token |

## Users — `/api/users` (`ADMIN`)

| Method | Path | Body | Data |
|---|---|---|---|
| GET | `/?page=&size=&sort=` | — | `PageResponse<UserResponse>`. Default `size=20`, `sort=id,asc` |
| POST | `/{id}/role` | `{ role }` `USER` or `SELLER` | updated `UserResponse`. 403 if the target is an admin or the role is `ADMIN` |

## Events — `/api/events`

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/` | SELLER | body = `EventRequest` (name, venue, startsAt, status). Caller becomes `seller`. Data = `EventResponse`. 201 |
| GET | `/?page=&size=&sort=` | public | page of non-DRAFT events, plus caller's own drafts if a token is sent. Default `size=12`, `sort=startsAt,asc`. `data` is `PageResponse` |
| GET | `/search?name=&page=&size=&sort=` | public | exact name match, same visibility and page shape |
| GET | `/{id}` | public | 404 Event not found (also for other sellers' drafts) |

## Ticket types — `/api/events/{eventId}/ticket-types`

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/` | SELLER, event owner | body = `TicketTypeRequest` (name, price, quota). soldCount forced to 0. Data = `TicketTypeResponse` (`eventId`, no nested event). 201. 403 if not owner |
| GET | `/` | public | 404 if event missing or not visible |

## Orders — `/api/orders` (JWT)

Identity = `authentication.getName()` (email).

| Method | Path | Body | Data |
|---|---|---|---|
| POST | `/` | `{ ticketTypeId, quantity }` quantity > 0. `USER` role only | `OrderResponse` AWAITING_PAYMENT (`quantity`, `amount`). Reserves quota. 201 |
| POST | `/{id}/pay` | `{ cardNumber, expiry, cvc }` `MM/YY`. Demo only: `4242424242424242` pays, a number ending `0002` declines (402). No card number is stored | `List<TicketResponse>` with `qr` base64. 201. After commit, Resend emails ticket codes |
| GET | `/?page=&size=&sort=` | — | `PageResponse<OrderResponse>` for that email. Default `size=20`, `sort=id,desc` |
| POST | `/{id}/cancel` | — | cancelled `OrderResponse` |

## Tickets — `/api/tickets`

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/{code}` | JWT, buyer or event seller | `TicketResponse` + `qr`. 404 for anyone else |
| GET | `/{code}/qr` | public | `image/png` bytes, not ApiResponse |
| POST | `/{code}/check-in` | SELLER, event owner | ISSUED → CHECKED_IN, data = `TicketResponse`. 403 if not owner |

## Errors (`ApiExceptionHandler`)

| Case | HTTP |
|---|---|
| `ApiException` | its status (403/404/409/401/500) |
| Wrong role on protected route | 403 `{"status":403,"message":"Forbidden","data":null}` |
| Bean validation | 400, `"field message, ..."` |
| Invalid path or query type (e.g. non-numeric id) | 400 `Bad Request` |
| DataIntegrityViolation | 409 `"Data conflict"` (logged; SQL stays off the response) |
| DataAccessException | 500 `"Database error"` (logged) |
| Any other unexpected exception | 500 `"Server error"` (logged) |
| Missing/invalid JWT on protected route | 401 `{"status":401,"message":"Unauthorized","data":null}` |
