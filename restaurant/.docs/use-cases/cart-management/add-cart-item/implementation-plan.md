# Add To Cart API (GH-21) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `POST /api/v1/carts/items` so a customer can add a menu item to their cart, per `add-cart-item.md` in this directory.

**Architecture:** The insert-vs-increment rule lives on the `Cart` entity, not the service. `CartService.addItem` orchestrates: load, guard (open → stock → same restaurant), get-or-create cart, delegate to the domain, map to a response. Flyway owns the schema; Hibernate validates the entities against it at boot.

**Tech Stack:** Java 17, Spring Boot 4.1.0, Spring Data JPA, Flyway, PostgreSQL 16 (via `compose.yaml`), H2 in tests, JUnit 5 + Mockito + RestTestClient, Spotless.

**Baseline:** `main` as of the Spotless commit. Migrations run to `V5`, so this plan's migration is **`V6`**.

---

## Decisions this plan makes

**1. The cart's restaurant is derived, not stored.** Per the team decision, no `restaurant_id` column is added to `carts`. A cart's restaurant is inferred from its lines: `CartItem → MenuItem → Menu → Restaurant`. An empty cart has no restaurant and therefore accepts any, which is exactly the doc's "cart is empty OR same restaurant" rule.

Consequences: `Cart.getRestaurant()` returns `Optional<Restaurant>`; the guard becomes `cart.accepts(restaurant)`, true for an empty cart; `CartResponse.restaurantId` is `null` for an empty cart; and every cart load must fetch its lines, so the repository needs **one** query method rather than two.

**2. The price snapshot is mandatory.** `cart_items.cart_item_price NOT NULL CHECK (>= 0)` already exists, so a line cannot be inserted without a price. The doc's Totals section claims the opposite. This plan captures `menu_item_price` into `cart_item_price` at creation and computes totals from the captured value, so a menu price change never reprices open carts. Incrementing a line **keeps** its original price. Task 8 corrects the doc.

**3. `menu_item_name` is added.** `menu_items` has `menu_item_code` (`'NK-KOFTA-01'`) and `menu_item_note` (a description sentence), but no name. A cart response needs a readable name, so V6 adds one and backfills the six seeded rows by code.

**4. Stock already exists.** `V4` added `menu_items.menu_item_stock` (NOT NULL, seeded 40–80). This plan **does not** add a stock column; it maps the existing one. Note the name is `menu_item_stock`, not `stock_quantity`.

**5. Concurrency is out of scope.** Two concurrent adds by the same customer can both find no cart, and the second violates `carts.customer_id`'s unique constraint, surfacing as a 500. Concurrent increments can lose an update. The database constraints prevent corruption; the failure mode is a poor error, under a rare interleaving. Recorded on PR #27 rather than solved here.

## Global Constraints

- Java 17. Spring Boot 4.1.0 (parent POM; do not change the version).
- Package root: `com.mentorship.restaurant.cart`.
- `spring.jpa.hibernate.ddl-auto=validate` everywhere. Flyway owns the schema.
- Rejection messages verbatim from the spec: `"Restaurant is closed"`, `"Item is not in stock"`, `"Item is of different restaurant"`.
- A `CartItem` is unique by menu item alone; `uq_cart_items_cart_menu_item` already enforces it.
- Stock is checked against the **resulting** quantity (existing line + requested), never the requested quantity alone.
- Auth is out of scope: `customerId` arrives in the request body.
- Entities keep their `protected` no-arg constructor for JPA; construction goes through static factories.
- Endpoints live under `/api/v1`.
- **Run `./mvnw spotless:apply` before every commit** — Spotless is on `main` and CI fails on unformatted code.
- Every task ends with a commit. Branch: `feat/GH-21-api-add-cart-item`.

## Schema as it stands

```text
users(user_id, user_name, user_email UQ, user_password)
customers(customer_id, user_id UQ -> users)
restaurants(restaurant_id, user_id UQ -> users, restaurant_name, restaurant_description)
menus(menu_id, restaurant_id -> restaurants, menu_code UQ, menu_category)
menu_items(menu_item_id, menu_id -> menus, menu_item_code UQ, menu_item_image_url,
           menu_item_price NUMERIC(12,2) CHECK >= 0,
           menu_item_stock INTEGER NOT NULL,          -- V4
           menu_item_note VARCHAR(255))               -- V4
carts(cart_id, customer_id UQ -> customers)
cart_items(cart_item_id, cart_id -> carts, menu_item_id -> menu_items,
           cart_item_quantity INTEGER CHECK > 0,
           cart_item_price NUMERIC(12,2) CHECK >= 0,
           cart_item_note VARCHAR(255),               -- V5
           UNIQUE (cart_id, menu_item_id))
```

Seeded data: `V2` seeds users 1–4, customers 1–2, restaurants 1–2, menus 1–2, menu items 1–6. `V3` seeds carts 1–2 (for customers 1 and 2) with three lines.

**Both seeded customers already own a cart**, so nothing exercises "create a cart when the customer has none". V6 seeds a third customer with no cart, reserved as this use-case's fixture.

**Note on seed strategy:** global seed data shared by every test is brittle — one test's cleanup breaks another's fixture. The agreed direction is for each test to seed only the data it needs. That refactor is not in this plan; V6's third customer is a stopgap that keeps this use-case from mutating existing seeds.

## The identity sequence bug

`V2` and `V3` insert explicit primary keys (`cart_id`, `user_id`, …) into `GENERATED BY DEFAULT AS IDENTITY` columns. That does **not** advance the underlying sequence, so the first row the application inserts reuses id 1 and collides. Verified against a freshly migrated database:

```text
INSERT INTO carts (customer_id) VALUES (2) RETURNING cart_id;
ERROR:  duplicate key value violates unique constraint "carts_pkey"
DETAIL:  Key (cart_id)=(1) already exists.
```

This blocks the very first cart the API creates. V6 resynchronises every seeded table's sequence.

## File Structure

**Migration (new)**
- `src/main/resources/db/migration/V6__add_cart_item_fields.sql`

