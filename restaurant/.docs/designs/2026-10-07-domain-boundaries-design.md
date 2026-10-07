# Domain boundaries — design

**Issue:** #84 (sub-project C of four: A safety net → B handlers → **C domain boundaries** → D naming/builders/DTOs).
Also answers the open questions of #81 (closed). Related: #104.

C ships as **two stacked PRs**: C1 moves code to the domain that owns it, with no behaviour change; C2
fixes the cross-domain dependencies against the new layout. They are split so the renames do not
bury the logic changes.

| PR | Branch | Based on / targets |
|---|---|---|
| C1 — moves | `feat/GH-84-domain-moves` | `feat/GH-84-remove-handlers` (#103) |
| C2 — boundaries | `feat/GH-84-domain-boundaries` | `feat/GH-84-domain-moves` |

Each is retargeted down the stack as the one below it merges.

## Why

B wrote down the rule "a service injects only its own domain's repositories; for another domain's
data it injects that domain's service" and listed the violations without fixing them, because a naive
fix creates constructor-injection cycles that Spring refuses to start with. Some code also lives in
the wrong domain: restaurants and menus in `cart/`, saved cards and `User` in `customer/`, the
payment processor in a generic `support/`. Fixing the dependencies first would point some of them at
the wrong owner and need redoing once the code moved.

## Decisions

| Question | Decision |
|---|---|
| Split | Two stacked PRs: C1 moves, C2 boundaries |
| Restaurant and menu | `restaurant/`, accepting the package `com.mentorship.restaurant.restaurant`. `catalog` was rejected: nobody looking for restaurant CRUD (#85–#101) would look there. A root-package rename is out of proportion. |
| `payment/` | Owns saved cards (`PaymentMethod` and everything around it), the processor, and `Transaction` |
| `User` | Its own `user/` domain; a restaurant has a `User` too, so it is not a customer concept |
| Exceptions | Move with their subject, under a new base per domain |
| Cart on customer soft delete | **Kept.** The mentor's "no need" note on the delete. The gap this opens is #104. |
| Breaking the cycles | Delete-customer moves to its own service (`CustomerDeletionService`). Cross-domain calls are service → service. |
| Up-front "is the customer active?" checks | Stay, now called through `CustomerService` |
| Entity mappings across domains | Allowed. Only bean injection is restricted (#81's open question). |

## C1 — moves

| New domain | Moves in | From |
|---|---|---|
| `restaurant/` | `Restaurant`, `Menu`, `MenuItem`, `MenuItemRepository`; `MenuItemNotFoundException`, `RestaurantClosedException`, `OutOfStockException`, under a new `RestaurantException` | `cart/` |
| `payment/` | `PaymentMethod`, `CardBrand`, `PaymentMethodController`, `PaymentMethodService`, `PaymentMethodRepository`, `PaymentMethodMapper`, their request and response classes, the `@NotExpired` constraint and its validators, `PaymentMethodNotFoundException` and `PaymentMethodAccessDeniedException` under a new `PaymentException` | `customer/` |
| | `Transaction`, `TransactionRepository` | `order/` |
| | `PaymentProcesser`, renamed **`PaymentProcessor`** | `support/`, which is then empty and removed |
| `user/` | `User`, `Gender`, `UserRepository` | `customer/` |

Also:

- The `order.model.request.PaymentMethod` enum (CARD, CASH) becomes `PaymentType`, ending its clash
  with the entity. The Java name only: the JSON field name and values are unchanged.
- `GlobalExceptionHandler` gets an `@ExceptionHandler` for `RestaurantException` and
  `PaymentException`, reading the status off `@ResponseStatus` like the existing three. Status codes
  and messages are unchanged.
- Package and import lines only. No logic, endpoint, schema or message changes. Existing cross-domain
  repository use follows the files (e.g. `CartService` now imports `restaurant.repository.MenuItemRepository`);
  C2 fixes it.

## C2 — boundaries

### The rule

A service injects only its own domain's repositories. For another domain's data or actions it calls
that domain's **service**. Entities may still map across domains (`Order.customer`, `Cart.customer`,
`OrderItem.menuItem`): those are the schema's foreign keys, and JPQL joins through them are allowed.
What is restricted is injecting another domain's beans other than its services.

### Each violation and its fix

| Today | After C2 |
|---|---|
| `CartService → CustomerRepository` (add item: active customer) | `customerService.findActiveCustomer(id)` |
| `CartService → MenuItemRepository` (lookup; stock decrement on checkout) | `RestaurantService` (new): `findMenuItem(id)`, `decrementStock(menuItemId, quantity)` |
| `CustomerService → OrderRepository` (delete: active orders) | `CustomerDeletionService` (new) → `orderService.hasActiveOrders(customerId)` |
| `CustomerService → CartRepository` (delete the cart) | Removed: the cart is kept |
| `CustomerService → UserRepository` (email checks, create user) | `UserService` (new): `isEmailTaken(email)`, `isEmailTakenByOther(email, userId)`, `create(user)`. `CustomerService` still throws `EmailAlreadyInUseException` with today's message. |
| `OrderService → CartRepository` (load the cart); chain's `OrderFinalizer → CartRepository` (delete it after the order) | `cartService.findCart(cartId)`, `cartService.deleteCart(cartId)` |
| `OrderService → AddressRepository` (delivery address) | `addressService.findAddress(addressId)` |
| `OrderStatusService → MenuItemRepository` (restore stock) | `restaurantService.restoreStock(menuItemId, quantity)` |
| `OrderHistoryService → CustomerRepository` (customer exists) | `customerService.ensureActiveCustomerExists(id)` |
| `PaymentMethodService → CustomerRepository` (view cards; cross-domain once C1 moves it) | `customerService.findActiveCustomer(id).getPaymentMethods()`. `Customer.paymentMethods` keeps its `@OrderBy`, so the order is unchanged. `CustomerRepository.findByIdWithPaymentMethods` is deleted. |

The create-order chain's `ProcessPaymentHandler → PaymentProcessor` stays a direct injection: the
processor is a payment-domain component, not a repository.

The methods other domains call carry no `@Transactional`; they join the caller's transaction, like
B's shared guards.

### Delete-customer

`deleteCustomer` moves from `CustomerService` to `CustomerDeletionService`, which injects
`CustomerService` (to load the active customer) and `OrderService`. `CustomerController` injects both
services. Delete-customer no longer deletes the cart.

Why a separate service: delete-customer is the only customer use-case that needs order, and order
(through cart and address) needs customer. Kept inside `CustomerService`, it closes the cycle
`CustomerService → OrderService → CartService → CustomerService`. Moved out, nothing points back at it.

### Resulting graph

```text
CustomerDeletionService → CustomerService → UserService
                        → OrderService    → CartService       → CustomerService
                                                              → RestaurantService
                                          → AddressService    → CustomerService
OrderStatusService      → OrderService, RestaurantService
OrderRatingService      → OrderService
OrderHistoryService     → CustomerService
PaymentMethodService    → CustomerService
```

Customer, address, user, restaurant and payment never point at cart or order, so there is no cycle.

### Alternatives rejected

- **`*Api` leaf per domain** (an `OrderApi` injecting only repositories, and so on): cycle-free by
  construction, but a second kind of class per domain, and unnecessary once delete-customer is out.
- **Domain events** for delete-customer: cannot express "refuse while orders are active" without a
  veto by exception from a listener.
- **`@Lazy`**: hides the cycle instead of removing it.
- **Dropping the up-front customer checks** as an authentication concern, with
  `getReferenceById` for new rows: right once auth exists, but until then it turns unknown customer
  ids into 500s. The checks stay; #104 records the auth direction.

### Behaviour changes

1. **Delete-customer keeps the cart.** `DeleteCustomerEndpointTest.deletesTheCustomersCart` becomes
   `keepsTheCustomersCart`. The cart-by-id operations can then reach a soft-deleted customer's cart;
   that gap is #104.
2. **Create-order's messages** "Cart Not Found" and "Address Not Found" become "Cart not found" and
   "Address not found", the cart and address services' wording. No test asserts them.

Nothing else changes.

## Verification

**C1**

1. `./mvnw -B clean verify`: 177 tests pass.
2. Test changes are package and import lines only: `git diff <base> -- src/test` shows no assertion change.
3. `grep -rn "com.mentorship.restaurant.support" src` prints nothing.

**C2**

1. `./mvnw -B clean verify`: 177 tests pass. The only test change is `keepsTheCustomersCart`. Any other
   failing test means behaviour changed: stop and report.
2. Spring starts in every `@SpringBootTest`, which is the proof that there is no cycle.
3. No domain imports another domain's repository. Run from `restaurant/`, this prints nothing:

   ```bash
   for d in cart customer order payment restaurant user; do
     grep -rln "import com\.mentorship\.restaurant\.$d\.repository\." src/main/java/com/mentorship/restaurant --include='*.java' \
       | grep -v "/com/mentorship/restaurant/$d/"
   done
   ```

Both: one commit per domain or concern, each passing `verify`; `spotless:apply` with JDK ≤ 21 on the
files the PR touches.

## Docs

**C1**

- `CLAUDE.md`: the domains, and every example that names a moved package (`cart/exception/`, the
  exception bases `GlobalExceptionHandler` catches).
- Use-case specs: only where they name a moved package or class.
- ADR `0003-domain-layout.md`: `restaurant/` over `catalog/`, `payment/`'s scope, `user/`, exceptions
  following their subject.

**C2**

- `CLAUDE.md`: the cross-domain rule (call services, never another domain's repository; entity
  mappings allowed), and "a use-case whose dependencies would close a cycle gets its own service;
  `CustomerDeletionService` is the example". The known-violations list goes.
- Delete-customer spec: the cart is kept; link #104.
- ADR `0004-cross-domain-access-through-services.md`: the rule, why delete-customer has its own
  service, and the rejected alternatives above.
- The roadmap: C's status.

## Out of scope

- Restoring 404s on the cart-by-id operations for a soft-deleted owner: #104.
- Authentication, and moving the "is the customer active?" checks into it: #104 and the auth work.
- Renames, builders and request DTOs: sub-project D.

## Done when

1. Both PRs pass `./mvnw -B clean verify` with 177 tests, with only the test changes listed above.
2. The C2 boundary check prints nothing.
3. CLAUDE.md, ADRs 0003 and 0004, the specs and the roadmap are updated.
4. Both PRs are opened after the user's go-ahead.
