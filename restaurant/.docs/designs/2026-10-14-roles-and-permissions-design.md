# #83 part 4 — Roles and permissions

Part 4 of `2026-10-14-auth-roadmap.md`, built on part 1 (`2026-10-14-keycloak-plumbing-design.md`). Endpoints start requiring a token, and each protected endpoint requires one **permission**. Keycloak bundles permissions into roles; the API checks permissions only and never sees a role name. ADR 0008 records why.

Part 4 decides what *kind* of user may call an endpoint. Which customer, restaurant or order is the caller's own still comes from the `customerId`, `restaurantId` and `role` parameters; part 2 replaces them with the token's identity.

## Decisions

- **Permissions are client roles of `restaurant-api`; realm roles are composites of them.** An admin creates a new role in the Keycloak console from existing permissions, with no code change and no deploy. A new *permission* still needs code, because an endpoint has to check it.
- **The API reads `resource_access.restaurant-api.roles` only.** `realm_access` is ignored.
- **Checks are `@PreAuthorize("hasAuthority('<permission>')")` on each controller method.** `SecurityConfig` holds only the public list and `anyRequest().authenticated()`.
- **Permission names are lowercase and dotted** (`order.accept`), used as the Spring authority with no prefix.

## Public endpoints

| Endpoint | Why |
|---|---|
| `GET /api/v1/health` | Probes and compose healthchecks carry no token |
| `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` | Swagger UI is where you log in |
| `/error` | Spring forwards errors there; requiring a token would turn an anonymous 400 or 404 into a 401 |
| `POST /api/v1/customers` | Registration; the caller has no account yet. Part 3 replaces it |

Everything else requires a valid token and a permission. Restaurant browsing does not exist yet; when #85–#101 add it, its `GET`s are public: anonymous menu browsing is normal for food delivery.

## Permission catalogue

`<resource>.read` / `<resource>.write` where every caller of a resource is the same kind of actor; one verb per order-lifecycle transition, because customers and restaurants change the same order in different ways.

| Permission | Endpoints | `CUSTOMER` | `RESTAURANT_OWNER` |
|---|---|:-:|:-:|
| `customer.read` | `GET /customers/{id}` | ✓ | |
| `customer.write` | `PATCH /customers/{id}`, `PUT /customers/{id}/password`, `DELETE /customers/{id}` | ✓ | |
| `address.read` | `GET /addresses` | ✓ | |
| `address.write` | `POST /addresses`, `PUT /addresses/{id}`, `DELETE /addresses/{id}`, `PUT /addresses/{id}/default` | ✓ | |
| `payment-method.read` | `GET /payment-methods` | ✓ | |
| `payment-method.write` | `POST /payment-methods`, `DELETE /payment-methods/{id}`, `PUT /payment-methods/{id}/default` | ✓ | |
| `cart.read` | `GET /cart/{cartId}` | ✓ | |
| `cart.write` | `POST /cart/items`, `PUT /cart/{cartId}/items/{cartItemId}`, `DELETE /cart/{cartId}`, `DELETE /cart/{cartId}/items`, `POST /cart/{cartId}/checkout` | ✓ | |
| `order.create` | `POST /orders` | ✓ | |
| `order.read` | `GET /orders/{orderId}`, `GET /orders` | ✓ | ✓ |
| `order.cancel` | `POST /orders/{orderId}/cancel` | ✓ | |
| `order.rate` | `POST /orders/{orderId}/rating` | ✓ | |
| `order.accept` | `POST /orders/{orderId}/accept` | | ✓ |
| `order.reject` | `POST /orders/{orderId}/reject` | | ✓ |
| `order.prepare` | `POST /orders/{orderId}/preparing`, `POST /orders/{orderId}/ready-for-pickup` | | ✓ |

`ADMIN` holds all fifteen. All paths are under `/api/v1`. `preparing` and `ready-for-pickup` share a permission: they are the same kitchen job. A future cashier would be `order.read`, `order.accept`, `order.reject` and `order.prepare`.

## The realm (`restaurant/keycloak/realm-restaurant.json`)