**Domain (modified)**
- `model/entity/Restaurant.java` — `isOpen` + accessors
- `model/entity/MenuItem.java` — `menuItemName`, `menuItemStock`, `hasStockFor(int)`, accessors
- `model/entity/Cart.java` — `items`, `create`, `addItem`, `findItem`, `getRestaurant`, `accepts`, accessors
- `model/entity/CartItem.java` — `price`, `create`, `increaseQuantityBy`, accessors
- `model/entity/Menu.java`, `Customer.java` — accessors only

**Errors (new + modified)**
- `exception/RestaurantClosedException.java`, `OutOfStockException.java`, `DifferentRestaurantException.java`, `NotFoundException.java` — new
- `exception/InvalidQuantityException.java` — message constructor
- `controller/CartExceptionHandler.java`, `controller/response/ErrorResponse.java` — new

**Plumbing (modified)**
- `repository/CartRepository.java` — one query method
- `service/CartService.java` — implement `addItem`
- `model/mapper/CartRestMapper.java` — build `CartResponse`, compute the total
- `controller/response/CartResponse.java`, `CartItemResponse.java` — reshape
- `controller/CartController.java` — the endpoint
- `model/request/AddCartItemRequest.java` — validation
- `pom.xml` — restore `spring-boot-starter-webmvc-test`

**Tests (new)**
- `model/entity/TestEntities.java` — in-memory entity builders
- `model/entity/CartTest.java`, `MenuItemTest.java` — pure unit
- `model/mapper/CartRestMapperTest.java` — pure unit
- `service/CartServiceTest.java` — Mockito, covers every rejection
- `cart/AddCartItemEndpointTest.java` — `@SpringBootTest(RANDOM_PORT)` + `RestTestClient`

**Docs (modified)**
- `add-cart-item.md` in this directory

---

### Task 1: Migration V6

**Files:**
- Create: `src/main/resources/db/migration/V6__add_cart_item_fields.sql`
- Test: `src/test/java/com/mentorship/restaurant/RestaurantApplicationTests.java` (exists; booting it is the test)

**Interfaces:**
- Consumes: the V1–V5 schema.
- Produces: `restaurants.is_open BOOLEAN NOT NULL`, `menu_items.menu_item_name VARCHAR(150) NOT NULL`, customer 3 with no cart, and resynchronised identity sequences.

- [ ] **Step 1: Write the migration**

```sql
-- Step 2 of the use-case: restaurant open/closed. A flag, not opening hours.
ALTER TABLE restaurants
    ADD COLUMN is_open BOOLEAN NOT NULL DEFAULT TRUE;

-- menu_items has menu_item_code and menu_item_note, but no readable name.
ALTER TABLE menu_items
    ADD COLUMN menu_item_name VARCHAR(150);

UPDATE menu_items SET menu_item_name = 'Kofta Platter'    WHERE menu_item_code = 'NK-KOFTA-01';
UPDATE menu_items SET menu_item_name = 'Falafel Sandwich' WHERE menu_item_code = 'NK-FALAFEL-01';
UPDATE menu_items SET menu_item_name = 'Koshari Bowl'     WHERE menu_item_code = 'NK-KOSHARI-01';
UPDATE menu_items SET menu_item_name = 'Classic Burger'   WHERE menu_item_code = 'BY-CLASSIC-01';
UPDATE menu_items SET menu_item_name = 'Chicken Burger'   WHERE menu_item_code = 'BY-CHICKEN-01';
UPDATE menu_items SET menu_item_name = 'Fries'            WHERE menu_item_code = 'BY-FRIES-01';

-- No such rows exist today; this only makes the NOT NULL below safe if any
-- were added between V2 and here.
UPDATE menu_items SET menu_item_name = menu_item_code WHERE menu_item_name IS NULL;

ALTER TABLE menu_items
    ALTER COLUMN menu_item_name SET NOT NULL;

-- A customer with no cart. V3 gave both existing customers one, leaving nothing
-- to exercise "create a cart when the customer has none".
INSERT INTO users (user_id, user_name, user_email, user_password) VALUES
    (5, 'Sara Nabil', 'sara.nabil@example.com', '$2a$10$placeholder.customer3');

INSERT INTO customers (customer_id, user_id) VALUES
    (3, 5);

-- V2 and V3 inserted explicit ids into GENERATED BY DEFAULT AS IDENTITY columns,
-- which does not advance the sequences. Without this the first row the
-- application inserts reuses id 1 and violates the primary key.
SELECT setval(pg_get_serial_sequence('users', 'user_id'),
              (SELECT MAX(user_id) FROM users));
SELECT setval(pg_get_serial_sequence('customers', 'customer_id'),
              (SELECT MAX(customer_id) FROM customers));
SELECT setval(pg_get_serial_sequence('restaurants', 'restaurant_id'),
              (SELECT MAX(restaurant_id) FROM restaurants));
SELECT setval(pg_get_serial_sequence('menus', 'menu_id'),
              (SELECT MAX(menu_id) FROM menus));
SELECT setval(pg_get_serial_sequence('menu_items', 'menu_item_id'),
              (SELECT MAX(menu_item_id) FROM menu_items));
SELECT setval(pg_get_serial_sequence('carts', 'cart_id'),
              (SELECT MAX(cart_id) FROM carts));
SELECT setval(pg_get_serial_sequence('cart_items', 'cart_item_id'),
              (SELECT MAX(cart_item_id) FROM cart_items));
```

No stock column is added — `V4` already provides `menu_item_stock`. No `carts.restaurant_id` — the cart's restaurant is derived from its lines.

- [ ] **Step 2: Apply it and confirm the sequence fix**

```bash
docker compose down -v
docker compose up -d --wait postgres
./mvnw -B test -Dtest=RestaurantApplicationTests \
  -Dspring.datasource.url=jdbc:postgresql://localhost:55433/restaurant \
  -Dspring.datasource.driver-class-name=org.postgresql.Driver \
  -Dspring.datasource.username=postgres -Dspring.datasource.password=postgres
```

Expected: PASS, log shows `Successfully applied 6 migrations`.

Then prove the collision is gone:

```bash
docker exec restaurant-postgres psql -U postgres -d restaurant \
  -c "INSERT INTO carts (customer_id) VALUES (3) RETURNING cart_id;"
```

