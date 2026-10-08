# mentorship-restaurant

Spring Boot food-delivery backend. The Maven project lives in `restaurant/`; the repository root holds CI and top-level docs.

**Stack:** Java 17, Spring Boot 4.1.0, Spring Data JPA, Lombok, Flyway, PostgreSQL 16, Spotless.

## Commands

Run everything from `restaurant/`.

```bash
./mvnw -B verify            # compile + test + package (what CI runs)
./mvnw -B test -Dtest=Foo   # a single test class
./mvnw spotless:apply       # format before committing (see the JDK note below)
docker compose up -d --wait postgres   # database only
docker compose --profile app up        # database + application
```

## Architecture

The pattern is set by `CartService`. Follow it rather than inventing a new shape.

**Domains**, one top-level package each: `cart/` (carts and their lines), `customer/` (customers and their addresses), `order/` (orders, status, history, ratings), `restaurant/` (restaurants, menus, menu items and their stock), `payment/` (saved cards, the processor, transactions), `user/` (login accounts, shared by customers and restaurants). ADR 0003 records why the code sits where it does.

**One `@Service` per controller holds the logic.** Each public method is one use-case, annotated `@Transactional` (`readOnly = true` for reads), with guards as private `ensureXxx()` methods (`ensureStockAvailable`, `ensureSameRestaurant`). Related services share their domain's directory: `order/service/` holds `OrderService`, `OrderStatusService`, `OrderHistoryService` and `OrderRatingService`. There are no per-use-case handler classes; ADR 0002 records why.

**A guard two services in a domain share goes on that domain's primary service** (`OrderService`, `CustomerService`) as a public method, and the others inject it: `OrderRatingService` calls `orderService.ensureOwnedBy(...)`, `AddressService` calls `customerService.findActiveCustomer(...)`. Dependencies run one way only. The primary service never injects its siblings, because Spring refuses to start with a constructor-injection cycle. A guard only one service uses stays private there. Lookups that run different queries stay separate even when they throw the same exception.

**Chain of responsibility is fine where a use-case is a pipeline of steps.** Create-order validates the cart, address and items, then pays, saves and notifies, each link in `order/service/createorder/` handing on to the next. `OrderService.createOrder` builds and runs the chain.

**Across domains, call the other domain's service, never its repository.** `CartService` asks `CustomerService.findActiveCustomer` and `RestaurantService.decrementStock`; it does not inject `CustomerRepository` or `MenuItemRepository`. This keeps each domain's queries and rules behind one door. Entity mappings across domains (`Order.customer`, `CartItem.menuItem`) are allowed: they are the schema's foreign keys, and JPQL may join through them. Only injection is restricted. The methods other domains call carry no `@Transactional`; they join the caller's transaction. A domain's service can exist before its controller when other domains need it: `RestaurantService` has none until the restaurant CRUD (#85–#101). The create-order chain's `ProcessPaymentHandler` uses `PaymentProcessor` directly, because it is a payment component, not a repository. ADR 0004 records why. To check, from `restaurant/` (prints nothing when clean):

```bash
for d in cart customer order payment restaurant user; do
  grep -rln "import com\.mentorship\.restaurant\.$d\.repository\." src/main/java/com/mentorship/restaurant --include='*.java' \
    | grep -v "/com/mentorship/restaurant/$d/"
done
```

**A use-case whose dependencies would close a cycle gets its own service.** Delete-customer needs order, and order needs customer through cart and address, so `deleteCustomer` lives in `CustomerDeletionService`, not `CustomerService`; `CustomerController` injects both. Nothing points back at it, so there is no cycle. Reach for this before `@Lazy`, which hides a cycle instead of removing it.

**A `@Transactional` method called from the same class gets no transaction of its own.** The call skips Spring's proxy, so the annotation is ignored. When each item of a loop needs its own transaction, use `TransactionTemplate`: `OrderStatusService.autoRejectStaleOrders` rejects each stale order in its own, so one failure neither rolls back nor stops the rest.

**Entities are anemic data holders.** `@Getter @Setter`, no behaviour, no hand-written factory methods, no queries. Do not put business rules on an entity.

**An entity the application creates has a builder; setters change it.** It carries `@Builder`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for Hibernate and `@AllArgsConstructor(access = AccessLevel.PRIVATE)` for the builder, so nothing outside calls a constructor. **An initialised field needs `@Builder.Default`** (`private List<CartItem> items = new ArrayList<>();`): without it the builder sets the list to null, and the first `getItems().add(...)` throws. Updates keep using setters, which dirty checking turns into the `UPDATE`. An entity the application never creates (`Restaurant`, `Menu`, `MenuItem`) has only the protected no-args constructor. ADR 0005 records why.

**Lombok and `boolean` fields:** `private boolean isOpen` generates `isOpen()` **and `setOpen()`** — it strips the `is` prefix from the setter but not the getter.

