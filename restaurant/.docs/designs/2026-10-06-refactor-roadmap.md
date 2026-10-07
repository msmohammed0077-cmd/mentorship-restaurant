# #84 refactor — roadmap

Umbrella for issue #84 ("Refactor code"). The issue body links a Telegram message carrying
`mentorship-restaurant-main.rar`, a copy of the repo that the mentorship session annotated, plus "Remove
handlers". This file records how the work was split and what is still open, so a session with no
history can pick it up. Each sub-project gets its own design → plan → PR, and each PR updates
`CLAUDE.md` with the rules it introduces (the user asked for this).

## Sub-projects

| # | What | Status | Design | Branch / PR |
|---|---|---|---|---|
| A | Safety net: green `main`, working create-order chain, end-to-end tests pinning today's order behaviour (#82 §1, §2 chain, §5 tests) | **Done.** PR open, waiting for review | `2026-10-06-order-safety-net-design.md` | `feat/GH-82-order-safety-net`, #102 |
| B | Remove the handler pattern: one service per controller | **Design approved (2026-10-07). Plan written** (`2026-10-06-remove-handlers-plan.md`, gitignored, so local only). Next: execute it | `2026-10-06-remove-handlers-design.md` | `feat/GH-84-remove-handlers`, stacked on A; its PR targets `feat/GH-82-order-safety-net` |
| C | Domain boundaries | Not started. Needs brainstorming | — | — |
| D | The mentor's smaller points | Not started. Needs brainstorming | — | — |

Order matters: each step needs the previous one's tests or structure. A pins behaviour; B
restructures; C moves code between domains; D renames and reshapes.

## Working agreements

- **Process.** `superpowers:brainstorming` (one question at a time, the user approves each design
  section, then the spec is committed) → `superpowers:writing-plans` (plan saved as
  `restaurant/.docs/designs/<date>-<topic>-plan.md`, gitignored) → `superpowers:executing-plans`,
  inline (the user chose inline over subagents) → `superpowers:finishing-a-development-branch`.
- **Ask first, then act.** State the plan and wait for an explicit LGTM before acting. Before
  pushing, opening a PR, or editing issues, get the user's go-ahead.
- **Stacked PRs.** Each sub-project branches from the previous one's branch and its PR targets it.
- **Behaviour is pinned, not fixed.** Known-wrong behaviour stays as it is, asserted with
  `// #82 §n: should be X, currently Y` comments. #82 holds the order bug and feature list.

## C — domain boundaries (open)

Rule, written into `CLAUDE.md` by B: a service injects only its own domain's repositories; for
another domain's data it injects that domain's **service** (`CustomerService` injects `OrderService`,
not `OrderRepository`). B lists the known violations in `CLAUDE.md`; C fixes them.

To decide in C:

- **Cycles.** Fixing the violations naively gives `CustomerService ↔ OrderService` (delete-customer's
  active-order check vs create-order's address lookup) and `CustomerService ↔ CartService`
  (add-to-cart's customer check vs delete-customer's cart clear). Spring refuses to start with
  either. Options from #81: small per-domain query/command components, domain events, or `@Lazy`.
- **`payment/`.** The user wants "Payment, including PaymentMethod" grouped. What does it own:
  saved cards (`PaymentMethod`, its controller, service, repository and validators, now in
  `customer/`), the processor (`support/PaymentProcesser`, misspelt), `Transaction` (now in
  `order/`)? Also rename the `order.model.request.PaymentMethod` enum, which clashes with the entity
  (e.g. `PaymentType`).
- **Restaurant/menu.** `Restaurant`, `Menu`, `MenuItem` and `MenuItemRepository` live in `cart/`.
  The package name `restaurant` clashes with the root package (`com.mentorship.restaurant.restaurant`);
  `catalog` is the alternative. Issues #85–#101 (restaurant and menu CRUD) will build on it.
- **`User`.** It sits in `customer/`. The mentor's note "replaced userRepository with userService"
  suggests a `user/` domain with `UserService`.
- The user's view: Address and PaymentMethod belonging to Customer "might be correct". The analysis
  agreed for Address, which is owned by the customer and deleted with it.
- #81 (closed) also asked whether a JPQL join from one domain's repository into another domain's
  entity counts as a boundary crossing. Undecided.

## D — the mentor's smaller points (open)

From the annotated RAR. To diff against it again: `unrar x` (RARLAB's unrar; `unar` and this
`7z` can't decode it) and compare with `diff -rq --strip-trailing-cr`. Only 13 files differ from
`main`, mostly by comments. Points not covered by A–C:

| Mentor's note | Where | Open question |
|---|---|---|
| `@Builder @AllArgsConstructor` on `User`; "create builder", "buider" | `User`, `CreateCustomerHandler`, `RateOrderHandler` | `CLAUDE.md` says entities have "no factories" and a protected no-args constructor. `@Builder` drops field initialisers (`items = new ArrayList<>()` becomes null) unless they have `@Builder.Default`. The proposal is builders only on entities the application creates (User, Customer, Order, OrderRating), always with `@Builder.Default`. **The user has not decided.** |
| `/password` → `/changePassword` | `CustomerController` | The proposal is to keep `PUT /customers/{id}/password`: the rest of the API uses nouns, and the change breaks clients. The mentor's `// 203` comment is wrong; it returns 204. **The user has not decided.** |
| `CreateCustomerRequest` → `CustomerRequest`; fields `customerName`, `customerEmail`, `phoneNumber` | `CreateCustomerRequest` | Create and update validate differently, so one class would need validation groups. Renaming the fields changes the JSON contract. Options: keep everything (recommended), rename the Java fields only with `@JsonProperty`, or rename everything. **The user has not decided.** |
| `rate` → `rateOrder`, `viewOrder` → `viewOrderDetails` | order controller, service, handlers | Each operation keeps one name through every layer. Agreed in principle. |
| `OrderStatus.getActiveOrderStatus()` | `OrderStatus`, `DeleteCustomerHandler` | Use a static `EnumSet` constant (`OrderStatus.ACTIVE`) rather than rebuilding the set per call. |
| `isCustomerEmailExists` returning `boolean` while throwing | `UpdateCustomerHandler` | Rejected: misleading. Keep `ensureXxx()`. The duplication it targeted goes in B. |
| `cartRepository.deleteByCustomer_Id // no need`, `is_deleted = 1,0` | `DeleteCustomerHandler` | The meaning is unclear: keep the cart on soft delete? Use a boolean flag instead of `deleted_at`? **Ask the user.** Until then, unchanged. |
| "can be replaced with one query [native query]" | `RateOrderHandler` | A possible follow-up issue, not part of #84. |
| "JPA Pagination" | `ViewOrderHistoryHandler` | Possibly Spring Data's keyset `Window`/`ScrollPosition`. A follow-up issue, not part of #84. |

## Environment notes

- Postgres: `docker compose up -d --wait postgres` from `restaurant/`. If `docker pull postgres:16`
  times out, retry; it was transient.
- Spotless needs JDK ≤ 21. This machine has only JDK 25. See `CLAUDE.md` for the no-root Temurin 21
  download.
- Baseline on A's branch: `./mvnw -B clean verify` passes 177 tests.
