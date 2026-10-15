# #83 authentication and authorization — roadmap

Umbrella for issue #83 ("Authentication & Authorization"). The issue listed open choices: OTP or password, role- or permission-based, our own tables or Keycloak, JWT or sessions. This file records what was decided, how the work was split and what is still open, so a session with no history can pick it up. Each part gets its own design → plan → PR.

## Starting point (2026-10-14)

- **Nothing is authenticated.** `SecurityConfig` permits every request; HTTP Basic is switched on and unused.
- **Callers say who they are.** Address, payment-method and order-status endpoints take `@RequestParam customerId`, `restaurantId` or `ActorRole role`; customer endpoints take `/{customerId}`. Anyone can act as anyone.
- **The application owns passwords.** `users.user_password` is non-null, `POST /customers` registers with one and `PUT /customers/{id}/password` changes it.
- **Ownership is already in our schema:** `customers.user_id` and `restaurants.user_id` reference `users`, and services guard it (`OrderService.ensureOwnedBy`, …).

## Decisions that span every part

- **Keycloak is the identity provider.** It owns credentials, login (later passwordless OTP), sessions, roles and its admin UI. ADR 0007 records why.
- **The API is an OAuth2 resource server that validates JWTs locally**, against Keycloak's published keys, with no call to Keycloak per request.
- **A Keycloak user maps to our `users` row through `users.user_keycloak_id`**, matched against the token's `sub`. Our primary keys stay `BIGINT`.
- **Ownership stays ours.** Keycloak roles are global ("is a restaurant owner"); which restaurant, cart or order belongs to whom stays in Postgres and in the services' guards.
- **Tests sign their own tokens** with a key pair generated per test run, so no test and no CI job needs a running Keycloak.

## Parts

Order: 1 → 4 → 2 → 3. It closes the "anyone can act as anyone" hole as early as possible; sign-up and passwordless login come last, and until then development users come from the Keycloak realm file.

| # | What | Status | Design | Branch / PR |
|---|---|---|---|---|
| 1 | Plumbing: Keycloak in compose (its tables in a `keycloak` schema of our database), the realm as a committed file, the API validating JWTs without requiring them yet, `users.user_keycloak_id`, test token signing, Swagger login | Implemented; PR not opened yet | `2026-10-14-keycloak-plumbing-design.md` | `feat/GH-83-keycloak-plumbing` |
| 4 | Roles and permissions: permissions as `restaurant-api` client roles, bundled by the realm roles `CUSTOMER`, `RESTAURANT_OWNER` and `ADMIN`; a converter from `resource_access.restaurant-api.roles`; endpoints require a token and a permission | Implemented; PR not opened yet | `2026-10-14-roles-and-permissions-design.md` | `feat/GH-83-roles` (stacked on #116) |
| 2 | The authenticated caller: replace the `customerId`, `restaurantId` and `role` parameters with the identity from the token (`sub` → `users` → customer or restaurant), endpoint by endpoint | Implemented; PR not opened yet | `2026-10-15-authenticated-caller-design.md` | `feat/GH-119-authenticated-caller` (stacked on #121) |
| 3 | Sign-up and login: passwordless email OTP through Keycloak; how a new Keycloak user gets its `users` row; what happens to `POST /customers`, the change-password endpoint and `user_password`. Phone OTP only if still wanted then. | Not started | | |

## Decided in part 4

Permissions are `restaurant-api` client roles; realm roles bundle them; the API checks permissions only (ADR 0008). Public: health, Swagger, `/error`, `POST /api/v1/customers`; restaurant browsing will be public when it exists.

## Decided in part 2

- The token carries only Keycloak's identity; after verification the API resolves `sub` → `users` → customer into a `Caller` (ADR 0009). An unknown `sub` is a 401.
- A customer never names themselves: `/customers/me`, no `customerId` anywhere, lookups scoped to the caller, so someone else's row is a 404. A restaurant owner names the restaurant (`?restaurantId`), checked against the caller (403). `?role` is gone.
- Nothing bypasses ownership, `ADMIN` included; the admin has a `users` row (V16) with no customer or restaurant.
- `GET /orders/{orderId}` is visible to the order's customer and its restaurant's owner; `GET /orders` is customer-only.

## Open questions for later parts

- **3:** Keycloak-hosted registration versus our API calling Keycloak's admin API; whether Keycloak's email OTP needs an extension in the pinned version; the mail server for development (e.g. Mailpit). Whether `default-roles-restaurant` gives `CUSTOMER` to every sign-up. A Keycloak user with no `users` row gets 401 since part 2, even on public endpoints if it sends its token; sign-up decides how the row appears (the admin API, or get-or-create on the first token).
- **Later:** a staff table before any cashier role; publishing or switching off `/.well-known/oauth-protected-resource`. A restaurant's view of its orders; admin endpoints on other users' data; recording which user performed an order transition.

## Working agreements

As in the #84 roadmap: brainstorming one question at a time with each design section approved, then a plan, then implementation; ask for an explicit go-ahead before pushing, opening PRs or editing issues.