**Every lookup is a repository method.** Nothing filters or searches inside an entity or a service — if you need to find something, add a query to the repository.

**Constructor injection is generated.** `@RequiredArgsConstructor` on the class, `private final` fields — do not hand-write a constructor that only assigns fields. `HealthController` is the exception: its parameter carries `@Value`, and Lombok drops annotations on generated parameters without a `lombok.config`.

**Mappers build responses.** `CartMapper` / `CartItemMapper` in `cart/model/mapper/`, `@Component`.

**Requests and responses are Lombok `@Data` classes** (not records) with Jakarta validation annotations, in `cart/model/request/` and `cart/model/response/`.

**Controllers return `ResponseEntity<T>`**, base path `/api/v1/...`, `@Valid @RequestBody`, `@Tag` for springdoc.

**Exceptions live in their domain's `exception/` package** (`cart/exception/`, …), carry their message, **extend their domain's base** (`CartException`, `CustomerException`, `OrderException`, `RestaurantException`, `PaymentException`), and **declare their status with `@ResponseStatus`**. `GlobalExceptionHandler` has one handler per base and reads the status back off the annotation, so extending the base class is the only registration step; a new domain adds its base and one handler method. An exception that extends `RuntimeException` directly falls through to `@ExceptionHandler(Exception.class)` and becomes a 500; one that extends a base without `@ResponseStatus` also returns 500, deliberately, so the omission is noticed.

**The generic 500 never echoes the exception.** `handleGenericException` returns a fixed message and logs the detail — an unhandled exception's own message can carry SQL, class names or connection details.

### Persistence rules

**Multi-row deletes are one statement.** An explicit `@Modifying @Query` scoped to the cart, never a loop and never `orphanRemoval` over a mutated collection. A *derived* `deleteAllBy...` with no `@Query` loads every row and deletes them one at a time, which defeats the point.

**Never `save()` an entity that is already managed** inside `@Transactional`. Dirty checking issues the `UPDATE` at flush; the extra call is noise.

**A bulk query and the entities it affects cannot share a transaction safely.** A `@Modifying` query bypasses the persistence context, so a collection loaded before it goes stale and `clearAutomatically` detaches the entity outright. Take what you need out of the entity *before* the first such query, and re-read anything you need after it — `CartService.clearCart` and `CartService.checkout` are the worked examples. Checking existence with `existsById` rather than `findById` is the cheap way to make the mistake impossible, because there is then no stale entity in scope to map.

**Reads do not validate business state.** `viewCart` reports what is in the cart. Stock, opening hours and the rest belong to the operations that change something — a read must not fail because the world moved on.

### Where validation goes

Shape and range live on the request as Jakarta annotations (`@NotNull`, `@Positive`, `@Max`) and are rejected with a 400 before the service runs. Services only check what needs loaded state — is the restaurant open, is the item in stock, is it already in the cart. Do not re-check a bound in the service; that code is unreachable over HTTP.

### Layering

```text
Controller  -> <Domain>Service   (@Service, @Transactional, the logic; one per controller)
            -> <Primary>Service  (shared guards within a domain, injected one way)
            -> Other domains' services (never their repositories — see the cross-domain rule)
            -> Repositories      (all lookups; own domain only)
            -> Entities          (anemic)
            -> Mappers           (responses)
```

### Formatting, and the JDK it needs

Spotless has **no lifecycle binding** in the pom, so `verify` does not run it and CI does not enforce formatting. Run `spotless:apply` yourself before committing.

It also needs **JDK 21 or older**. `google-java-format` reaches into `javac` internals and dies on newer JDKs:

```text
NoSuchMethodError: com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()
```

