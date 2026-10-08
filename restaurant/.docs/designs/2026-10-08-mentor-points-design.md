# The mentor's smaller points — design

**Issue:** #84 (sub-project D of four: A safety net → B handlers → C domain boundaries → **D the mentor's smaller points**).

The mentorship review annotated a copy of the repo (`mentorship-restaurant-main.rar`, linked from #84). A–C covered its large points. This design settles the rest: which suggestions D applies, which it declines and why.

D ships as **two stacked PRs**, split like C so the mechanical diff does not bury the query changes:

| PR | Branch | Based on / targets |
|---|---|---|
| D1 — builders and names | `feat/GH-84-builders-and-names` | `feat/GH-84-domain-boundaries` (#106) |
| D2 — the two queries | `feat/GH-84-query-builtins` | `feat/GH-84-builders-and-names` |

## Decisions

| Mentor's note | Where (annotated copy) | Decision |
|---|---|---|
| `@Builder @AllArgsConstructor` on `User`; "create builder", "buider" | `User`, `CreateCustomerHandler`, `RateOrderHandler` | **Applied, everywhere.** Builders on every entity the application creates (D1) |
| `rate` → `rateOrder`, `viewOrder` → `viewOrderDetails` | order controller, service, handlers | **Applied.** `viewOrderDetails` landed in B; `rateOrder` in D1 |
| `OrderStatus.getActiveOrderStatus()` | `OrderStatus`, `DeleteCustomerHandler` | **Applied** as a constant, `OrderStatus.ACTIVE` (D1) |
| "can be replaced with one query [native query]" | `RateOrderHandler.rateOrder` | **Applied** as one JPQL projection, not native SQL (D2) |
| "JPA Pagination" | `ViewOrderHistoryHandler` | **Applied** with Spring Data's Scroll API (D2) |
| `/password` → `/changePassword`; `// 203` | `CustomerController` | **Declined** |
| `CreateCustomerRequest` → `CustomerRequest`; `customerName`, `customerEmail`, `phoneNumber` | `CreateCustomerRequest` | **Declined** |
| `is_deleted = 1,0` | `DeleteCustomerHandler` | **Declined** |
| `cartRepository.deleteByCustomer_Id // no need` | `DeleteCustomerHandler` | Applied in C2: the cart is kept (#104) |
| `isCustomerEmailExists` returning `boolean` while throwing | `UpdateCustomerHandler` | Declined in B's roadmap: misleading; `ensureXxx()` stays |

### Why the three declines

- **`PUT /customers/{id}/password` stays.** The verb already says "change"; every other path in the API is a noun (`/addresses`, `/payment-methods`, `/rating`, `/{id}/default`); renaming breaks callers for no gain. The endpoint returns **204 No Content**, which is right: 203 means "Non-Authoritative Information", a proxy's answer, not "changed".
- **`CreateCustomerRequest` and `UpdateCustomerRequest` stay, with their field names.** Create requires `name`, `email` and `password`; update has no password and treats every field as optional, rejecting only a present-but-blank value. One `CustomerRequest` would need validation groups on every field and a password field that exists only on create. Addresses use the same pair (`AddAddressRequest`, `UpdateAddressRequest`). The field names are the JSON contract (`name`, `email`, `phone`); renaming them changes the API for every client.
- **`users.user_deleted_at` stays; no boolean flag.** A null check is already the flag, and the timestamp also says *when*, which a boolean loses. The partial unique index `uq_users_active_email` (`WHERE user_deleted_at IS NULL`) and every active-customer query are built on it. A boolean means a migration, a rebuilt index and every query rewritten, for less information.

## D1 — builders and names

### Builders on entities

The 12 entities: the 11 the application creates (`User`, `Customer`, `Address`, `PaymentMethod`, `Cart`, `CartItem`, `Order`, `OrderItem`, `OrderRating`, `OrderStatusHistory`, `Transaction`) and `OrderTracking`, which already carries an unused `@Builder`. Each gets:

```java
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // Hibernate only
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // the builder only
```

- **Initialised fields get `@Builder.Default`**, or the builder silently leaves them null: `Customer.addresses`, `Customer.paymentMethods`, `Cart.items`, `Order.items`.
- **Every `new Entity()` plus setters becomes `Entity.builder()…build()`**, in services, mappers and `PaymentProcessor`. `Transaction` and `OrderTracking` lose their public all-args constructors.
- **The no-args constructor is protected everywhere.** Most entities have a public one today, though `CLAUDE.md` calls protected the default; builders remove the only reason to widen it, so the `Cart`/`CartItem` exception goes.
- **Setters stay.** Updates rely on dirty checking (`setStatus`, `setUserDeletedAt`, …). The builder is for creating an entity, setters for changing one.

### Names

- `OrderRatingController.rate` and `OrderRatingService.rate` become `rateOrder`. The URL is unchanged.
- `OrderStatus.ACTIVE`: a `public static final Set<OrderStatus>`, an `EnumSet` derived from `isTerminal()` once, in the enum. `OrderService`'s private `ACTIVE_ORDER_STATUSES` goes.

### Behaviour

None. 177 tests pass and no test file changes.

## D2 — the two queries

### Rating: one query instead of two

Today rating reads twice before it writes: `findByIdWithOwner` (order, customer, user) then `existsByOrderId`. D2 reads once, with an interface projection beside `OrderOwnerProjection`, the pattern cancel already uses:

```java
public interface OrderRatingContext {
  Long getCustomerId();
  OffsetDateTime getCustomerDeletedAt();   // null while active
  OrderStatus getStatus();
  boolean isRated();
}

@Query("""
    select c.id as customerId, u.userDeletedAt as customerDeletedAt,
           o.status as status, case when r.id is null then false else true end as rated
    from Order o join o.customer c join c.user u
    left join OrderRating r on r.order = o
    where o.id = :orderId
    """)
Optional<OrderRatingContext> findRatingContextById(@Param("orderId") Long orderId);
```

`rateOrder` runs today's checks in today's order on that row: empty → 404 "Order not found"; `orderService.ensureOwnedBy` → 403; `orderService.ensureOwnerActive` → 404; not `DELIVERED` → `OrderNotDeliveredException`; rated → 409 "Order is already rated". The rating is built with `orderRepository.getReferenceById(orderId)`, a proxy that issues no SELECT; `OrderRatingMapper` reads only the order's id, which the proxy holds. `saveAndFlush` and its unique-constraint catch stay as the backstop for two concurrent ratings.

`OrderRepository.findByIdWithOwner` and `OrderRatingRepository.existsByOrderId` are deleted; rating was their only caller.

**Why JPQL, not native.** One native `INSERT … SELECT … WHERE <every rule holds>` would be a single statement, but when it inserts nothing it cannot say which rule failed, and the follow-up query to choose the error brings the round trip back. The JPQL projection keeps every rule in Java and every error as it is.

### Order history: Spring Data's Scroll API

The two hand-written keyset queries (`findFirstPage`, `findPageAfter`) and `paging/KeysetPage` give way to the Scroll API. Scrolling works only with derived query methods, Query-by-Example and Querydsl ("Scrolling with String-based query methods is not yet supported"), so the query becomes derived and returns entities:

```java
@EntityGraph(attributePaths = "restaurant")
Window<Order> findByCustomer_IdOrderByCreatedAtDescIdDesc(
    Long customerId, ScrollPosition position, Limit limit);
```

Spring writes the keyset condition, over-fetches one row to answer `hasNext()`, and keeps `createdAt desc, id desc`. **The plan's first step verifies** that a derived `Window` method takes `Limit` together with `@EntityGraph` on Spring Data JPA 4.1; the docs show the parts, not this combination. The fallback is the fluent `findBy(specification, q -> q.limit(…).sortBy(…).scroll(…))` through `JpaSpecificationExecutor`, still built in.

`OrderHistoryService.viewOrderHistory` keeps its checks (role, customer exists, cursor usable), then:

- **Cursor in:** none → `ScrollPosition.keyset()`; given → `ScrollPosition.forward(Map.of("createdAt", cursor.getCreatedAt(), "id", cursor.getOrderId()))`.
- **Rows:** each `Order` maps to `OrderSummaryResponse` in Java. `status.name()` equals today's `cast(o.status as string)` because the column is `EnumType.STRING`; restaurant name, total and `createdAt` come from the entity.
- **Item counts:** one grouped query per page in `OrderItemRepository`, `select oi.order.id, count(oi) from OrderItem oi where oi.order.id in :orderIds group by oi.order.id`. It is the one piece the Scroll API cannot express (today it is a correlated subquery in the summary). A page becomes two SELECTs instead of one. Rejected: a `countBy` per row (N+1), `@Formula` on `Order` (a query on an entity), loading `items` to call `size()` (reads every line to count it).
- **Cursor out:** if `window.hasNext()` and the page is not empty, the last row's `createdAt` and `id`; otherwise `next_cursor` is null. The same values as today.

**The API is unchanged:** the `cursor.createdAt` / `cursor.orderId` parameters, the `next_cursor` shape, the ordering, the error cases.

### Behaviour

None visible over HTTP. The SQL changes: rating issues one SELECT then the INSERT (checked once in Hibernate's SQL log and reported in the PR); a history page issues two SELECTs. 177 tests pass and no test file changes.

## Verification

- Each PR: `./mvnw -B clean verify` passes 177 tests, with **no test file changed**. A failing test means behaviour changed: stop and report.
- D2: the order-history tests pin page boundaries, `createdAt` ties, an empty history, cursor validation and the soft-deleted 404; the rating tests pin every rejection and the happy path.
- Both: `spotless:apply` with JDK ≤ 21 on the files the PR touches; one commit per concern, each passing `verify`.

## Docs

**D1**

- `CLAUDE.md`: entities get builders for creation and setters for updates, `@Builder.Default` on initialised fields, a protected no-args constructor everywhere (the `Cart`/`CartItem` exception goes); "no factories" now means no hand-written factory methods.
- ADR `0005-builders-on-entities.md`: the choice, the `@Builder.Default` trap, the constructor access.

**D2**

- `view-order-history.md`: data access (the derived `Window` method, the grouped count), the sequence diagram, structure. Rating has no use-case spec; none is created.
- ADR `0006-keyset-paging-with-the-scroll-api.md`: the Scroll API over hand-written keyset queries, the string-query limitation that forces a derived method, and the grouped count it costs.
- The roadmap: D's status and PRs.

## Out of scope

- Authentication and #104's cart-by-id gap.
- Any other `@Query` method; only history moves to the Scroll API.
- Offset paging, total counts, or backward scrolling for history.

## Done when

1. D1 and D2 pass `./mvnw -B clean verify` with 177 tests and no test file changed.
2. Rating issues one SELECT before its INSERT; history pages through `Window`.
3. `CLAUDE.md`, ADRs 0005 and 0006, the history spec and the roadmap are updated.
4. Both PRs are opened after the user's go-ahead.
