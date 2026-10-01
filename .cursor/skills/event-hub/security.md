# Security

Stateless JWT access tokens plus server-side `AuthSession` for refresh and logout. `SecurityConfig` + `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

Filter is a `@Component` but `FilterRegistrationBean` is **disabled** so it is not registered twice in the servlet chain. Keep that pattern.

## Public vs authenticated

Permit all:

- `/error` (so Spring's error dispatch is not turned into 401)
- `GET /`, `GET /events/**` except `GET /events/new`, `GET /favicon.ico`, `/css/**`, `/js/**`, `/images/**`
- `GET` and `POST /login`, `/register`
- Page login writes an HttpOnly `access_token` cookie (`SameSite=Lax`). `JwtAuthenticationFilter` accepts that cookie when the `Authorization` header is absent. `/api/**` still uses `Bearer`. Unauthenticated page requests redirect to `/login`; wrong role redirects to `/?denied`. API requests stay JSON 401/403.
- `/api/auth/**` (logout still requires a valid Bearer access token; the controller returns 401 without one)
- `GET /api/events/**` (includes `GET /api/events/{id}/ticket-types`)
- `GET /api/tickets/*/qr`
- `GET /actuator/health` (status only; component details require `ADMIN`)

Role rules:

- `POST /api/events`, `POST /api/events/*/ticket-types`, `POST /api/tickets/*/check-in`, and the matching page routes → `SELLER` or `ADMIN`
- `POST /api/orders` and the matching page routes → `USER` or `ADMIN`
- `/api/users/**`, `GET /admin/users`, `POST /admin/users/*/role`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, `/actuator/**` except public health → `ADMIN`

`anyRequest().authenticated()`. CSRF is on for browser forms (cookie repository, `Secure` when `COOKIE_SECURE=true`) and ignored for `/api/**`, which accepts only `Authorization: Bearer` — the access cookie is not read on `/api/**`. Session `STATELESS`. `POST /login` and `POST /register` (page and API) allow 10 attempts per minute per client address. Ownership (seller owns event, buyer owns order) is checked in services, not in `SecurityConfig`.

CORS (`CorsConfig`, applied on the filter chain): `/api/**` from `http://localhost:*` and `http://127.0.0.1:*`. Methods `GET`, `POST`, `OPTIONS`. Headers `Authorization`, `Content-Type`. Same-origin Thymeleaf pages do not send a CORS request; this covers a browser on another port.

Unauthenticated protected call → 401 JSON:
`{"status":401,"message":"Unauthorized","data":null}`. Wrong role → 403 `{"status":403,"message":"Forbidden","data":null}`.

## JWT and sessions

- Config: `app.jwt.secret` = `JWT_SECRET`, `app.jwt.expiration-ms` = `JWT_EXPIRATION_MS` (access), `app.jwt.refresh-expiration-ms` = `JWT_REFRESH_EXPIRATION_MS` (default 7 days). Datasource password = `DB_PASSWORD`. Never commit secret values; copy `.env.example` to `.env` locally. Missing `JWT_SECRET` fails startup.
- Login creates an `AuthSession` (SHA-256 of refresh token, `expiresAt`, optional `revokedAt`) and returns access JWT + raw refresh token. Access claims: subject = email, `sid` = session id, `typ` = `access`.
- Filter verifies the JWT, requires `typ=access` and `sid`, loads the session, and rejects revoked/expired sessions or email mismatch. Tokens without `sid` are rejected.
- Refresh rotates the refresh hash on the same session. Logout sets `revokedAt`; that access token fails on the next request.
- Header: `Authorization: Bearer <access>`. Invalid token or inactive session → clear context, continue filter chain (then 401 if route is protected).

## User details

`AppUserDetailsService` loads `User` by email for login (`AuthenticationManager`) and throws `UsernameNotFoundException` when missing (never `ApiException`, so login maps unknown email to the same 401 as a wrong password). Filter authentication uses `SecurityUser` with the session id. Authorities: `ROLE_USER`, `ROLE_SELLER`, or `ROLE_ADMIN`. Registration always stores `USER`. Startup promotes or creates the account in `app.admin.email` as `ADMIN` when `app.admin.password` is set. `Authentication.getName()` is email — services take that string, not a user id.

On public GETs the `Authentication` argument is `null` for anonymous callers; pass `null` as the viewer email.

`PasswordEncoder` = `BCryptPasswordEncoder`. `AuthenticationManager` from `AuthenticationConfiguration`.

## Mail (Resend)

`MailService` sends purchase confirmation after the order transaction commits. Production: `ResendMailService` (`RESEND_API_KEY`, `RESEND_FROM`). Missing `RESEND_API_KEY` fails startup outside `test`. Under the `test` profile there is no `MailService` bean, so every `@SpringBootTest` declares `@MockitoBean(types = MailService.class)` at the type level — `ApiIntegrationTest` covers its subclasses, `EventHubApplicationTests` and `ErrorResponseIntegrationTest` declare their own. Tests that care about the email constructor-inject `MailService` and assert with `verify` / `ArgumentCaptor`.

## When adding a route

1. Add the controller method.
2. If it should be public, add a matcher in `SecurityConfig` (be specific: method + path).
3. If it needs the current user, take `Authentication` and pass `getName()` into the service.