Expected: returns `cart_id` 3, not a duplicate-key error. Clean up with:

```bash
docker exec restaurant-postgres psql -U postgres -d restaurant \
  -c "DELETE FROM carts WHERE customer_id = 3;"
```

Note `pg_get_serial_sequence` is PostgreSQL-specific and the test suite runs on H2. If `RestaurantApplicationTests` fails under H2 with an unknown-function error, wrap the seven `setval` statements so they run only on PostgreSQL — the simplest route is moving them into a `V6_1__resync_sequences.sql` excluded from the H2 profile, or switching the test datasource back to PostgreSQL. Resolve this before continuing; the rest of the plan assumes the sequences are correct.

- [ ] **Step 3: Format and commit**

```bash
./mvnw -B spotless:apply
git add src/main/resources/db/migration/V6__add_cart_item_fields.sql
git commit -m "feat(db): add open flag, item name, a cart-less customer and sequence resync"
```

---

### Task 2: Domain behaviour on the entities

**Files:**
- Modify: `model/entity/Restaurant.java`, `MenuItem.java`, `Cart.java`, `CartItem.java`, `Menu.java`, `Customer.java`
- Create: `src/test/java/com/mentorship/restaurant/cart/model/entity/TestEntities.java`
- Test: `src/test/java/com/mentorship/restaurant/cart/model/entity/CartTest.java`, `MenuItemTest.java`

**Interfaces:**
- Consumes: Task 1's columns.
- Produces:
  - `Restaurant.getId(): Long`, `getRestaurantName(): String`, `isOpen(): boolean`
  - `MenuItem.getId(): Long`, `getMenuItemName(): String`, `getMenuItemPrice(): BigDecimal`, `getMenuItemStock(): Integer`, `getMenu(): Menu`, `getRestaurant(): Restaurant`, `hasStockFor(int): boolean`
  - `Cart.create(Customer): Cart`, `getId(): Long`, `getCustomer(): Customer`, `getItems(): List<CartItem>`, `getRestaurant(): Optional<Restaurant>`, `accepts(Restaurant): boolean`, `addItem(MenuItem, int): CartItem`, `findItem(Long): Optional<CartItem>`
  - `CartItem.getId(): Long`, `getMenuItem(): MenuItem`, `getQuantity(): Integer`, `getPrice(): BigDecimal`
  - `Menu.getRestaurant(): Restaurant`, `Customer.getId(): Long`
  - `TestEntities.restaurant(Long, String, boolean)`, `menu(Restaurant)`, `menuItem(Long, Menu, String, String, int)`, `customer(Long)` — test scope

`Restaurant`, `Menu` and `MenuItem` carry NOT NULL columns no entity maps (`user_id`, `menu_code`, `menu_item_code`), so they get no factory and are never constructed by application code.

- [ ] **Step 1: Write the test entity builders**

```java
package com.mentorship.restaurant.cart.model.entity;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * Builds entity instances for unit tests. Restaurant, Menu and MenuItem carry
 * NOT NULL columns that no entity maps, so they cannot be persisted from code.
 * These instances exist only in memory.
 */
public final class TestEntities {

    public static Restaurant restaurant(Long id, String name, boolean open) {
        Restaurant restaurant = new Restaurant();
        set(restaurant, "id", id);
        set(restaurant, "restaurantName", name);
        set(restaurant, "open", open);
        return restaurant;
    }

    public static Menu menu(Restaurant restaurant) {
        Menu menu = new Menu();
        set(menu, "restaurant", restaurant);
        return menu;
    }

    public static MenuItem menuItem(Long id, Menu menu, String name, String price, int stock) {
        MenuItem item = new MenuItem();
        set(item, "id", id);
        set(item, "menu", menu);
        set(item, "menuItemName", name);
        set(item, "menuItemPrice", new BigDecimal(price));
        set(item, "menuItemStock", stock);
        return item;
    }

    public static Customer customer(Long id) {
        Customer customer = new Customer();
        set(customer, "id", id);
        return customer;
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot set " + fieldName, ex);
        }
    }

    private TestEntities() {}
}
```

- [ ] **Step 2: Write the failing domain tests**

`CartTest.java`:

```java
package com.mentorship.restaurant.cart.model.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    private final Restaurant nileKitchen = TestEntities.restaurant(1L, "Nile Kitchen", true);
    private final Restaurant burgerYard = TestEntities.restaurant(2L, "Burger Yard", true);
    private final Menu nileMenu = TestEntities.menu(nileKitchen);
    private final MenuItem kofta = TestEntities.menuItem(1L, nileMenu, "Kofta Platter", "185.00", 20);
    private final MenuItem falafel = TestEntities.menuItem(2L, nileMenu, "Falafel Sandwich", "65.00", 20);

    @Test
    void addsANewLineWhenTheItemIsNotInTheCart() {
        Cart cart = Cart.create(null);

        cart.addItem(kofta, 2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void capturesThePriceWhenTheLineIsCreated() {
        Cart cart = Cart.create(null);

        cart.addItem(kofta, 1);

        assertThat(cart.getItems().get(0).getPrice()).isEqualByComparingTo("185.00");
    }

    @Test
    void incrementsTheExistingLineWhenTheItemIsAlreadyInTheCart() {
        Cart cart = Cart.create(null);

        cart.addItem(kofta, 2);
        cart.addItem(kofta, 3);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void keepsTheOriginalPriceWhenIncrementing() {
        Cart cart = Cart.create(null);
        cart.addItem(kofta, 1);
        MenuItem repriced = TestEntities.menuItem(1L, nileMenu, "Kofta Platter", "999.00", 20);

        cart.addItem(repriced, 1);

        assertThat(cart.getItems().get(0).getPrice()).isEqualByComparingTo("185.00");
    }

    @Test
    void keepsSeparateLinesForDifferentItems() {
        Cart cart = Cart.create(null);

        cart.addItem(kofta, 1);
        cart.addItem(falafel, 1);

        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    void findsAnExistingLineByMenuItemId() {
        Cart cart = Cart.create(null);
        cart.addItem(kofta, 1);

        assertThat(cart.findItem(1L)).isPresent();
        assertThat(cart.findItem(2L)).isEmpty();
    }

    @Test
    void hasNoRestaurantWhileEmpty() {
        assertThat(Cart.create(null).getRestaurant()).isEmpty();
    }

    @Test
    void takesItsRestaurantFromItsLines() {
        Cart cart = Cart.create(null);
        cart.addItem(kofta, 1);

        assertThat(cart.getRestaurant()).contains(nileKitchen);
    }

    @Test
    void anEmptyCartAcceptsAnyRestaurant() {
        Cart cart = Cart.create(null);

        assertThat(cart.accepts(nileKitchen)).isTrue();
        assertThat(cart.accepts(burgerYard)).isTrue();
    }

    @Test
    void aNonEmptyCartAcceptsOnlyItsOwnRestaurant() {
        Cart cart = Cart.create(null);
        cart.addItem(kofta, 1);

        assertThat(cart.accepts(nileKitchen)).isTrue();
        assertThat(cart.accepts(burgerYard)).isFalse();
    }
}
```