- **Client roles on `restaurant-api`:** the fifteen permissions, each with a one-line `description` for the console.
- **Realm roles:** `CUSTOMER` (12 permissions) and `RESTAURANT_OWNER` (4), composites as in the catalogue. `ADMIN` lists all fifteen directly rather than composing the other two roles, so a permission later added to neither role still reaches it.
- **Users:** seeded users 1, 2 and 5 get `CUSTOMER`; 3, 4 and 6 get `RESTAURANT_OWNER`. A new user, `admin@example.com` / `password`, gets `ADMIN`. It has no `users` row: an admin is neither customer nor restaurant, and nothing reads `sub` until part 2.
- **What reaches the token:** the realm file defines no client scopes, so Keycloak creates its built-in ones, including `roles`, which writes the user's effective client roles (composites expanded) into `resource_access.<client>.roles`. `restaurant-swagger` keeps `fullScopeAllowed` at its default `true`, so its tokens carry `resource_access.restaurant-api.roles`. Keycloak's default roles also add `offline_access` and `uma_authorization` to `realm_access` and `account` roles to `resource_access.account`; the API ignores both.
- **Applying it:** a realm edit needs the Keycloak reset from `CLAUDE.md`. The PR description says so.

## The API

**`config/KeycloakPermissionsConverter`**, a `Converter<Jwt, Collection<GrantedAuthority>>`. Spring Security's `JwtGrantedAuthoritiesConverter` reads only a top-level claim, so the nested one needs this.

- Reads `resource_access` → `restaurant-api` → `roles` and maps each to a `SimpleGrantedAuthority`, no prefix.
- Any level missing yields no authorities: the caller is authenticated, and every protected endpoint answers 403.
- The client id is the constant `PERMISSIONS_CLIENT = "restaurant-api"`; `JwtTestSupport` uses the same constant.

**`SecurityConfig`:**

- `@EnableMethodSecurity`;
- `jwt(j -> j.jwtAuthenticationConverter(...))`, a `JwtAuthenticationConverter` wrapping the converter above; the principal's name stays `sub`;
- `permitAll()` for the public endpoints, with a comment on `POST /api/v1/customers` that part 3 replaces registration;
- `anyRequest().authenticated()` in place of `permitAll()`.

**Controllers:** every non-public handler carries `@PreAuthorize("hasAuthority('<permission>')")` from the catalogue, 28 in all. Literal strings, not constants: the annotation reads directly, and `EndpointAuthorizationTest` catches typos.

**`GlobalExceptionHandler`** gains `@ExceptionHandler(AccessDeniedException.class)` returning 403 with the fixed message `Access denied`, in the usual `ErrorResponse` shape. Without it the catch-all `@ExceptionHandler(Exception.class)` turns `@PreAuthorize`'s exception into a 500. The message never names the missing permission.

**Behaviour:**

