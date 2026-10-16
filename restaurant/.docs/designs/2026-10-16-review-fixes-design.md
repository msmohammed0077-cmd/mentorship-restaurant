# #83 stack — code-review fixes

A medium code review of the four #83 PRs (#116, #121, #124, #125), plus CodeRabbit on #116, raised 38 findings. Each fix went on the branch that introduced the problem, and each branch was merged into the one above it (no rebase), so the stack stays reviewable PR by PR. This records what changed and what was deliberately left.

## #116 — Keycloak plumbing

- **Public paths ignore bearer tokens.** `SecurityConfig.PUBLIC_PATHS` (health, Swagger, API docs; from #121 also `/error` and, until #125, `POST /customers`) is both the `permitAll` list and a `BearerTokenResolver` filter, so a stale token left in Swagger UI no longer turns public calls into 401s. Health publishes an empty security requirement. Token checks in `BearerTokenEndpointTest` moved to a path with no endpoint.
- Keycloak binds to `127.0.0.1:8180` (CodeRabbit). The Keycloak init script is idempotent.
- The test `JwtDecoder` also honours `jws-algorithms` and extra validator beans, as Boot's does ("light"; replacing it with Boot's own decoder fed a committed key was judged not worth rewiring every test).
- Token signers are built once; the untrusted key is generated once. The OpenAPI test reads the issuer from config.
- The Swagger client accepts ports 8080–8082, listed: a trailing `*` in Keycloak is a prefix match, so `http://localhost*` would accept `localhost.attacker.com`.
- `User.userKeycloakId` loses its inert `unique = true`.
- **Left:** `user_keycloak_id` stays unique across soft-deleted rows. A deleted customer's token still resolves to the old row (part 2), so nothing fails; they are stuck, the limit ADR 0010 records.

## #121 — roles and permissions

- **Deny by default at startup.** `EndpointSecurityVerifier` refuses to start the application if a handler of ours has neither `@PreAuthorize` (method or class) nor the new `@PublicEndpoint`. `EndpointAuthorizationTest` also counts a mapping with no HTTP method, and fails on any 5xx from a permitted request.
- Our 403 carries `WWW-Authenticate: Bearer error="insufficient_scope"`.
- `RealmPermissions` finds the realm file from the repository root too.
- **Left:** order history and order reads were open to the wrong callers on this branch alone; #124 closes both. Validation answering 400 before the permission's 403 stays: fixing it means duplicating every permission as URL rules, and it reveals request shapes, not data. The duplicated test setup is gone in #124. The realm reset is documented.

## #124 — the authenticated caller

- A soft-deleted customer's token outlives the delete, so the five cart endpoints and `GET /orders/{id}` now answer 404 "Customer not found", as addresses, cancel and rate did.
- Another restaurant's order is 404, not 403, so owners cannot probe which ids exist. The lookup is scoped to the named restaurant (`existsByIdAndRestaurant_Id`); the restaurant-ownership check stays a `RestaurantService` call (cross-domain rule), so it is still two queries.
- `CallerAuthenticationToken.getName()` is the token's `sub`.
- Two Javadocs no longer describe a separate ownership check.
- **Left:** customers created by `POST /customers` cannot log in on this branch alone; #125 replaces API sign-up. No caller cache (part 2 design: none until a measurement asks). V16 is not edited: every local database that applied it would need repair, for a clash that needs `admin@example.com` registered through the old API first.

## #125 — email-code login and sign-up

- **Brute-force protection** is on: 5 failures (or rapid ones) lock the account for a growing wait, capped at 15 minutes. Checked live: the plugin's wrong codes count.
- **Keycloak's email wins.** `UserService.findCaller(keycloakId, verifiedEmail)` reads our stored email in the same query and replaces it when a verified token's differs, unless another active row holds it (logged). A race on the unique index falls back to resolving without the update.
- **What counts as a sign-up** is narrower: `customer.write` and no kitchen permission (`order.accept`, `order.reject`, `order.prepare`), from a client in `app.security.sign-up-clients`. Staff created in the console get the default role too; their kitchen permission now keeps them from becoming customers. A dedicated sign-up permission was rejected: Keycloak cannot give a role only to self-registrations without another plugin.
- A failed insert that is not the race is a logged 401, never a raw 500 from the security filter. The unlinked-email refusal says so in its description.
- Names are cut by code points, so an emoji is never split.
- `CLAUDE.md` names the security filter's two methods as the exception to "cross-domain methods join the caller's transaction". The ERD drops `user_password` and gains `user_keycloak_id`.
- **Left:** the reviewer's claim that our default role replaced Keycloak's built-ins was wrong (the admin API showed them kept).