`MenuItemTest.java`:

```java
package com.mentorship.restaurant.cart.model.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MenuItemTest {

    private final Menu menu = TestEntities.menu(TestEntities.restaurant(1L, "Nile Kitchen", true));

    @Test
    void hasStockWhenTheRequestedQuantityIsBelowStock() {
        assertThat(TestEntities.menuItem(1L, menu, "Kofta", "185.00", 5).hasStockFor(4)).isTrue();
    }

    @Test
    void hasStockWhenTheRequestedQuantityExactlyMatchesStock() {
        assertThat(TestEntities.menuItem(1L, menu, "Kofta", "185.00", 5).hasStockFor(5)).isTrue();
    }

    @Test
    void hasNoStockWhenTheRequestedQuantityExceedsStock() {
        assertThat(TestEntities.menuItem(1L, menu, "Kofta", "185.00", 5).hasStockFor(6)).isFalse();
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

```bash
./mvnw -B test -Dtest='CartTest,MenuItemTest'
```

Expected: compilation failure — `cannot find symbol: method create(...)`, `accepts(...)`, `hasStockFor(...)`.

- [ ] **Step 4: Implement the entity changes**

`Restaurant.java` — add after `restaurantName`:

```java
    @Column(name = "is_open", nullable = false)
    private boolean open;

    protected Restaurant() {}

    public Long getId() {
        return id;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public boolean isOpen() {
        return open;
    }
```

`Menu.java`:

```java
    public Long getId() {
        return id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }
```

`Customer.java`:

```java
    public Long getId() {
        return id;
    }
```

`MenuItem.java` — map the two columns V4 and V6 added, rename the price field to match its column (delete the old `itemPrice`), and add behaviour:

```java
    @Column(name = "menu_item_name", nullable = false, length = 150)
    private String menuItemName;

    @Column(name = "menu_item_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal menuItemPrice;

    @Column(name = "menu_item_stock", nullable = false)
    private Integer menuItemStock;

    protected MenuItem() {}

    public boolean hasStockFor(int quantity) {
        return menuItemStock >= quantity;
    }

    public Long getId() {
        return id;
    }

    public Menu getMenu() {
        return menu;
    }

    public Restaurant getRestaurant() {
        return menu.getRestaurant();
    }

    public String getMenuItemName() {
        return menuItemName;
    }

    public BigDecimal getMenuItemPrice() {
        return menuItemPrice;
    }

    public Integer getMenuItemStock() {
        return menuItemStock;
    }
```

`CartItem.java` — add the mandatory price snapshot:

```java
    @Column(name = "cart_item_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected CartItem() {}

    static CartItem create(Cart cart, MenuItem menuItem, Integer quantity) {
        CartItem item = new CartItem();
        item.cart = cart;
        item.menuItem = menuItem;
        item.quantity = quantity;
        item.price = menuItem.getMenuItemPrice();
        return item;
    }

    void increaseQuantityBy(int amount) {
        this.quantity += amount;
    }

    public Long getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }
```

`create` and `increaseQuantityBy` are package-private: a `CartItem` is only made or changed through its `Cart`.

`Cart.java` — no `restaurant` field. Add imports `jakarta.persistence.CascadeType`, `OneToMany`, `java.util.ArrayList`, `List`, `Objects`, `Optional`:

```java
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {}

    public static Cart create(Customer customer) {
        Cart cart = new Cart();
        cart.customer = customer;
        return cart;
    }

    /**
     * Business Rule 1: one line per menu item. Adding an item already present
     * increments that line. The line keeps the price captured when it was
     * created, so a menu price change never reprices what is already in a cart.
     */
    public CartItem addItem(MenuItem menuItem, int quantity) {
        Optional<CartItem> existing = findItem(menuItem.getId());
        if (existing.isPresent()) {
            CartItem line = existing.get();
            line.increaseQuantityBy(quantity);
            return line;
        }
        CartItem line = CartItem.create(this, menuItem, quantity);
        items.add(line);
        return line;
    }

    public Optional<CartItem> findItem(Long menuItemId) {
        return items.stream()
                .filter(line -> Objects.equals(line.getMenuItem().getId(), menuItemId))
                .findFirst();
    }

    /**
     * Business Rule 2: the cart's restaurant is not stored. It is whichever
     * restaurant its lines belong to, and an empty cart has none.
     */
    public Optional<Restaurant> getRestaurant() {
        return items.stream().findFirst().map(line -> line.getMenuItem().getRestaurant());
    }

    /** True when the cart is empty, or already belongs to this restaurant. */
    public boolean accepts(Restaurant other) {
        return getRestaurant()
                .map(current -> Objects.equals(current.getId(), other.getId()))
                .orElse(true);
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }
```

`getItems()` returns a defensive copy so `addItem` stays the only way in. Hibernate reads the private field directly, so persistence is unaffected.

- [ ] **Step 5: Run the tests to verify they pass**

```bash
./mvnw -B test -Dtest='CartTest,MenuItemTest'
```

Expected: PASS, 13 tests.

- [ ] **Step 6: Verify the entities still match the schema**

```bash
./mvnw -B test -Dtest=RestaurantApplicationTests
```

Expected: PASS.

- [ ] **Step 7: Format and commit**

```bash
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart/model/entity src/test/java/com/mentorship/restaurant/cart/model/entity
git commit -m "feat(cart): give the cart domain behaviour and the new fields"
```

---

### Task 3: Repository lookup

Because the cart's restaurant is derived from its lines, every load needs those lines. One query method covers it.

**Files:**
- Modify: `src/main/java/com/mentorship/restaurant/cart/repository/CartRepository.java`

**Interfaces:**
- Consumes: Task 2's entities.
- Produces: `CartRepository.findWithItemsByCustomerId(Long): Optional<Cart>`

No dedicated repository test: Task 7 exercises this query end to end against a real database, and a `@DataJpaTest` would duplicate it while needing its own fixture handling.

- [ ] **Step 1: Add the query method**

```java
package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("""
            select c from Cart c
            left join fetch c.items i
            left join fetch i.menuItem m
            left join fetch m.menu mm
            left join fetch mm.restaurant
            where c.customer.id = :customerId
            """)
    Optional<Cart> findWithItemsByCustomerId(Long customerId);
}
```

The fetch chain runs all the way to `restaurant` because `Cart.getRestaurant()` walks it, and with `open-in-view=false` a lazy hop outside the transaction throws `LazyInitializationException`.

- [ ] **Step 2: Verify it compiles**

```bash
./mvnw -B -q compile
```

Expected: no output, exit 0.

- [ ] **Step 3: Format and commit**

```bash
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart/repository
git commit -m "feat(cart): add the cart lookup query"
```

---

### Task 4: Exceptions

**Files:**
- Create: `exception/RestaurantClosedException.java`, `OutOfStockException.java`, `DifferentRestaurantException.java`, `NotFoundException.java`
- Modify: `exception/InvalidQuantityException.java`

**Interfaces:**
- Produces: four `RuntimeException` subclasses each with a `(String message)` constructor, plus `InvalidQuantityException(String)`.

- [ ] **Step 1: Create the four exception classes**

`RestaurantClosedException.java`:

```java
package com.mentorship.restaurant.cart.exception;

public class RestaurantClosedException extends RuntimeException {

    public RestaurantClosedException(String message) {
        super(message);
    }
}
```

`OutOfStockException.java`:

```java
package com.mentorship.restaurant.cart.exception;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String message) {
        super(message);
    }
}
```

`DifferentRestaurantException.java`:

```java
package com.mentorship.restaurant.cart.exception;

