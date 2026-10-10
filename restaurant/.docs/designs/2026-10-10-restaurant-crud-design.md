# Restaurant CRUD — design

**Issue:** #85 (Restaurant CRUD), sub-issues #86–#91. #101 (menu item images) is out of scope.

The issues carry titles only. This design settles what each endpoint does, who may call it, and how the work splits into reviewable PRs (each under ~1000 lines, code + tests + docs).

## PR stack

Four stacked PRs. Each branches from the previous one and targets it; PR 1 targets `main`.

| PR | Branch | Issues | Endpoints | Est. size | Status |
|---|---|---|---|---|---|
| 1 — read | `feat/GH-89-read-restaurants` | #89, #88 | `GET /api/v1/restaurants/{id}`, `GET /api/v1/restaurants` | ~700 | **Done** — #111. Both reads, specs, ADR 0007 |
| 2 — create | `feat/GH-86-create-restaurant` | #86 | `POST /api/v1/restaurants` | ~750 | **Done** — #112. Create, spec, ADR 0005 amended, `ActorRole` in `user/` with `ADMIN` |
| 3 — edit and open | `feat/GH-87-edit-restaurant` | #87, #91 | `PATCH /api/v1/restaurants/{id}`, `PUT /api/v1/restaurants/{id}/open` | ~900 | **Done** — #113. Edit, open/close, checkout guard, specs, glossary |
| 4 — delete | `feat/GH-90-delete-restaurant` | #90 | `DELETE /api/v1/restaurants/{id}` | ~700 | **Done** — #114. Delete, add-to-cart guard, spec. Last PR of the stack |

Reads go first: they build what the others need (controller, response, mapper, test support) and carry no write rules. If PR 3 runs over budget, #91 moves to its own PR.

**Status** is updated by each PR before it is opened, so a session with no history can tell which PR is next.

## Authorisation

Trust-based, like the order endpoints, until #83 delivers real authentication. The caller declares who they are; the service checks it.