If your default JDK is newer (25 here), run it explicitly:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B spotless:apply
```

That path is machine-specific (`ls /usr/lib/jvm`). With no JDK ≤ 21 installed, unpack one anywhere — no root needed — and point `JAVA_HOME` at it:

```bash
curl -sSL "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse" | tar xz -C /tmp
JAVA_HOME=$(echo /tmp/jdk-21*) ./mvnw -B spotless:apply
```

`spotless:apply` formats the whole tree, including files your change never touched (the `createOrder/` chain still uses 4-space indents). Revert those with `git checkout -- <file>` so the diff stays about your change.

The rest of the build is unaffected — the pom targets release 17, so compiling and testing work on any modern JDK. CI uses Temurin 17.

## Database

**Columns are prefixed with their table's noun:** `restaurant_name`, `restaurant_is_open`, `menu_item_stock`, `cart_item_price`. Keep new columns consistent with that.

**Flyway owns the schema; `ddl-auto=validate` everywhere.** Never let Hibernate generate DDL. Add a numbered migration in `restaurant/src/main/resources/db/migration/` and map the entity to it — a mismatch fails the build at boot, which is the intended safety net.

Two things to know before writing a migration:

**Seeding explicit ids breaks the identity columns.** `V2` and `V3` insert explicit primary keys into `GENERATED BY DEFAULT AS IDENTITY` columns, which does not advance them, so the first row the application inserted collided with a seeded id:

```text
INSERT INTO carts (customer_id) VALUES (2) RETURNING cart_id;
ERROR:  duplicate key value violates unique constraint "carts_pkey"
```

`V6` resynced every seeded table. **Any future migration that seeds explicit ids must do the same**, with `ALTER TABLE <t> ALTER COLUMN <pk> RESTART WITH <max+1>` so the identity columns stay aligned with the seeded rows.

**Migration numbers are claimed at merge time, not branch time.** Two branches each added a `V12`; once both were merged, Flyway refused to start (`Found more than one migration with version 12`) and every Spring test errored (#82). Before merging, check `main` for your version number and renumber if it is taken. After renaming a migration, build with `clean` — Maven leaves the old file in `target/classes`, where Flyway still finds it — and reset any local database that already applied it with `docker compose down -v`.

**Seed data is global and shared.** `V2` seeds users, customers, restaurants, menus and menu items; `V3` seeds carts for customers 1 and 2; `V6` adds customer 3 with no cart and restaurant 3 closed, both reserved as add-to-cart fixtures. A test that deletes broadly destroys fixtures Flyway will not restore. Scope every cleanup to the rows that test created. The agreed direction is per-test seeding rather than shared global seeds.

## Testing

Tests run against **PostgreSQL** with `validate` and Flyway applied, so entity/migration drift is caught.

**Default to end-to-end**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `RestTestClient`, one class per use-case, covering the happy path and every rejection. Needs `spring-boot-starter-webmvc-test`, which brings `spring-boot-resttestclient`. Reach for a mocked unit test only when a branch cannot be triggered over HTTP.

Two things that follow from testing over real HTTP:

- **`@Transactional` rolls back nothing the server did** — requests run on server threads in their own transactions. Clean up explicitly, scoped to the rows the test created.
- **Every rejection needs a fixture that can reach it.** Seed one if none exists, as `V6` does with the closed restaurant, or pick request values that trigger it — asking for 999 of an item stocked at 50 exercises the out-of-stock path with no fixture at all.

**Seed through the database, not the API.** The only HTTP call a test makes is to the endpoint it tests. Arrange state — customers, addresses, carts, orders, cards — with direct SQL through the `support/*EndpointTestSupport` helpers (`insertCustomer`, `softDeleteCustomer`, `insertOrder`, …), and check side effects the same way. Setting up through another endpoint ties a test to code it is not about, so one broken endpoint fails a dozen unrelated tests. Need a new fixture? Add a helper to the matching support class — `CustomerEndpointTestSupport` is the base for anything that owns its customers — rather than a private copy in one test. Some older tests (addresses, cart) still arrange state over HTTP; convert them when you touch them.

`CustomerEndpointTestSupport` also seeds what a customer owns — `addressIdForCustomer`, `insertCart`, `insertCartItem`, `insertOrder` — so a test that owns its customers builds their cart and address the same way.

**Pin known-wrong behaviour before a refactor; do not fix it in passing.** A refactor needs tests of what the code *does*, so a structural change cannot quietly change behaviour. When today's behaviour is known to be wrong, the test still asserts it, and says so on the line above:

```java
// #82 §2: should be 409, currently 403
.expectStatus().isForbidden();
```

The PR that fixes the behaviour flips the assertion and deletes the comment. `grep -rn "// #82" restaurant/src/test` lists what is still open. `CreateOrderEndpointTest` is the worked example.

## Documentation

Use-cases live in `restaurant/.docs/use-cases/<area>/<use-case>/`, each with a spec and its diagrams as inline Mermaid. **The spec is the source of truth — when code and spec disagree, fix the spec in the same PR.**

Work that is not a use-case — refactors, test infrastructure, cross-cutting changes — gets a design spec in `restaurant/.docs/designs/YYYY-MM-DD-<topic>-design.md`, committed with the work. Not in a top-level `docs/` (some tooling defaults there): all project docs live under `restaurant/.docs/`. A design records scope, decisions and what is deliberately left out; once implemented, the code and its tests take over, so update the design only while the work is in flight.

Implementation plans are gitignored (`.docs/use-cases/**/implementation-plan.md`, `.docs/designs/*-plan.md`). They are working notes; anything worth keeping belongs in the spec or design.

## Conventions

- Branches: `feat/GH-<issue>-<slug>`; issues are tracked as GitHub sub-issues under an umbrella issue.
- Commit messages explain *why*, not just what.
- Do not add dependencies without a reason that is written down.