public class DifferentRestaurantException extends RuntimeException {

    public DifferentRestaurantException(String message) {
        super(message);
    }
}
```

`NotFoundException.java`:

```java
package com.mentorship.restaurant.cart.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Add a message constructor to the existing exception**

```java
package com.mentorship.restaurant.cart.exception;

public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
```

Leave `EmptyCartException` and `CartNotEditableException` alone — they belong to other use-cases.

- [ ] **Step 3: Verify it compiles, format and commit**

```bash
./mvnw -B -q compile
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart/exception
git commit -m "feat(cart): add add-to-cart rejection exceptions"
```

---

### Task 5: The service and the mapper

**Files:**
- Modify: `service/CartService.java`, `model/mapper/CartRestMapper.java`, `controller/response/CartResponse.java`, `CartItemResponse.java`
- Create: `src/test/java/com/mentorship/restaurant/cart/model/mapper/CartFixtures.java`
- Test: `src/test/java/com/mentorship/restaurant/cart/model/mapper/CartRestMapperTest.java`, `src/test/java/com/mentorship/restaurant/cart/service/CartServiceTest.java`

**Interfaces:**
- Consumes: Tasks 2–4.
- Produces: `CartService.addItem(Long, Long, Integer): CartResponse`; `CartRestMapper.toResponse(Cart): CartResponse`; `CartResponse(Long id, Long customerId, Long restaurantId, List<CartItemResponse> items, BigDecimal total)`; `CartItemResponse(Long id, Long menuItemId, String menuItemName, BigDecimal price, Integer quantity, BigDecimal lineTotal)`.

- [ ] **Step 1: Write the mapper fixtures and failing test**

`CartFixtures.java`:

```java
package com.mentorship.restaurant.cart.model.mapper;

import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.Menu;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.cart.model.entity.Restaurant;
import com.mentorship.restaurant.cart.model.entity.TestEntities;

final class CartFixtures {

    private static final Restaurant RESTAURANT = TestEntities.restaurant(1L, "Nile Kitchen", true);
    private static final Menu MENU = TestEntities.menu(RESTAURANT);

    static Cart emptyCart() {
        return Cart.create(TestEntities.customer(1L));
    }

    static Cart cartWith(String firstPrice, int firstQuantity, String secondPrice, int secondQuantity) {
        Cart cart = emptyCart();
        MenuItem first = TestEntities.menuItem(1L, MENU, "Kofta Platter", firstPrice, 20);
        MenuItem second = TestEntities.menuItem(2L, MENU, "Falafel Sandwich", secondPrice, 20);
        cart.addItem(first, firstQuantity);
        cart.addItem(second, secondQuantity);
        return cart;
    }

    private CartFixtures() {}
}
```

`CartRestMapperTest.java`:

```java
package com.mentorship.restaurant.cart.model.mapper;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartRestMapperTest {

    private final CartRestMapper mapper = new CartRestMapper();

    @Test
    void computesTheTotalFromTheCapturedLinePrices() {
        CartResponse response = mapper.toResponse(CartFixtures.cartWith("185.00", 2, "65.00", 1));

        assertThat(response.total()).isEqualByComparingTo("435.00");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("370.00");
        assertThat(response.items().get(0).menuItemName()).isEqualTo("Kofta Platter");
    }

    @Test
    void reportsTheRestaurantOfTheCartsLines() {
        assertThat(mapper.toResponse(CartFixtures.cartWith("185.00", 1, "65.00", 1)).restaurantId())
                .isEqualTo(1L);
    }

    @Test
    void returnsAZeroTotalAndNoRestaurantForAnEmptyCart() {
        CartResponse response = mapper.toResponse(CartFixtures.emptyCart());

        assertThat(response.total()).isEqualByComparingTo("0");
        assertThat(response.items()).isEmpty();
        assertThat(response.restaurantId()).isNull();
    }
}
```