| Request | Result |
|---|---|
| Public endpoint, no token or a valid one | As today |
| Any endpoint, invalid token | 401, `WWW-Authenticate: Bearer error="invalid_token"` (part 1) |
| Protected endpoint, no token | 401, `WWW-Authenticate: Bearer`, empty body (Spring's entry point) |
| Protected endpoint, valid token without the permission | 403, `Access denied` |
| Valid token with the permission | As today, ownership guards included |

**Validation runs before the permission check.** Spring resolves `@Valid @RequestBody` and required `@RequestParam`s before the secured method is invoked, so a caller without the permission who sends an invalid request gets the 400, not the 403. It learns only the request's shape, which the OpenAPI document already publishes.

**Unchanged:** the `customerId`, `restaurantId` and `role` parameters and every service. A customer token can still name another customer's `customerId`; part 2 closes that.

## Testing

**Permission sets come from the realm file.** `support/RealmPermissions` parses `keycloak/realm-restaurant.json` (tests run from `restaurant/`) and returns each realm role's composite client roles. CI therefore checks the realm's composites, which nothing else does without Keycloak.

**`JwtTestSupport` gains:**

- `tokenFor(UUID sub, String realmRole)`: `resource_access.restaurant-api.roles` holds what the realm gives that role, as Keycloak would expand it;
- `tokenWithPermissions(UUID sub, Collection<String> permissions)`, for the matrix test.

`tokenFor(UUID)` stays: a valid token with no permissions.

**Existing tests** send the token of the actor they play, added by their support base: Customer, Address, PaymentMethod and Cart send a `CUSTOMER` token; Order sends `CUSTOMER` for create, view, history, cancel and rate, and `RESTAURANT_OWNER` for accept, reject, preparing and ready-for-pickup. `sub` is a random UUID; nothing reads it until part 2. A class that must send no token (create-customer) returns `null` from `realmRole()`. No assertion changes.

**`EndpointAuthorizationTest`** (`RANDOM_PORT` + `RestTestClient`) holds one row per protected endpoint: method, path with sample ids, a **valid** request (parameters and body, so validation cannot answer first) and the permission. For each row:

| Token | Expected |
|---|---|
| None | 401, `WWW-Authenticate` starting with `Bearer` |
| Every permission but the row's | 403, `Access denied` |
| The row's permission only | Not 401, and not a 403 with `Access denied` |

The third check excludes only *our* 403: some ownership guards answer 403 themselves (the `// #82 §2` pins), and a request that then fails on a missing entity still proves the permission sufficed.

Two coverage tests in the same class:

- every handler in `RequestMappingHandlerMapping` is public or has a row, so an endpoint added without one fails the build. Without this, an endpoint missing `@PreAuthorize` would be open to any logged-in user;
- every row's permission is a client role in the realm file, and every client role in the realm file is used by a row.

Every row targets ids that do not exist, so a permitted request ends in a 404 and nothing needs cleaning up.

**Public endpoints:** `HealthEndpointTest`, `OpenApiDocsEndpointTest` and `BearerTokenEndpointTest` call without a token, and `CreateCustomerEndpointTest` registers without one.

**Manual check, in the PR description:** after the Keycloak reset, log in to Swagger UI as `ahmed.ali@example.com`, decode the token and confirm `resource_access.restaurant-api.roles` lists the twelve customer permissions; call a customer endpoint (200) and `POST /orders/{id}/accept` (403).

## Documentation

- ADR `0008-permissions-as-keycloak-client-roles.md`, committed with this design.
- `actors.md`: the RBAC-matrix TODO becomes the catalogue table, noting that the realm file is the development baseline.
- Use-case specs: each gains the permission it requires. The sentences saying the project has no authentication (`address-management.md`, `payment-methods.md`, `view-order-history.md`, `accept-reject-order.md`) are corrected: tokens are now required, but the caller's identity still comes from the parameter until part 2.
- `CLAUDE.md` › Authentication: tokens are required, with the public list; a new endpoint needs `@PreAuthorize`, a row in `EndpointAuthorizationTest` and, for a new permission, a client role in the realm file; tests send `jwts.tokenFor(sub, "CUSTOMER")`; `admin@example.com` exists.
- The roadmap: part 4 done, its questions moved to decided, and the questions below added.

## Questions handed on

- **Part 2:** may `ADMIN` bypass ownership guards? Today they still apply, so an admin acting on another customer's order is refused. Does an admin get a `users` row? `GET /orders/{orderId}` has no ownership guard at all. How does a restaurant owner's order history resolve?
- **Part 3:** should `default-roles-restaurant` give `CUSTOMER` to every sign-up? `POST /api/v1/customers` is public until then.
- **Later:** a cashier needs a staff table linking users to the restaurant they work at before the role can pass ownership guards. Whether to publish or switch off `/.well-known/oauth-protected-resource`.

## Rejected alternatives

- **Realm roles only, checked with `hasRole`.** Every endpoint shared by several roles repeats `hasAnyRole(...)`, and a new role means editing each of them.
- **Permissions mapped from roles in our code.** Keeps the matrix in git, but creating a role needs a deploy, which is the use-case this part serves.
- **OAuth scopes (`SCOPE_*`).** A scope says what a client may do on a user's behalf, not what the user is.
- **A roles and permissions table of our own.** Rebuilds Keycloak's console; contradicts ADR 0007.
- **URL matchers in `SecurityConfig`.** Paths typed twice drift silently; a renamed path falls through to `authenticated()`.
- **One all-permissions token for the existing tests.** Every test would run as a superuser, and the roles' compositions would be tested only by the matrix.
- **`@WithMockUser` or security switched off in tests.** A mock security context never reaches the server thread of a `RANDOM_PORT` test.
- **Permission sets copied by hand into the tests.** They would drift from the realm file unnoticed.

## Out of scope

- Replacing `customerId`, `restaurantId` and `role` with the token's identity (part 2).
- Registration and OTP (part 3).
- Cashier, courier and support roles.
- Production Keycloak.
