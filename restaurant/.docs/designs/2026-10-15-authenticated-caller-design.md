# #83 part 2 — The authenticated caller

Part 2 of `2026-10-14-auth-roadmap.md` (#119), built on part 4 (`2026-10-14-roles-and-permissions-design.md`). Part 4 decided what *kind* of user may call an endpoint; the caller still named themselves through `customerId`, `restaurantId` and `role` parameters, so anyone holding a permission could act as anyone. Part 2 takes the caller's identity from the token instead. ADR 0009 records how.

## Decisions

- **The token carries only Keycloak's identity.** After Spring verifies it, the API resolves `sub` → `users.user_keycloak_id` → the user and, if there is one, their customer, once per request. Controllers receive the result as a `Caller`. No custom claims, no realm change.
- **A customer never names themselves.** `customerId` leaves every path, query string and request body; `/api/v1/customers/{customerId}` becomes `/api/v1/customers/me`. Services take the customer id from the `Caller`. This absorbs #123.
- **A restaurant owner still names the restaurant.** `?restaurantId` stays on the order-status endpoints, because an owner may one day have several restaurants and a cashier will act for one through a staff table. `RestaurantService.ensureOwnedBy(restaurantId, userId)` checks it with one query on a new `RestaurantRepository` — `existsByIdAndUser_Id`, i.e. `restaurant.id = :restaurantId and restaurant.user.id = :userId` — before `OrderStatusService` looks at the order; the existing check that the order belongs to that restaurant is unchanged.
- **`?role` is gone.** Each controller method passes its fixed `ActorRole`: `RESTAURANT` for accept, reject, preparing and ready-for-pickup; `CUSTOMER` for cancel. The services keep their `ActorRole` parameter, which the scheduler's `SYSTEM` auto-reject still uses.
- **Ownership is part of the lookup, not a separate step.** Each use-case's own query carries the caller (`findByIdAndCustomer_Id(addressId, customerId)`), and an empty result throws. Nothing runs a query only to prove the caller exists: holding a resolvable token proves it.
- **`ADMIN` bypasses nothing.** Guards apply to every caller. Acting on someone else's data waits for dedicated endpoints with their own permission, when a use-case asks for them.
- **The admin gets a `users` row,** linked to its Keycloak user, with no customer or restaurant.

## Resolving the caller

**`user/model/Caller`** — `record Caller(Long userId, Long customerId)`; `customerId` is null for a user who is not a customer.

**`UserRepository.findCallerByKeycloakId(UUID)`** — a JPQL constructor projection: `users` left-joined to `customers`, matched on the unique `user_keycloak_id`. **`UserService.findCaller(UUID)`** wraps it, `@Transactional(readOnly = true)` because it runs from the security filter, outside any use-case transaction, and returns `Optional<Caller>`.

It does **not** filter soft-deleted users. A customer deleted while their token is still valid resolves, then meets the soft-delete filters the use-cases already apply and gets today's 404. Filtering here would turn that into a 401.

**`config/CallerAuthenticationConverter`** — a `@Component` implementing `Converter<Jwt, AbstractAuthenticationToken>`, injecting `UserService` (a service, never the repository):

1. resolves the `Caller` from `jwt.getSubject()`; an unknown `sub` throws `InvalidBearerTokenException("Unknown user")`, which the bearer-token entry point turns into 401 with `WWW-Authenticate: Bearer`;
2. maps permissions with `KeycloakPermissionsConverter`, unchanged;
3. returns a `CallerAuthenticationToken` — an `AbstractAuthenticationToken` whose principal is the `Caller` and whose credentials are the `Jwt`.

`SecurityConfig` passes this bean to `jwtAuthenticationConverter` in place of the inline `JwtAuthenticationConverter`. A public endpoint called without a token never reaches it.

**Controllers** take `@AuthenticationPrincipal Caller caller` and pass `caller.customerId()` or `caller.userId()` on. Services take `Long` ids as today; they never see the `Jwt`.

**A caller who is not a customer** — the admin, or a Keycloak user linked to a non-customer row — calling a customer-side endpoint gets 403 `NotACustomerException` ("Caller is not a customer", extends `CustomerException`). The permission check already keeps restaurant owners out; this covers the admin, who holds every permission. Where the null check lives (one public method on `CustomerService` or a check in each customer-side service) is settled in the plan.

No cache: one indexed lookup per authenticated request. A cache would need a dependency and an invalidation rule for deleted users; add one when a measurement asks for it.

## Endpoints

All paths are under `/api/v1`.

| Endpoint | Change |
|---|---|
| `GET`, `PATCH`, `DELETE /customers/{customerId}`; `PUT /customers/{customerId}/password` | Become `/customers/me` |
| `POST`, `GET /addresses`; `PUT`, `DELETE /addresses/{addressId}`; `PUT /addresses/{addressId}/default` | `?customerId` removed |
| `POST`, `GET /payment-methods`; `DELETE /payment-methods/{id}`; `PUT /payment-methods/{id}/default` | `?customerId` removed |
| `POST /cart/items` | `customerId` removed from `AddCartItemRequest` |
| `PUT /cart/{cartId}/items/{cartItemId}`, `GET`, `DELETE /cart/{cartId}`, `DELETE /cart/{cartId}/items`, `POST /cart/{cartId}/checkout` | `{cartId}` stays (it names a resource, not the caller); every lookup is scoped to `cart.id` **and** the caller's customer. These had no ownership check at all |
| `POST /orders` | `customerId` removed from `CreateOrderRequest`; the create-order chain receives the caller's customer id alongside the request |
| `GET /orders/{orderId}` | Scoped: `order.customer.id = :customerId or order.restaurant.user.id = :userId`. It had no ownership check at all |
| `GET /orders` | `?customerId` and `?role` removed; `OrderHistoryService.ensureCustomerRole` deleted. Customer-only: a restaurant's view of its orders is a separate use-case |
| `POST /orders/{orderId}/rating` | `customerId` removed from `RateOrderRequest` |
| `POST /orders/{orderId}/cancel` | `?customerId` and `?role` removed; the controller passes `CUSTOMER` |
| `POST /orders/{orderId}/accept`, `/reject`, `/preparing`, `/ready-for-pickup` | `?role` removed, the controller passes `RESTAURANT`; `?restaurantId` checked against the caller; `refuseSystemRole` deleted |

Unchanged: `POST /customers` (public), health, Swagger. The scheduler's `autoRejectStaleOrders` calls `reject(…, SYSTEM, …)` with no caller and no restaurant check.

Lookups other domains call (`findActiveCustomer`, `decrementStock`, …) keep their signatures; the entry use-case has already scoped the request. Existing soft-delete checks stay where they are: they are the rule that a deleted customer does not exist to the API, not a check that the caller exists.

## Statuses

| Situation | Status |
|---|---|
| No token, or invalid | 401 (unchanged) |
| Valid token, `sub` matches no `users` row | 401, `WWW-Authenticate: Bearer` |
| Valid token without the permission | 403 `Access denied` (unchanged) |
| Customer-side endpoint, caller is not a customer | 403 `NotACustomerException` |
| `?restaurantId` the caller does not own (or that does not exist) | 403 `RestaurantAccessDeniedException` (new, extends `RestaurantException`) |
| Customer-side lookup scoped to the caller finds nothing — the thing does not exist, or it is someone else's | 404, the lookup's existing not-found exception |
| Order belongs to another restaurant than `?restaurantId` | 403 `OrderNotOwnedException` (unchanged) |
| Customer soft-deleted, token still valid | 404 (unchanged) |

Once the caller is in the query, "missing" and "someone else's" cannot be told apart, so the customer side answers both with 404. `AddressAccessDeniedException`, `PaymentMethodAccessDeniedException` and the customer-side uses of `OrderNotOwnedException` (view, cancel, rate) are removed; their 403s become 404s. The restaurant side keeps 403, because the request names a restaurant and the caller is told it is not theirs.

## The admin row

`V16__seed_admin_user.sql` inserts a `users` row — name `Administrator`, `admin@example.com`, the V2 placeholder-password style, `user_keycloak_id = '00000000-0000-4000-a000-000000000001'` (its id in `keycloak/realm-restaurant.json`) — **without an explicit `user_id`**, so the identity column assigns it and needs no resync. No customer or restaurant row points at it, so it resolves to `Caller(id, null)`. Check `main` for the version number before merging.

The realm file does not change.

## Testing

**Tokens name real users.** `jwts.as(client, role)` with a random `sub` would now get 401. Instead:

- `insertCustomer(email)` writes a random `user_keycloak_id` with the user row;
- the support classes gain `asCustomer(customerId)` and `asRestaurantOwner(restaurantId)`, which read that row's `user_keycloak_id` and return `client` sending a `CUSTOMER` or `RESTAURANT_OWNER` token with it as `sub`, and `asUser(keycloakId, realmRole)` underneath both;
- seeded actors resolve through their V15 ids;
- the class-wide `sendToken()` default goes; each test says whom it acts as. `realmRole()` remains only for create-customer's `null`.

**Address and cart tests stop arranging over HTTP** (`CLAUDE.md` › Testing): they are rewritten here anyway, since every call now needs its actor's token. They move to the support helpers in their domain's commit.

**Assertion changes**, all intended:

- another customer's address, payment method or order (view, cancel, rate): 403 → 404;
- another customer's `cartId`, on each of the five cart endpoints: 404 (new);
- `GET /orders/{orderId}`: read by its customer, read by its restaurant's owner, 404 for another customer and for another restaurant's owner (new);
- a `?restaurantId` the caller does not own: 403 (new);
- tests sending a wrong `?role` or `SYSTEM`: deleted with the parameter.

**`config/CallerResolutionEndpointTest`:** a valid token whose `sub` has no `users` row → 401 with `WWW-Authenticate: Bearer`; the admin's `sub` with `customer.read` → 403 on `GET /customers/me`, which also proves V16 links the admin; a soft-deleted customer's own token → 404 on `GET /customers/me`.

**`EndpointAuthorizationTest`:** rows move to the new paths and parameters. Every token carries **the admin's `sub`**: it resolves, but the admin is not a customer and owns no restaurant, so a permitted request ends in our own 403 or a 404 and no seeded row changes. The third check (not 401, not `Access denied`) holds; the coverage tests are unchanged.

**Manual check, in the PR description**, after starting the stack: log in to Swagger UI as `ahmed.ali@example.com`, `GET /customers/me` → 200; as `admin@example.com` → 403; create a user in the Keycloak console with no `users` row, call any protected endpoint → 401.

## Documentation

- ADR `0009-caller-resolved-after-verification.md`, committed with this design.
- Use-case specs: new paths and parameters, `?role` gone, the 403s that became 404s, the new restaurant and cart checks; the sentences saying the caller's identity still comes from a parameter are removed.
- `CLAUDE.md` › Authentication: controllers take `@AuthenticationPrincipal Caller`; customer-side queries are scoped by `caller.customerId()` and requests never carry it; restaurant-side requests keep `?restaurantId`, checked against `caller.userId()`; unknown `sub` → 401; tests act through `asCustomer` / `asRestaurantOwner`; the admin has a `users` row.
- The roadmap: part 2 done, its questions moved to decided, the questions below added.

## Delivery

Branch `feat/GH-119-authenticated-caller`, from `feat/GH-83-roles`; one PR based on it, closing #119 and #123. Commits, each green: design and ADR → groundwork (`Caller`, converter, V16, test support, `CallerResolutionEndpointTest`) → customer and addresses → payment methods → cart → orders → docs → formatting.

## Questions handed on

- **Part 3:** a Keycloak user with no `users` row gets 401. Sign-up decides how the row appears — the API calling Keycloak's admin API, or get-or-create on the first token, as `wisemen/nestjs-project-template` does. `users.user_keycloak_id` is the link either way.
- **Later:** a restaurant's view of its orders (`GET /orders` is customer-only); admin endpoints acting on other users' data; recording which user performed an order transition (`order_status_history` stores only `actor_role`).

## Rejected alternatives

- **Keep `/{customerId}` and the `customerId` parameters, guarded against the caller.** Every request carries an id the server already knows, and each endpoint needs a guard that can be forgotten.
- **`user_id` and `customer_id` as custom claims, from Keycloak user attributes.** Two sources of truth that sign-up must keep in step before the first token; Keycloak 26's declarative user profile must be restated in the realm file; an attribute left user-editable would let anyone become anyone.
- **Resolve lazily per controller argument, or pass `sub` to the services.** Every use-case would join through `users` again, and the services would learn about Keycloak ids.
- **A separate existence or ownership query before each use-case.** The use-case's own lookup can carry the caller; a second query only adds a round trip.
- **`ADMIN` bypasses ownership**, by role or through an `ownership.bypass` permission. Every guard grows a second branch, a leaked admin token acts as anyone, and the order history would record an admin as `RESTAURANT`.
- **Keep `?role` and check it against the permissions.** The server already knows it; the caller could only be wrong.
- **Derive the restaurant from the token.** Breaks when an owner has two restaurants or staff exist.
- **Restaurant order history behind `GET /orders`.** A new use-case, with its own filters and fields, does not belong in an identity change.

## Out of scope

- Restaurant order list, admin endpoints, an audit of which user acted.
- Sign-up, OTP, the fate of `user_password` and the change-password endpoint (part 3).
- A cache for the caller lookup.