- [ ] **Step 2: Write the failing service test**

```java
package com.mentorship.restaurant.cart.service;

import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.NotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.exception.RestaurantClosedException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.Customer;
import com.mentorship.restaurant.cart.model.entity.Menu;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.cart.model.entity.Restaurant;
import com.mentorship.restaurant.cart.model.entity.TestEntities;
import com.mentorship.restaurant.cart.model.mapper.CartRestMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.cart.repository.CustomerRepository;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long MENU_ITEM_ID = 10L;

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MenuItemRepository menuItemRepository;

    private CartService cartService;

    private Customer customer;
    private Restaurant nileKitchen;
    private MenuItem kofta;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, customerRepository,
                menuItemRepository, new CartRestMapper());

        customer = TestEntities.customer(CUSTOMER_ID);
        nileKitchen = TestEntities.restaurant(1L, "Nile Kitchen", true);
        Menu menu = TestEntities.menu(nileKitchen);
        kofta = TestEntities.menuItem(MENU_ITEM_ID, menu, "Kofta Platter", "185.00", 5);

        lenient().when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        lenient().when(menuItemRepository.findById(MENU_ITEM_ID)).thenReturn(Optional.of(kofta));
    }

    @Test
    void rejectsAQuantityOfZeroOrLess() {
        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 0))
                .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void rejectsAnUnknownCustomer() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(99L, MENU_ITEM_ID, 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsAnUnknownMenuItem() {
        when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, 99L, 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsAClosedRestaurant() {
        Restaurant closed = TestEntities.restaurant(2L, "Burger Yard", false);
        MenuItem fries = TestEntities.menuItem(MENU_ITEM_ID, TestEntities.menu(closed), "Fries", "35.00", 5);
        when(menuItemRepository.findById(MENU_ITEM_ID)).thenReturn(Optional.of(fries));

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1))
                .isInstanceOf(RestaurantClosedException.class)
                .hasMessage("Restaurant is closed");
    }

    @Test
    void rejectsAQuantityBeyondStock() {
        when(cartRepository.findWithItemsByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 6))
                .isInstanceOf(OutOfStockException.class)
                .hasMessage("Item is not in stock");
    }

    @Test
    void rejectsAnIncrementThatWouldExceedStock() {
        Cart cart = Cart.create(customer);
        cart.addItem(kofta, 3);
        when(cartRepository.findWithItemsByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 3))
                .isInstanceOf(OutOfStockException.class)
                .hasMessage("Item is not in stock");
    }

    @Test
    void rejectsAnItemFromADifferentRestaurant() {
        Restaurant burgerYard = TestEntities.restaurant(2L, "Burger Yard", true);
        MenuItem burger = TestEntities.menuItem(20L, TestEntities.menu(burgerYard), "Classic Burger", "120.00", 5);
        Cart cart = Cart.create(customer);
        cart.addItem(burger, 1);
        when(cartRepository.findWithItemsByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1))
                .isInstanceOf(DifferentRestaurantException.class)
                .hasMessage("Item is of different restaurant");
    }

    @Test
    void acceptsAnyRestaurantIntoAnExistingEmptyCart() {
        when(cartRepository.findWithItemsByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(Cart.create(customer)));

        cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1);

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void createsACartWhenTheCustomerHasNone() {
        when(cartRepository.findWithItemsByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2);

        verify(cartRepository).save(any(Cart.class));
    }
}
```

`rejectsAnIncrementThatWouldExceedStock` pins the resulting-quantity rule: stock 5, cart holds 3, adding 3 more must fail even though 3 alone would pass. `acceptsAnyRestaurantIntoAnExistingEmptyCart` pins the derived-restaurant rule — an emptied cart is not locked to its former restaurant.

- [ ] **Step 3: Run both tests to verify they fail**

```bash
./mvnw -B test -Dtest='CartServiceTest,CartRestMapperTest'
```

Expected: compilation failure — `CartService` takes four constructor arguments and `CartRestMapper.toResponse` does not exist.

- [ ] **Step 4: Reshape the response records**

`CartItemResponse.java`:

```java
package com.mentorship.restaurant.cart.controller.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long menuItemId,
        String menuItemName,
        BigDecimal price,
        Integer quantity,
        BigDecimal lineTotal) {}
```

`CartResponse.java`:

```java
package com.mentorship.restaurant.cart.controller.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Long customerId,
        Long restaurantId,
        List<CartItemResponse> items,
        BigDecimal total) {}
```

`price` is the captured line price, not the current menu price. `restaurantId` is null for an empty cart.

- [ ] **Step 5: Implement the mapper**

```java
package com.mentorship.restaurant.cart.model.mapper;

import com.mentorship.restaurant.cart.controller.response.CartItemResponse;
import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.entity.Customer;
import com.mentorship.restaurant.cart.model.entity.Restaurant;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CartRestMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items =
                cart.getItems().stream().map(this::toItemResponse).toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Customer customer = cart.getCustomer();
        return new CartResponse(
                cart.getId(),
                customer == null ? null : customer.getId(),
                cart.getRestaurant().map(Restaurant::getId).orElse(null),
                items,
                total);
    }

    private CartItemResponse toItemResponse(CartItem line) {
        return new CartItemResponse(
                line.getId(),
                line.getMenuItem().getId(),
                line.getMenuItem().getMenuItemName(),
                line.getPrice(),
                line.getQuantity(),
                line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
    }
}
```

- [ ] **Step 6: Implement the service**

Add the mapper to the constructor and replace the body of `addItem`. Leave the other five methods throwing `UnsupportedOperationException` — they belong to #3, #4, #5, #6, #7.