- `ActorRole` gains `ADMIN` and moves from `order/model/entity/` to `user/model/` (PR 2, mechanical). It is no longer order-specific, and the restaurant domain should not import an enum from an order entity package.
- Write endpoints take `?role=`, plus `?restaurantId=` for `RESTAURANT` (the caller's claimed identity, the same parameter the order endpoints use).
- One private guard, `ensureMayManage(id, role, restaurantId)`. PR 2 adds the `ADMIN`-only half, `ensureAdmin(role, action)`, for create; PR 3 adds `ensureMayManage` beside it, and PR 4 reuses `ensureAdmin` for delete:

| Endpoint | Allowed |
|---|---|
| `GET` details, `GET` index | anyone, no role parameter |
| `POST` create | `ADMIN` |
| `PATCH` edit, `PUT` open | `ADMIN`, or `RESTAURANT` with `restaurantId == {id}` |
| `DELETE` | `ADMIN` |

- Anything else is **403**, "Role X may not …" (`RestaurantActionNotAllowedException extends RestaurantException`). A `RESTAURANT` caller without `restaurantId` counts as a mismatch.
- **Order of checks on writes:** role (403, no database read) → existence (404) → business rule (409). An unauthorised caller cannot probe which ids exist.
- Order endpoints are unchanged. `ADMIN` passed to them is already refused, because no `OrderTransition` belongs to it.
- Every spec has an **Authorisation** section naming the IDOR and pointing to #83, like the customer specs.

Rejected: a `users.user_role` column (nothing would read it until #83, which may model roles differently), and building real authentication here (#83's open decisions belong to its owner).

## Data model

**No migration.** `restaurants` (name, description, `is_open`, `user_id`) and `users` (name, email, password, `user_deleted_at`, the partial unique email index `uq_users_active_email`) already hold everything. A restaurant is a login account like a customer: `users` ← `restaurants.user_id`.

`Restaurant` gets `@Builder` and `@AllArgsConstructor(access = AccessLevel.PRIVATE)` in PR 2; ADR 0005 and `CLAUDE.md` move it out of the "never created" list. `Menu` and `MenuItem` stay there until the menu CRUD (#92).

## Lookups

`RestaurantRepository.findActiveById` fetches the user and filters `user.userDeletedAt is null`. Unknown and soft-deleted restaurants are both **404**, "Restaurant not found" (`RestaurantNotFoundException`). The filter applies from PR 1, so PR 4 does not revisit the reads.

## Endpoints

### `RestaurantResponse`

Every endpoint returns this shape (snake_case, the global Jackson setting):

```json
{ "restaurant_id": 4, "name": "Koshary Corner", "description": "…", "email": "contact@koshary.example.com", "is_open": false }
```

The password is never returned. Menus are not included: they belong to the menu CRUD (#96, "Get Restaurant Menu Items").

### Details — `GET /api/v1/restaurants/{id}` (PR 1, #89)

**200** with `RestaurantResponse`, or 404.

### Index — `GET /api/v1/restaurants?page=0&size=20` (PR 1, #88)

Offset paging, entirely Spring's: `Pageable` in, `PagedModel` out.

- The controller takes `@PageableDefault(size = 20) Pageable`. The service rebuilds it as `PageRequest.of(page, size, Sort.by("id"))`, so a client's `?sort=` is **ignored**; the order is always id ascending.
- `spring.data.web.pageable.max-page-size=50`, matching order history's cap. Spring **clamps** rather than rejecting: `size=500` returns 50 rows, a negative page is page 0. The spec states this; no hand-written validation.
- `Page<Restaurant> findByUser_UserDeletedAtIsNull(Pageable)` with `@EntityGraph(attributePaths = "user")`, because the email lives on `users`. Spring derives the count query.
- Response, `new PagedModel<>(page.map(mapper::toResponse))`:

  ```json
  { "content": [ {…} ], "page": { "size": 20, "number": 0, "total_elements": 3, "total_pages": 1 } }
  ```

  The metadata names follow the snake_case setting (`total_elements`, `total_pages`), as `ListRestaurantsEndpointTest` asserts.
- Open and closed restaurants alike. No `?open=` filter until a client needs one.

**Why offset, not keyset:** it is the only option built in end to end (Spring Data has no web resolver or JSON form for a keyset position, so every keyset endpoint needs its own cursor code). Keyset's advantages — deep pages, stability under constant inserts — matter for order history, not for a small, admin-curated directory. Recorded as ADR 0007 in PR 1. Order history's cursor is revisited separately in #110.

### Create — `POST /api/v1/restaurants?role=ADMIN` (PR 2, #86)

The System Admin creates the restaurant; it has no usable login.

| Field | Rule |
|---|---|
| `name` | required, not blank, max 150 (`user_name`'s limit; stored in both `user_name` and `restaurant_name`) |
| `description` | optional |
| `email` | required, valid email, max 255 — the contact email |

`RestaurantService.createRestaurant`, one transaction:

1. Trim and lower-case the email. Taken by an active user (`UserService.isEmailTaken`) → **409**, "Email is already in use" (`RestaurantEmailInUseException`).
2. Build the `User`: `user_name` = the restaurant name, the email, `user_password` = `UserService.NO_LOGIN_PASSWORD` (a fixed marker such as `"!no-login"`). It is not a BCrypt hash, so no password can match it. The constant marks the accounts #83 must give a real password.
3. `userService.create(user)`, then build and save the `Restaurant`.
4. **201** with `RestaurantResponse`.

**A new restaurant starts closed** (`is_open = false`): it has no menu yet, and the owner journey ends with "go online". The column's `DEFAULT TRUE` stays; it serves the seeds and V6's backfill.

Not stored: phone, address, opening hours. `is_open` stays a manual flag, as V6 decided.

### Edit — `PATCH /api/v1/restaurants/{id}?role=…[&restaurantId=…]` (PR 3, #87)

- `name`, `description`, `email`, all optional; absent or `null` means unchanged. As in update-customer, `description` cannot be cleared.
- `name` updates `restaurant_name` and `user_name` together.
- `email` is trimmed and lower-cased; taken by another active user (`isEmailTakenByOther`) → **409**. Re-sending the restaurant's own email is not a conflict.
- Managed entity: setters, dirty checking, no `save()`.
- **200** with `RestaurantResponse`.

### Open / close — `PUT /api/v1/restaurants/{id}/open?role=…[&restaurantId=…]` (PR 3, #91)

- Body `{"is_open": true}`, `@NotNull`. **200** with `RestaurantResponse`.
- Idempotent: setting the current value again is 200.
- Closing does not touch orders already placed: accepted and preparing orders carry on, and the PLACED auto-reject keeps its normal deadline. What closing stops is new business: add-to-cart (existing) and checkout (below).

### Delete — `DELETE /api/v1/restaurants/{id}?role=ADMIN` (PR 4, #90)

Soft delete, mirroring delete-customer. **204**, no body.

1. Not `ADMIN` → **403**.
2. Unknown or already deleted → **404**, "Restaurant not found". Deleting twice is 404 the second time.
3. An order in an active status (`OrderStatus.ACTIVE`: `PLACED` through `PICKED_UP`) → **409**, "Restaurant has active orders" (`RestaurantHasActiveOrdersException`), through a new `OrderService.hasActiveRestaurantOrders(restaurantId)` over `existsByRestaurant_IdAndStatusIn`.
4. Otherwise `user_deleted_at = now()` on the restaurant's user.

**It lives in `RestaurantDeletionService`.** The active-order check needs `OrderService`, and the order domain already needs `RestaurantService` (the checkout guard below), as does cart. `RestaurantService` injecting `OrderService` would close a cycle. `RestaurantController` injects both services; nothing points back at the deletion service. Same shape as `CustomerDeletionService`.

**Kept:** past orders, ratings, menus, menu items, carts' lines.

**Afterwards:**

| Path | Result |
|---|---|
| Details, edit, open, delete | 404, "Restaurant not found" |
| Index | not listed |
| Add-to-cart of its items | 404, "Item not found": `RestaurantService.findMenuItem` joins `menu.restaurant.user` and filters `userDeletedAt` |
| Checkout of a cart holding its items | 404, "Restaurant not found" (below) |
| Its contact email | reusable by a new restaurant or customer |

Still visible on purpose: a customer's order history shows past orders with the restaurant's name, and a delivered order can still be rated.

Accepted race: an order placed between step 3 and the commit. After the commit, checkout refuses.

## Stale carts at checkout

A cart can hold items of a restaurant that has since closed or been deleted. Today create-order checks neither: `ItemsValidatorHandler` checks only emptiness and stock.

A new chain link, `RestaurantValidatorHandler`, runs after `ItemsValidatorHandler` (the cart is known to be non-empty) and before payment. A cart holds one restaurant's items (V9), so it checks that restaurant once, through a new cross-domain method `RestaurantService.ensureOrderable(restaurantId)` (no `@Transactional`; joins the caller's):

| Cart's restaurant | Result | Added in |
|---|---|---|
| closed | **409**, "Restaurant is closed" (the existing `RestaurantClosedException`, as add-to-cart answers) | PR 3 |
| soft-deleted | **404**, "Restaurant not found" | PR 3 (code: `ensureOrderable` loads through `findActiveRestaurant`), PR 4 (test) |

The cart is kept; the customer sees the refusal and empties it. A restaurant closed for the night does not wipe the carts waiting on it.

## Testing

End-to-end, one class per use-case (`RANDOM_PORT` + `RestTestClient`), happy path and every rejection. Fixtures are seeded through SQL; cleanup is scoped to the rows each test created.

| PR | Class | Rejections and fixtures |
|---|---|---|
| 1 | `GetRestaurantEndpointTest` | 404 unknown id; 404 soft-deleted (inserted, then soft-deleted) |
| 1 | `ListRestaurantsEndpointTest` | paging; `size` clamped to 50; `sort` ignored; deleted excluded. Seeds are global, so counts are relative to a SQL baseline |
| 2 | `CreateRestaurantEndpointTest` | 403 `CUSTOMER` / `RESTAURANT`; 400 per validation rule; 409 seeded email, also in different case; email reusable after soft delete; starts closed; no password in the response |
| 3 | `UpdateRestaurantEndpointTest` | 403 role; 403 `RESTAURANT` on another restaurant; 404; 409 another user's email; own email accepted; `null` leaves a field unchanged |
| 3 | `SetRestaurantOpenEndpointTest` | 403; 404; 400 missing `is_open`; idempotent |
| 3 | `CreateOrderEndpointTest` +1 | checkout with the closed restaurant's item → 409 |
| 4 | `DeleteRestaurantEndpointTest` | 403; 404, including a second delete; 409 with a `PLACED` order; reads 404 afterwards |
| 4 | `CreateOrderEndpointTest` +1, `AddCartItemEndpointTest` +1 | deleted restaurant → 404 |

`support/RestaurantEndpointTestSupport`, new in PR 1: `insertRestaurant(name, email, isOpen)` (users and restaurants rows), `softDeleteRestaurant`, and cleanup by email prefix. `insertMenuItem` arrives with the first PR that uses it.

## Documentation

Specs go in `restaurant/.docs/use-cases/restaurant-management/<use-case>/<use-case>.md`, from `_use-case-template.md`, with an inline Mermaid flowchart and sequence diagram.

| PR | Docs |
|---|---|
| 1 | Specs `get-restaurant`, `list-restaurants`; a "Restaurant Management" section in `use-cases.md`, tracked under #85; ADR 0007 (offset by default, keyset for deep append-heavy lists; links #110); `CLAUDE.md`'s "`RestaurantService` has none until …" updated |
| 2 | Spec `create-restaurant`; ADR 0005 amended; `CLAUDE.md`: `ActorRole` lives in `user/` and has `ADMIN` |
| 3 | Specs `update-restaurant`, `set-restaurant-open`; `create-order.md` exception flow "restaurant closed"; `glossary.md` "Open (restaurant)" |
| 4 | Spec `delete-restaurant`; `create-order.md` exception flow "restaurant deleted"; `CLAUDE.md` names `RestaurantDeletionService` beside `CustomerDeletionService` |

## Left out

- **#101**, menu item images.
- **Real authentication and authorisation** — #83.
- **Menus in the details response** — #96.
- **Menu endpoints refusing a deleted restaurant's menus** — the menu CRUD (#92); PR 4's description hands it over.
- **Order history's cursor** — #110.
- **An `?open=` filter, opening hours, phone, address** — no client needs them yet.
