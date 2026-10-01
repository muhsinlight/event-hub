# Domain

IDs: `Long`, `GenerationType.IDENTITY`. Enums stored as STRING.

## User (`users`)

Fields: name, email (unique), password (`@JsonIgnore`, BCrypt), role (`USER` buyer, `SELLER` ticket seller, `ADMIN`). Enum is `entity.Role`, stored as STRING.

Registration always creates `USER`. `ADMIN` is created from `ADMIN_EMAIL` / `ADMIN_PASSWORD` at startup. Admin grants or removes `SELLER` via `POST /api/users/{id}/role` (`USER` or `SELLER` only). An admin's own role cannot be changed this way. Login returns `{ token, refreshToken, user }` where `user` is `UserResponse` (no password).

Register: reject duplicate email (`409 Email already used`). Login: `401 Invalid email or password`. Access JWT subject = email; claims include `sid` (session id) and `typ=access`.

## AuthSession (`auth_sessions`)

Fields: `user`, `refreshTokenHash` (SHA-256 hex, unique), `expiresAt`, `revokedAt` (nullable). Raw refresh tokens are never stored. Login creates a session; refresh rotates the hash; logout sets `revokedAt`. Access tokens are rejected when the session is revoked or past `expiresAt`.

## Event

Fields: name, venue, startsAt (`LocalDateTime`), status (`DRAFT`, `PUBLISHED`, `CANCELLED`), `seller` (`ManyToOne` User, LAZY, set from the authenticated seller on create). Column is nullable because rows created before ownership have no seller; such events have no owner.

Sale is allowed only when status is `PUBLISHED`.

`ownedBy(email)`: seller email matches. `isVisibleTo(email)`: not DRAFT, or owner. DRAFT events are 404 to everyone else (list, search, by id, ticket types). `EventRepository.findVisible` / `findVisibleByName` apply the same rule in JPQL and return a page.

Only the owning seller can add ticket types (`403 Event belongs to another seller`).

## TicketType

Fields: name, price (`BigDecimal` @Positive), quota (@Positive), soldCount (@Min 0), `ManyToOne` Event required.

On create, `TicketType.create` sets `soldCount = 0`. Remaining = quota − soldCount. `hasCapacity` uses a `long` sum so `soldCount + quantity` cannot overflow.

`TicketTypeRepository.findByIdForUpdate` uses `PESSIMISTIC_WRITE`. Use it for purchase and cancel. Cancel locks the order with `OrderRepository.findByIdForUpdate`. Check-in locks the ticket with `TicketRepository.findByCodeForUpdate`.

## Order (`orders`)

Fields: buyerName, buyerEmail, status (`AWAITING_PAYMENT`, `CONFIRMED`, `CANCELLED`), quantity, amount, optional ticket type, required `ManyToOne` User.

Buyer snapshot is copied from the authenticated `User` at purchase time.

## Ticket

Fields: code (UUID string, unique), status (`ISSUED`, `CHECKED_IN`, `CANCELLED`), Order, TicketType. `qr` is `@Transient` (base64 PNG).

`GET /api/tickets/{code}` is 404 unless `Ticket.visibleTo(email)`: the buyer (order owner) or the event's seller.

## Purchase (`OrderService.purchase`)

1. Load user by email; 404 if missing.
2. Lock ticket type; 404 if missing.
3. Event must be `PUBLISHED` else `409 Event is not on sale`.
4. `soldCount + quantity > quota` → `409 Not enough tickets`.
5. Increment soldCount and save an AWAITING_PAYMENT order (amount = price × quantity). Tickets are not issued yet.

## Pay (`OrderService.pay`)

404 if the order is missing. Ownership mismatch → `409 Order belongs to another user`. Not AWAITING_PAYMENT → `409 Order is not awaiting payment`. Invalid card → 400. A Luhn-valid number ending in `0002` → `402 Payment declined` and the reservation stays. Otherwise store `Payment` (last4 and brand only), issue tickets, mark CONFIRMED, and email after commit.

## Cancel (`OrderService.cancel`)

404 if order missing. Ownership always runs: match `user.email`, or `buyerEmail` when `user` is null. Null user is not a skip. Mismatch → `409 Order belongs to another user`. Already CANCELLED → `409 Order already cancelled`.

AWAITING_PAYMENT releases the reserved quantity. CONFIRMED: for each ISSUED ticket, set CANCELLED, lock ticket type, decrement soldCount. Then order → CANCELLED.

## Check-in (`TicketService.checkIn`)

404 if code unknown. `403 Ticket belongs to another seller's event` unless the caller owns the ticket's event (`Ticket.checkableBy`). `409 Ticket already used` if CHECKED_IN. `409 Ticket is not valid` unless ISSUED. Else CHECKED_IN.

## QR (`QrService`)

ZXing 300×300 PNG. `png(text)` → bytes; `base64(text)` → string. Failure → `500 QR could not be created`.