```java
    private final CartRestMapper cartRestMapper;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CustomerRepository customerRepository,
            MenuItemRepository menuItemRepository,
            CartRestMapper cartRestMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerRepository = customerRepository;
        this.menuItemRepository = menuItemRepository;
        this.cartRestMapper = cartRestMapper;
    }

    @Transactional
    public CartResponse addItem(Long customerId, Long menuItemId, Integer quantity) {
        AddToCartHandler command = new AddToCartHandler(customerId, menuItemId, quantity);

        if (command.quantity() == null || command.quantity() <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero");
        }

        Customer customer = customerRepository
                .findById(command.customerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        MenuItem menuItem = menuItemRepository
                .findById(command.menuItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));
        Restaurant restaurant = menuItem.getRestaurant();

        // Step 2
        if (!restaurant.isOpen()) {
            throw new RestaurantClosedException("Restaurant is closed");
        }

        Cart cart = cartRepository.findWithItemsByCustomerId(command.customerId()).orElse(null);

        // Step 3 - against the resulting quantity, so repeated adds cannot walk
        // past available stock one call at a time.
        int alreadyInCart = cart == null
                ? 0
                : cart.findItem(menuItem.getId()).map(CartItem::getQuantity).orElse(0);
        if (!menuItem.hasStockFor(alreadyInCart + command.quantity())) {
            throw new OutOfStockException("Item is not in stock");
        }

        // Step 4 - an empty cart accepts any restaurant.
        if (cart != null && !cart.accepts(restaurant)) {
            throw new DifferentRestaurantException("Item is of different restaurant");
        }

        // Step 5
        if (cart == null) {
            cart = cartRepository.save(Cart.create(customer));
        }

        // Step 6 / Flow 1a
        cart.addItem(menuItem, command.quantity());

        return cartRestMapper.toResponse(cart);
    }
```

`alreadyInCart + command.quantity()` is `int` arithmetic. It cannot overflow because Task 6 caps the request quantity with `@Max`; without that cap `Integer.MAX_VALUE` would wrap negative and pass the stock check.

`cartItemRepository` stays injected but unused here — `CascadeType.ALL` on `Cart.items` persists new lines, and other use-cases need it.

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./mvnw -B test -Dtest='CartServiceTest,CartRestMapperTest'
```

Expected: PASS, 12 tests.

- [ ] **Step 8: Format and commit**

```bash
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart src/test/java/com/mentorship/restaurant/cart
git commit -m "feat(cart): implement add-to-cart in the service layer"
```

---

### Task 6: The endpoint and error responses

**Files:**
- Modify: `controller/CartController.java`, `model/request/AddCartItemRequest.java`
- Create: `controller/CartExceptionHandler.java`, `controller/response/ErrorResponse.java`

**Interfaces:**
- Consumes: Tasks 4–5.
- Produces: `POST /api/v1/carts/items` returning `201 Created` with a `CartResponse`; `ErrorResponse(String message)`.

- [ ] **Step 1: Add validation to the request record**

```java
package com.mentorship.restaurant.cart.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull Long customerId,
        @NotNull Long menuItemId,
        // Max keeps the service's int arithmetic safe: without it a quantity of
        // Integer.MAX_VALUE would wrap negative when added to the existing line
        // and slip past the stock check.
        @NotNull @Min(1) @Max(1000) Integer quantity) {}
```

- [ ] **Step 2: Create the error response record**

```java
package com.mentorship.restaurant.cart.controller.response;

public record ErrorResponse(String message) {}
```

- [ ] **Step 3: Create the exception handler**

```java
package com.mentorship.restaurant.cart.controller;

import com.mentorship.restaurant.cart.controller.response.ErrorResponse;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.NotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.exception.RestaurantClosedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CartExceptionHandler {

    @ExceptionHandler({
        RestaurantClosedException.class,
        OutOfStockException.class,
        DifferentRestaurantException.class
    })
    ResponseEntity<ErrorResponse> handleConflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(InvalidQuantityException.class)
    ResponseEntity<ErrorResponse> handleInvalidQuantity(InvalidQuantityException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}
```

409 for the three rejections: the request is well formed, but the state of the restaurant, the stock or the cart forbids it.

- [ ] **Step 4: Add the endpoint**

```java
package com.mentorship.restaurant.cart.controller;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.model.request.AddCartItemRequest;
import com.mentorship.restaurant.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(request.customerId(), request.menuItemId(), request.quantity());
    }
}
```

`customerId` comes from the body until authentication exists, at which point it moves to the authenticated principal.

- [ ] **Step 5: Verify existing tests still pass, format and commit**

```bash
./mvnw -B test
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart/controller src/main/java/com/mentorship/restaurant/cart/model/request
git commit -m "feat(cart): expose POST /api/v1/carts/items"
```

---

### Task 7: End-to-end verification

Walks the main flow and the rejections the seeded data can express, over real HTTP.

**Files:**
- Modify: `pom.xml`
- Test: `src/test/java/com/mentorship/restaurant/cart/AddCartItemEndpointTest.java`

**Interfaces:**
- Consumes: Tasks 1–6.

Three constraints shape this test:

1. **`spring-boot-starter-webmvc-test` was removed from the pom** when the health test was deleted. It is what brings `spring-boot-resttestclient`, so `RestTestClient` is unavailable until it is restored.
2. **`@Transactional` rolls back nothing the server did.** Requests run on server threads in their own transactions. The test therefore uses **customer 3**, seeded by V6 with no cart, and cleans up only that customer's cart. It never touches carts 1 or 2.
3. **Closed-restaurant and out-of-stock need fixtures the seed data lacks** — both restaurants are open and every item has stock 40+. Those flows are covered by `CartServiceTest`.

- [ ] **Step 1: Restore the test dependency**

In `pom.xml`, after `spring-boot-starter-test`:

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
```

- [ ] **Step 2: Write the end-to-end test**

