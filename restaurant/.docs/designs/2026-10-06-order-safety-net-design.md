# Order safety net — design

**Issue:** #82 (§1, the chain item in §2, the tests item in §5). First step of the #84 refactor.
**Branch:** `feat/GH-82-order-safety-net`

## Why

#84 removes the handler pattern and redraws domain boundaries. That refactor needs tests that pin
what the order endpoints do today, so a structural change cannot silently change behaviour. Today
there are none: `main` does not boot, and create-order and view-order have no tests.

This sub-project only builds that safety net. It does not change order behaviour beyond making
create-order run at all. Everything else in #82 waits until after the refactor, so it is written
once, in the new structure.

## Scope

In:

- #82 §1 — make `main` green.
- #82 §2, first item — the chain fix.
- #82 §5, first item — `CreateOrderEndpointTest` and `ViewOrderEndpointTest`.
- `CLAUDE.md` additions listed below.

Out (stay in #82): the remaining §2 bugs, §3–4 scope gaps, §5 specs, §6 tidy-ups.

## 1. Make `main` green

Baseline on 2026-10-06: every Spring test errors with
`Found more than one migration with version 12`.

1. Rename `V12__add_create_order_details.sql` to `V14__add_create_order_details.sql`, content
   unchanged. CI starts from an empty database, so this is safe there. A local database that
   already applied either V12 fails Flyway validation and must be reset
   (`docker compose down -v`). Say so in the PR description.
2. `CustomerEndpointTestSupport.insertOrder` omits `address_id`, which is `NOT NULL` since V12/V14.
   Move `addressIdForCustomer` from `OrderEndpointTestSupport` (private) to
   `CustomerEndpointTestSupport` (protected) and call it from `insertOrder`. Both support classes
   then use the one helper. This fixes `DeleteCustomerEndpointTest.rejectsACustomerWithAnActiveOrder`
   and both order tests in `DeletedCustomerGuardsEndpointTest`.

   `OrderEndpointTestSupport` does not extend `CustomerEndpointTestSupport`, so the helper is
   `protected static long addressIdForCustomer(JdbcTemplate, long customerId)` in
   `CustomerEndpointTestSupport`, and `OrderEndpointTestSupport` calls it by class name. The
   hierarchy stays as it is; no copy.
3. Record the `./mvnw -B verify` result after steps 1–2. It is the baseline for step 4.

## 2. Fix the chain

`OrderHandler.handleNext` calls `next.handleNext(request, response)`. It must call
`next.handle(request, response)`. Today only `CartValidatorHandler` runs, and
`POST /api/v1/orders` returns 201 with only `customerId` and `restaurantId`.

Once the chain runs, code that has never executed will run for the first time. **Rule:** fix only
what stops the happy path from returning 201 with a saved order. Any other broken path gets a
pinning test (below) and a new entry in #82, not a fix.

The chain is not restructured here. Sub-project B deletes it.

## 3. Tests

Both classes are end-to-end: `RestTestClient` against a random port, state arranged with SQL, side
effects checked with SQL. The only HTTP call in each test is to the endpoint under test.

### Pinning known-wrong behaviour

A test marked 🏷 below asserts what the code does **today**, even though #82 says it is wrong. It
carries a comment naming the issue section and the target behaviour:

```java
// #82 §2: should be 409, currently 403
.expectStatus().isForbidden();
```

The PR that fixes the behaviour flips the assertion and deletes the comment.

### `CreateOrderEndpointTest extends CustomerEndpointTestSupport`

Each test inserts its own customer, address and cart. It must not use the V3 seeded carts, because
today's code deletes the cart row. Cleanup is the existing email-prefix delete on `users`, which
cascades to customers, carts, cart items, addresses, orders, order items and transactions.
Out-of-stock asks for quantity 999 of an item stocked at 50, so no shared `menu_items` row is
changed.

| Test | Asserts |
|---|---|
| `placesACashOrder` | 201; body status `PLACED`, customer, restaurant and address ids, items, total = Σ qty × price; `orders` and `order_items` rows exist |
| `placesACardOrder` | 201; a `transaction` row with status `PAID` linked to the order; transaction present in the response |
| `deletesTheCartRow` 🏷 | the cart row is gone. #82 §2: keep the row, delete the items |
| `leavesStockUntouched` 🏷 | `menu_item_stock` unchanged. #82 §2: decrement |
| `takesTheRestaurantFromTheCart` 🏷 | a `restaurantId` that differs from the cart's restaurant is ignored; the order belongs to the cart's restaurant. #82 §2: reject |
| `treatsAMissingPaymentMethodAsCash` 🏷 | 201, no transaction. #82 §2: 400 |
| `acceptsASoftDeletedCustomer` 🏷 | 201. #82 §2: refuse |
| `refusesAQuantityAboveStock` 🏷 | 403. #82 §2: 409 |
| `refusesAnEmptyCart` 🏷 | 409. #82 §2: 400 |
| `refusesAnUnknownCart` | 404 |
| `refusesAnUnknownAddress` | 404 |
| `refusesAnotherCustomersCart` | 404 |
| `refusesAnotherCustomersAddress` | 404 |
| `refusesMissingRequiredFields` | 400 for each of `cartId`, `addressId`, `customerId`, `restaurantId` missing |

Every rejection test also asserts that the customer has no `orders` row.

If the fixed chain shows that one of these rows behaves differently from the table, the test pins
the actual behaviour with a 🏷 comment, and #82 gets the finding.

### `ViewOrderEndpointTest extends OrderEndpointTestSupport`

Uses the existing `seedOrder` and `seedOrderLine`.

| Test | Asserts |
|---|---|
| `returnsTheOrderWithItsLines` | 200; id, status, total, address id, items |
| `omitsTheTransactionForACashOrder` | `transaction_response` is null |
| `returnsAnyOrderWithoutAnOwnershipCheck` 🏷 | 200 with no customer in the request. #82 §4: 403 for a non-owner |
| `refusesAnUnknownOrder` | 404 |

### New helpers

In `CustomerEndpointTestSupport`, the base for tests that own their customers:

- `addressIdForCustomer(jdbcTemplate, customerId)`, moved there in step 1.
- `insertCart(customerId)` returns the cart id.
- `insertCartItem(cartId, menuItemId, quantity)` copies the menu item's current price into the
  cart item.
- `transactionFor(orderId)` returns the transaction row, or empty.

No private copies in test classes.

## 4. `CLAUDE.md` additions

- **Database:** migration numbers are claimed at merge time, not branch time. Before merging,
  check `main` for the same version. Two `V12`s stopped Flyway from starting and failed every
  Spring test (#82). Renaming an applied migration forces anyone who ran it to reset their local
  database.
- **Testing:** pinning tests for known-wrong behaviour, with the comment format above. A refactor
  needs tests of what the code does, so a structural change cannot quietly change behaviour; the
  comments keep the gap list searchable (`grep -rn "#82"`).
- **Testing:** tests that own their customers seed carts and addresses through
  `CustomerEndpointTestSupport` too.

## Done when

1. `./mvnw -B verify` passes. The PR records the baseline before and after.
2. `spotless:apply` has been run with JDK 21.
3. #82 boxes ticked: §1 (both), §2 chain, §5 tests.
4. New findings from running the chain are added to #82.
5. The PR links #84 as the first step of the refactor.
