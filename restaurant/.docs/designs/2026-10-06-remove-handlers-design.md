# Remove the handler pattern — design

**Issue:** #84 (sub-project B of four: A safety net → **B handlers** → C domain boundaries → D naming/builders/DTOs).
**Branch:** `feat/GH-84-remove-handlers`, from `feat/GH-82-order-safety-net`. The PR targets that branch (stacked on #102) and is retargeted to `main` once #102 merges.

## Why

Every use-case is a `*Handler` class, and each domain's `*Service` only delegates to its handlers. The
service layer has no behaviour of its own, so every operation is spread over two classes. The
mentorship review of #84 asked for the conventional controller → service shape. #81 (module boundaries)
was closed on the same grounds.

## Decisions

| Decision | Choice |
|---|---|
| What replaces handlers | Layered: one service per controller, holding the logic. No vertical-slice example for now. |
| Create-order | Keeps its chain of responsibility. |
| Related services | Share their domain's directory (`order/service/` holds all four order services). |
| Moves between domains (`payment/`, `restaurant`/menu, `User`) | Not in B. Sub-project C. |
| Cross-domain repository use | Rule written down now; existing violations fixed in C (see below). |
| Public method names | Unchanged. Renames are sub-project D. |
| Auto-reject's per-order transaction | `TransactionTemplate`. |

## 1. Target structure

| Domain | Today | After |
|---|---|---|
| `cart/` | `CartService` delegates to 6 handlers | `CartService` holds the logic of all 6 |
| `customer/` | `CustomerService` (5 handlers), `AddressService` (5), `PaymentMethodService` (4) | the same three services, each with its own logic |
| `order/` | `OrderService` delegates to 9 handlers, plus `CompensateOrderHandler` | `OrderService` (create, view), `OrderStatusService` (accept, reject, cancel, preparing, ready-for-pickup, auto-reject; stock compensation as a private method), `OrderHistoryService`, `OrderRatingService` |

- **All `*/service/handler/` packages are deleted.** The only exception is the create-order chain.
- **The create-order chain** moves from `order/service/handler/createOrder/` to
  `order/service/createorder/` (lowercase, which also clears that item in #82 §6). The link classes keep their `*Handler`
  names: "handler" is chain of responsibility's own term. `CreateOrderHandler`'s setup (load the cart and
  address, build the chain, run it) moves into `OrderService.createOrder()`.
- **Controllers:**
  - `OrderStatusController`, `OrderHistoryController` and `OrderRatingController` inject their own service.
  - `OrderController` keeps `OrderService`.
  - `AutoRejectStaleOrdersJob` calls `OrderStatusService`.
- **Each public service method keeps the `@Transactional` setting its handler had**, including
  `readOnly = true` and `Propagation` values. A method that was not transactional stays that way.
- **Guards** stay private `ensureXxx()` methods. Duplicates within one service collapse into one
  private method: `ensureCartExists` and `ensureStockAvailable` in `CartService`, the
  belongs-to-customer checks in `AddressService` and `PaymentMethodService`.

### Shared guards within a domain

A guard that two or more services in the same domain need goes on that domain's **primary service**,
as a public method, and the other services inject it. Dependencies run one way only. The primary
service never injects its siblings, so Spring cannot hit a circular dependency.

| Primary | Injected by | Shared methods |
|---|---|---|
| `OrderService` | `OrderStatusService`, `OrderRatingService`. `OrderHistoryService` shares no guard; its checks (customer role, cursor, customer exists) stay private. | `ensureOwnedBy(ownerId, actorId, message)`, from rate's `ensureCustomerOwnsOrder` and status's `ensureOwnerMatches`; `ensureOwnerActive(…)`, from rate's `ensureOwnerNotDeleted` and status's `ensureCustomerNotDeleted` |
| `CustomerService` | `AddressService`, `PaymentMethodService` | `findActiveCustomer(id)`: load the active customer or throw `CustomerNotFoundException("Customer not found")`. `ensureActive(Customer)`: the owning customer is not soft-deleted. |

Rules:

- A guard moves to the primary service only once two or more services share it. A guard used by one
  service stays private there.
- Shared guards keep today's exception types and messages exactly.
- Lookups that run **different queries** stay separate even when they throw the same exception.
  Today view-order loads the full order, rate loads it with its owner, and status reads a projection.
  Merging them would change what each operation loads. `findActiveCustomer` is shared only by
  callers that run the same repository query today.

### Auto-reject

Today `AutoRejectStaleOrdersHandler` calls `RejectOrderHandler.reject()` through Spring's proxy, so each
stale order is rejected in its own transaction, and `CompensateOrderHandler.compensate()`
(`Propagation.MANDATORY`) runs inside it. Once both methods are in `OrderStatusService`,
`this.reject()` would skip the proxy: there would be no transaction, `compensate` would throw, and
every stale order would be skipped.

`autoRejectStaleOrders()` wraps each order in
`transactionTemplate.executeWithoutResult(status -> reject(...))`. The existing per-order `try/catch`
and log line stay, so one failing order still neither rolls back nor stops the others.

## 2. Verification

B changes structure only, so the tests are the contract.

1. **Baseline** on `feat/GH-82-order-safety-net`: `./mvnw -B clean verify`, 177 tests pass.
2. **Every `*EndpointTest` passes unedited.** Needing to change one means behaviour changed: stop and report.
3. **The only allowed test edit** is a test that injects a service directly. `AcceptRejectOrderEndpointTest`
   injects `OrderService` for `autoRejectStaleOrders()` and switches to `OrderStatusService`. That's an
   import and a field type; no assertion changes.
4. **One commit per domain**, each passing `./mvnw -B clean verify`, in the order cart, customer, order.
5. **End-state checks**, run from `restaurant/`:
   - `find src -path '*service/handler*'` prints nothing.
   - `grep -rln "Handler" src/main/java` lists only the `order/service/createorder/` chain classes and
     `exception/GlobalExceptionHandler.java`.
   - Within a domain, the only service-to-service injections are `* → OrderService` and
     `* → CustomerService`.
6. **No new tests.** B adds no behaviour.

## 3. Docs

### `CLAUDE.md`

- **Architecture.** Replace "one `@Service` handler per use-case" and "`CartService` only delegates"
  with:
  - One service per controller holds the logic, with `@Transactional` on its public methods and
    guards as private `ensureXxx()` methods.
  - Related services share their domain's directory.
  - A guard shared within a domain goes on the primary service (`OrderService`, `CustomerService`),
    which the others inject. One way only.
  - Chain of responsibility is fine where a use-case is a pipeline of steps; create-order is the
    example.
  - The worked examples now point at `CartService` methods (modify item, clear, checkout).
- **Layering diagram:** `Controller → Service (logic) → Repository`, with mappers and anemic entities
  unchanged.
- **New: cross-domain rule.** A service injects only its own domain's repositories. For another
  domain's data it injects that domain's **service**, never its repository: `CustomerService` injects
  `OrderService`, not `OrderRepository`. Known violations, to be fixed in sub-project C (#84) and not to
  be copied:
  - `CartService` → `CustomerRepository` (add to cart)
  - `CustomerService` → `OrderRepository`, `CartRepository` (delete customer)
  - `OrderService` → `CartRepository`, `AddressRepository` (create order; the chain's
    `OrderFinalizer` → `CartRepository`)
  - `OrderStatusService` → `MenuItemRepository` (stock compensation)
  - `OrderHistoryService` → `CustomerRepository`

  These are deferred because fixing them naively creates cycles that Spring rejects at startup:
  `CustomerService ↔ OrderService`, `CustomerService ↔ CartService`. Breaking them means deciding
  what each domain owns, which is C's job.
- **New: self-invocation.** A call to a `@Transactional` method from inside the same class skips the
  proxy, so it gets no transaction of its own. For one transaction per item, use
  `TransactionTemplate`. Auto-reject is the example.

### Elsewhere

- **ADR** `restaurant/.docs/decisions/0002-services-over-use-case-handlers.md`, following
  `_template.md`. It records why handlers went, why create-order keeps its chain, why there is no
  vertical slice yet, and the cross-domain rule with its deferral to C.
- **Use-case specs.** These 11 files under `restaurant/.docs/use-cases/` name handlers, mostly as
  sequence-diagram participants and in pseudocode: create-order, view-order-history, accept-reject-order,
  update-order-status, delete-customer, get-customer, update-customer, create-customer,
  payment-methods, address-management/sequence-diagrams, and add-cart-item/images/pseudocode.txt.
  Rename each handler to the service method that now does the work. Nothing else in them changes.
- **`README.md`:** the same rename for its one mention.

## PR

The PR is opened from `feat/GH-84-remove-handlers` into `feat/GH-82-order-safety-net` and marked as stacked on #102. Its
description gives the before/after `verify` results, the end-state check output, and the follow-ups:
C (domain boundaries, `payment/`, the violations above) and D (naming, builders, request DTOs).

## Done when

1. `./mvnw -B clean verify` passes with 177 tests and no endpoint-test edits.
2. The end-state checks pass.
3. `spotless:apply` has been run with JDK ≤ 21. Unrelated reformatting is reverted, except in files B touches.
4. CLAUDE.md, the ADR, the specs and the README are updated as above.
5. The PR is opened against `feat/GH-82-order-safety-net` after the user's go-ahead.