```java
package com.mentorship.restaurant.cart;

import com.mentorship.restaurant.cart.repository.CartRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AddCartItemEndpointTest {

    /** Seeded by V6 with no cart, reserved for this test. Carts 1 and 2 are never touched. */
    private static final long CUSTOMER = 3L;

    private static final long KOFTA = 1L;          // restaurant 1, 185.00
    private static final long FALAFEL = 2L;        // restaurant 1, 65.00
    private static final long CLASSIC_BURGER = 4L; // restaurant 2, 120.00

    @Autowired private RestTestClient client;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CartRepository cartRepository;

    @BeforeEach
    @AfterEach
    void clearThisCustomersCart() {
        // Requests run in the server's own transactions, so nothing rolls back.
        // Scoped to this customer: a blanket delete would destroy V3's seeded carts,
        // and Flyway will not re-run V3 to restore them.
        jdbcTemplate.update(
                "DELETE FROM cart_items WHERE cart_id IN (SELECT cart_id FROM carts WHERE customer_id = ?)",
                CUSTOMER);
        jdbcTemplate.update("DELETE FROM carts WHERE customer_id = ?", CUSTOMER);
    }

    @Test
    void addsAnItemAndCreatesTheCart() {
        client.post()
                .uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(KOFTA, 2))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.restaurantId")
                .isEqualTo(1)
                .jsonPath("$.items.length()")
                .isEqualTo(1)
                .jsonPath("$.items[0].menuItemName")
                .isEqualTo("Kofta Platter")
                .jsonPath("$.items[0].quantity")
                .isEqualTo(2)
                .jsonPath("$.total")
                .isEqualTo(370.00);

        assertThat(cartRepository.findWithItemsByCustomerId(CUSTOMER)).isPresent();
    }

    @Test
    void incrementsInsteadOfAddingASecondLine() {
        addItem(KOFTA, 2);

        client.post()
                .uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(KOFTA, 1))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.items.length()")
                .isEqualTo(1)
                .jsonPath("$.items[0].quantity")
                .isEqualTo(3)
                .jsonPath("$.total")
                .isEqualTo(555.00);
    }

    @Test
    void keepsSeparateLinesForDifferentItemsOfTheSameRestaurant() {
        addItem(KOFTA, 1);

        client.post()
                .uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(FALAFEL, 1))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.items.length()")
                .isEqualTo(2)
                .jsonPath("$.total")
                .isEqualTo(250.00);
    }

    @Test
    void rejectsAnItemFromADifferentRestaurant() {
        addItem(KOFTA, 1);

        client.post()
                .uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(CLASSIC_BURGER, 1))
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo("Item is of different restaurant");
    }

    @Test
    void rejectsAnUnknownItem() {
        client.post()
                .uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(9999L, 1))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void rejectsAQuantityOfZero() {
        client.post()
                .uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(KOFTA, 0))
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    private String body(long menuItemId, int quantity) {
        return """
                {"customerId": %d, "menuItemId": %d, "quantity": %d}
                """
                .formatted(CUSTOMER, menuItemId, quantity);
    }

    private void addItem(long menuItemId, int quantity) {
        client.post()
                .uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(menuItemId, quantity))
                .exchange()
                .expectStatus()
                .isCreated();
    }
}
```

- [ ] **Step 3: Run it**

```bash
./mvnw -B test -Dtest=AddCartItemEndpointTest
```

Expected: PASS, 6 tests.

- [ ] **Step 4: Run the whole suite**

```bash
./mvnw -B verify
```

Expected: PASS. 13 (entities) + 12 (service, mapper) + 6 (endpoint) + 1 (context) = 32 tests.

- [ ] **Step 5: Format and commit**

```bash
./mvnw -B spotless:apply
git add pom.xml src/test/java/com/mentorship/restaurant/cart/AddCartItemEndpointTest.java
git commit -m "test(cart): cover add-to-cart end to end"
```

---

### Task 8: Correct the use-case doc

The doc contradicts the schema and the team's decision on two points.

**Files:**
- Modify: `add-cart-item.md` in this directory

- [ ] **Step 1: Replace the Totals section**

```markdown
## Totals

`CartItem` captures the item's price when the line is created
(`cart_items.cart_item_price`). Totals are computed from those captured prices,
never from the current menu price.

A cart therefore shows what the customer agreed to pay. If a restaurant changes
a price, carts already holding that item are unaffected; the new price applies
only to lines created afterwards. Incrementing an existing line keeps its
original price.
```

- [ ] **Step 2: Replace the Data Model table**

```markdown
| Table | Column | Purpose |
| --- | --- | --- |
| `restaurants` | `is_open` (boolean, not null) | Step 2 — open/closed check. A flag, not opening hours. |
| `menu_items` | `menu_item_name` (varchar, not null) | The table held only `menu_item_code`; a cart response needs a readable name. |

Already present: `menu_items.menu_item_stock` for the stock check,
`cart_items.cart_item_price` for the price snapshot, and
`uq_cart_items_cart_menu_item`, which enforces Rule 1 in the database.

The cart's restaurant is **not stored**. It is derived from the cart's lines
(`CartItem -> MenuItem -> Menu -> Restaurant`), and an empty cart has none —
which is why an empty cart accepts an item from any restaurant.
```

- [ ] **Step 3: Update the diagrams**

The flowchart and sequence diagram both describe totals computed on read and a cart that stores its restaurant. Update the mapper note to say totals come from the captured line prices, and the cart-creation step to `Create cart for customer` with no restaurant.

- [ ] **Step 4: Format and commit**

```bash
git add add-cart-item.md
git commit -m "docs: correct add-cart-item totals and the derived restaurant"
```

- [ ] **Step 5: Push**

```bash
git push
```

PR #27 already exists for this branch; the implementation commits land on it.

---

## Out of Scope

- Item options and options-based uniqueness.
- Real opening hours — `is_open` is a flag.
- Authentication; `customerId` stays in the request body.
- **Concurrency.** Two simultaneous adds by the same customer can both find no cart, and the second violates `carts.customer_id`'s unique constraint as a 500; concurrent increments can lose an update. The database constraints prevent corruption. Noted on PR #27.
- **Stock decrement.** Stock is checked but never reduced; reserving it belongs to checkout (#7). Two customers can both pass the check for the last item.
- Cart status / lifecycle. `carts.customer_id` stays unique, which checkout (#7) must resolve.
- **Per-test seed data.** The agreed direction is that each test seeds what it needs instead of sharing global seeds. V6's cart-less customer is a stopgap, not that refactor.
- The other five `CartService` methods (#3, #4, #5, #6, #7).
- The Surefire/Failsafe split (#26).
