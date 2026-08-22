# Add To Cart API (GH-21) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `POST /api/v1/carts/items` so a customer can add a menu item to their cart, per `.docs/use-cases/cart-management/add-cart-item/add-cart-item.md`.

**Architecture:** The insert-vs-increment rule lives on the `Cart` entity, not the service. `CartService.addItem` orchestrates: load, guard (open → stock → same restaurant), get-or-create cart, delegate to the domain, map to a response. Flyway owns the schema; Hibernate validates the entities against it at boot.

**Tech Stack:** Java 17, Spring Boot 4.1.0, Spring Data JPA, Flyway, PostgreSQL 16 (via `compose.yaml`), JUnit 5 + Mockito + RestTestClient.

**Baseline:** assumes `feat/GH-15-ci` (PR #25) is merged. That branch provides `flyway-database-postgresql`, entities aligned to the migrations, tests running against real PostgreSQL with `ddl-auto=validate`, and `spring-boot-starter-webmvc-test`.

---

## Decisions this plan makes — confirm before starting

The schema on `main` moved after the use-case doc was written. Two consequences change the design, and both contradict the doc as it stands.

**1. The price snapshot is mandatory, not absent.** `cart_items.cart_item_price NUMERIC(12,2) NOT NULL CHECK (>= 0)` already exists, so a `CartItem` cannot be inserted without it. The doc's Totals section says the opposite — that no snapshot exists and a cart always reflects current menu prices.

This plan **captures `menu_item_price` into `cart_item_price` when a line is created** and computes totals from the captured value. A later menu price change does not reprice open carts. This is the better behaviour; the doc gets corrected in Task 8.

*Sub-decision:* incrementing an existing line **keeps the original price** rather than refreshing it. The customer pays what they saw when they added it. A weighted-average rewrite is the alternative and is more surprising.

**2. `menu_items` has no name column.** It has `menu_item_code` (`'NK-KOFTA-01'`) and `menu_item_image_url`, but nothing human-readable, so `CartItemResponse` has nothing to show.

This plan **adds `menu_item_name VARCHAR(150) NOT NULL`** in the same migration and backfills the six seeded rows. The alternative — returning the code as the name — avoids a schema change but ships a visibly wrong API.

**3. Item options remain out of scope.** `uq_cart_items_cart_menu_item UNIQUE (cart_id, menu_item_id)` already enforces one line per item, which matches the doc's reduced Rule 1.

## Global Constraints

- Java 17. Spring Boot 4.1.0 (parent POM; do not change the version).
- Package root: `com.mentorship.restaurant.cart`.
- **No new Maven dependencies.**
- `spring.jpa.hibernate.ddl-auto=validate` everywhere. Flyway owns the schema.
- Rejection messages verbatim from the spec: `"Restaurant is closed"`, `"Item is not in stock"`, `"Item is of different restaurant"`.
- A `CartItem` is unique by menu item alone.
- Stock is checked against the **resulting** quantity (existing line + requested), never the requested quantity alone.
- Auth is out of scope: `customerId` arrives in the request body.
- Entities keep their `protected` no-arg constructor for JPA; construction goes through static factories.
- Endpoints live under `/api/v1`, matching `/api/v1/health`.
- Every task ends with a commit. Branch `feat/GH-21-api-add-cart-item`, cut from `main` after #25 merges.

## Schema as it stands

```
users(user_id, user_name, user_email UQ, user_password)
customers(customer_id, user_id UQ -> users)
restaurants(restaurant_id, user_id UQ -> users, restaurant_name, restaurant_description)
menus(menu_id, restaurant_id -> restaurants, menu_code UQ, menu_category)
menu_items(menu_item_id, menu_id -> menus, menu_item_code UQ, menu_item_image_url,
           menu_item_price NUMERIC(12,2) CHECK >= 0)
carts(cart_id, customer_id UQ -> customers)
cart_items(cart_item_id, cart_id -> carts, menu_item_id -> menu_items,
           cart_item_quantity INTEGER CHECK > 0,
           cart_item_price NUMERIC(12,2) CHECK >= 0,
           UNIQUE (cart_id, menu_item_id))
```

`V2__seed_reference_data.sql` seeds customers 1–2, restaurants 1–2, menus 1–2, and menu items 1–3 (restaurant 1) and 4–6 (restaurant 2).

**Tests use the seeded rows rather than constructing entities.** Several NOT NULL columns — `restaurants.user_id`, `menus.menu_code`, `menus.menu_category`, `menu_items.menu_item_code` — are mapped by no entity, so an entity-built `Restaurant` or `MenuItem` cannot be inserted at all. This shapes every test in the plan.

## File Structure

**Migration (new)**
- `src/main/resources/db/migration/V3__add_cart_item_fields.sql`

**Domain (modified)**
- `model/entity/Restaurant.java` — `isOpen` + accessors
- `model/entity/MenuItem.java` — `menuItemName`, `stockQuantity`, `hasStockFor(int)`, accessors
- `model/entity/Cart.java` — `restaurant`, `items`, `create`, `addItem`, `findItem`, `belongsTo`, accessors
- `model/entity/CartItem.java` — `price`, `create`, `increaseQuantityBy`, accessors
- `model/entity/Menu.java`, `Customer.java` — accessors only

**Errors (new + modified)**
- `exception/RestaurantClosedException.java`, `OutOfStockException.java`, `DifferentRestaurantException.java`, `NotFoundException.java` — new
- `exception/InvalidQuantityException.java` — message constructor
- `controller/CartExceptionHandler.java`, `controller/response/ErrorResponse.java` — new

**Plumbing (modified)**
- `repository/CartRepository.java` — two query methods
- `service/CartService.java` — implement `addItem`
- `model/mapper/CartRestMapper.java` — build `CartResponse`, compute the total
- `controller/response/CartResponse.java`, `CartItemResponse.java` — reshape
- `controller/CartController.java` — the endpoint
- `model/request/AddCartItemRequest.java` — validation

**Tests (new)**
- `model/entity/TestEntities.java` — shared in-memory entity builders
- `model/entity/CartTest.java`, `MenuItemTest.java` — pure unit
- `model/mapper/CartRestMapperTest.java` — pure unit
- `service/CartServiceTest.java` — Mockito, covers all rejections
- `repository/CartRepositoryTest.java` — `@DataJpaTest`
- `cart/AddCartItemEndpointTest.java` — `@SpringBootTest(RANDOM_PORT)` + `RestTestClient`

**Docs (modified)**
- `.docs/use-cases/cart-management/add-cart-item/add-cart-item.md`

---

### Task 1: Migration for the new fields

**Files:**
- Create: `src/main/resources/db/migration/V3__add_cart_item_fields.sql`
- Test: `src/test/java/com/mentorship/restaurant/RestaurantApplicationTests.java` (exists; booting it is the test)

**Interfaces:**
- Consumes: the V1/V2 schema.
- Produces: `restaurants.is_open BOOLEAN NOT NULL`, `menu_items.stock_quantity INTEGER NOT NULL`, `menu_items.menu_item_name VARCHAR(150) NOT NULL`, `carts.restaurant_id BIGINT NOT NULL`.

- [ ] **Step 1: Write the migration**

```sql
-- Step 2 of the use-case: restaurant open/closed. A flag, not opening hours.
ALTER TABLE restaurants
    ADD COLUMN is_open BOOLEAN NOT NULL DEFAULT TRUE;

-- Step 3 of the use-case: stock check.
ALTER TABLE menu_items
    ADD COLUMN stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0);

-- menu_items carried no human-readable name, only menu_item_code.
ALTER TABLE menu_items
    ADD COLUMN menu_item_name VARCHAR(150);

UPDATE menu_items SET menu_item_name = CASE menu_item_code
    WHEN 'NK-KOFTA-01'   THEN 'Kofta Platter'
    WHEN 'NK-FALAFEL-01' THEN 'Falafel Sandwich'
    WHEN 'NK-KOSHARI-01' THEN 'Koshari Bowl'
    WHEN 'BY-CLASSIC-01' THEN 'Classic Burger'
    WHEN 'BY-CHICKEN-01' THEN 'Chicken Burger'
    WHEN 'BY-FRIES-01'   THEN 'Fries'
    ELSE menu_item_code
END;

ALTER TABLE menu_items
    ALTER COLUMN menu_item_name SET NOT NULL;

-- Give the seeded items stock so they are orderable.
UPDATE menu_items SET stock_quantity = 20;

-- Business Rule 2: a cart belongs to exactly one restaurant. Added nullable,
-- backfilled from the cart's existing lines, then constrained.
ALTER TABLE carts
    ADD COLUMN restaurant_id BIGINT REFERENCES restaurants (restaurant_id);

UPDATE carts c
SET restaurant_id = (
    SELECT m.restaurant_id
    FROM cart_items ci
             JOIN menu_items mi ON mi.menu_item_id = ci.menu_item_id
             JOIN menus m ON m.menu_id = mi.menu_id
    WHERE ci.cart_id = c.cart_id
    LIMIT 1
)
WHERE restaurant_id IS NULL;

DELETE FROM cart_items WHERE cart_id IN (SELECT cart_id FROM carts WHERE restaurant_id IS NULL);
DELETE FROM carts WHERE restaurant_id IS NULL;

ALTER TABLE carts
    ALTER COLUMN restaurant_id SET NOT NULL;

CREATE INDEX idx_carts_restaurant_id ON carts (restaurant_id);
```

An empty cart has no derivable restaurant, so it is deleted rather than guessed at.

- [ ] **Step 2: Apply it against a clean database**

```bash
docker compose down -v
docker compose up -d --wait postgres
./mvnw -B test -Dtest=RestaurantApplicationTests
```

Expected: PASS, and the log shows `Successfully applied 3 migrations`.

- [ ] **Step 3: Confirm the data landed**

```bash
docker exec restaurant-postgres psql -U postgres -d restaurant -c \
  "SELECT menu_item_name, menu_item_price, stock_quantity FROM menu_items ORDER BY menu_item_id LIMIT 3;"
```

Expected: three named rows (`Kofta Platter`, `Falafel Sandwich`, `Koshari Bowl`) with `stock_quantity` 20.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V3__add_cart_item_fields.sql
git commit -m "feat(db): add open flag, stock, item name and cart restaurant"
```

---

### Task 2: Domain behaviour on the entities

**Files:**
- Modify: `model/entity/Restaurant.java`, `MenuItem.java`, `Cart.java`, `CartItem.java`, `Menu.java`, `Customer.java`
- Create: `src/test/java/com/mentorship/restaurant/cart/model/entity/TestEntities.java`
- Test: `src/test/java/com/mentorship/restaurant/cart/model/entity/CartTest.java`
- Test: `src/test/java/com/mentorship/restaurant/cart/model/entity/MenuItemTest.java`

**Interfaces:**
- Consumes: Task 1's columns.
- Produces:
  - `Restaurant.getId(): Long`, `getRestaurantName(): String`, `isOpen(): boolean`
  - `MenuItem.getId(): Long`, `getMenuItemName(): String`, `getMenuItemPrice(): BigDecimal`, `getStockQuantity(): Integer`, `getMenu(): Menu`, `getRestaurant(): Restaurant`, `hasStockFor(int): boolean`
  - `Cart.create(Customer, Restaurant): Cart`, `getId(): Long`, `getCustomer(): Customer`, `getRestaurant(): Restaurant`, `getItems(): List<CartItem>`, `addItem(MenuItem, int): CartItem`, `findItem(Long): Optional<CartItem>`, `belongsTo(Restaurant): boolean`
  - `CartItem.getId(): Long`, `getMenuItem(): MenuItem`, `getQuantity(): Integer`, `getPrice(): BigDecimal`
  - `Menu.getRestaurant(): Restaurant`, `Customer.getId(): Long`
  - `TestEntities.restaurant(Long, String, boolean)`, `menu(Restaurant)`, `menuItem(Long, Menu, String, String, int)`, `customer(Long)` — test scope only

Only `Cart` and `CartItem` get factories. `Restaurant`, `Menu` and `MenuItem` have unmapped NOT NULL columns and so are never constructed by application code.

- [ ] **Step 1: Write the test entity builders**

`Restaurant`, `Menu` and `MenuItem` cannot be built through a constructor, and unit tests must not need a database. `TestEntities` populates fields reflectively — acceptable in test scope, and it keeps production code free of setters that exist only for tests.

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
        set(item, "stockQuantity", stock);
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

    private TestEntities() {
    }
}
```

- [ ] **Step 2: Write the failing domain tests**

`CartTest.java`:

```java
package com.mentorship.restaurant.cart.model.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    private final Restaurant restaurant = TestEntities.restaurant(1L, "Nile Kitchen", true);
    private final Menu menu = TestEntities.menu(restaurant);
    private final MenuItem kofta = TestEntities.menuItem(1L, menu, "Kofta Platter", "185.00", 20);
    private final MenuItem falafel = TestEntities.menuItem(2L, menu, "Falafel Sandwich", "65.00", 20);

    @Test
    void addsANewLineWhenTheItemIsNotInTheCart() {
        Cart cart = Cart.create(null, restaurant);

        cart.addItem(kofta, 2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void capturesThePriceWhenTheLineIsCreated() {
        Cart cart = Cart.create(null, restaurant);

        cart.addItem(kofta, 1);

        assertThat(cart.getItems().get(0).getPrice()).isEqualByComparingTo("185.00");
    }

    @Test
    void incrementsTheExistingLineWhenTheItemIsAlreadyInTheCart() {
        Cart cart = Cart.create(null, restaurant);

        cart.addItem(kofta, 2);
        cart.addItem(kofta, 3);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void keepsTheOriginalPriceWhenIncrementing() {
        Cart cart = Cart.create(null, restaurant);
        cart.addItem(kofta, 1);
        MenuItem repriced = TestEntities.menuItem(1L, menu, "Kofta Platter", "999.00", 20);

        cart.addItem(repriced, 1);

        assertThat(cart.getItems().get(0).getPrice()).isEqualByComparingTo("185.00");
    }

    @Test
    void keepsSeparateLinesForDifferentItems() {
        Cart cart = Cart.create(null, restaurant);

        cart.addItem(kofta, 1);
        cart.addItem(falafel, 1);

        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    void findsAnExistingLineByMenuItemId() {
        Cart cart = Cart.create(null, restaurant);
        cart.addItem(kofta, 1);

        assertThat(cart.findItem(1L)).isPresent();
        assertThat(cart.findItem(2L)).isEmpty();
    }

    @Test
    void recognisesItsOwnRestaurant() {
        Cart cart = Cart.create(null, restaurant);

        assertThat(cart.belongsTo(restaurant)).isTrue();
        assertThat(cart.belongsTo(TestEntities.restaurant(2L, "Burger Yard", true))).isFalse();
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

Expected: compilation failure — `cannot find symbol: method create(...)`, `hasStockFor(...)`.

- [ ] **Step 4: Implement the entity changes**

`Restaurant.java` — add after `restaurantName`:

```java
    @Column(name = "is_open", nullable = false)
    private boolean open;

    protected Restaurant() {
    }

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

`Menu.java` — accessors only:

```java
    public Long getId() {
        return id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }
```

`Customer.java` — accessor only:

```java
    public Long getId() {
        return id;
    }
```

`MenuItem.java` — add the two new columns, rename the price field to match its column, add behaviour. Delete the existing `itemPrice` field; `menuItemPrice` below replaces it:

```java
    @Column(name = "menu_item_name", nullable = false, length = 150)
    private String menuItemName;

    @Column(name = "menu_item_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal menuItemPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    protected MenuItem() {
    }

    public boolean hasStockFor(int quantity) {
        return stockQuantity >= quantity;
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

    public Integer getStockQuantity() {
        return stockQuantity;
    }
```

`CartItem.java` — add the mandatory price snapshot plus behaviour:

```java
    @Column(name = "cart_item_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected CartItem() {
    }

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

`create` and `increaseQuantityBy` are package-private on purpose: a `CartItem` is only ever created or changed through its `Cart`.

`Cart.java` — the substantive one. Add imports `jakarta.persistence.CascadeType`, `ManyToOne`, `OneToMany`, `java.util.ArrayList`, `List`, `Objects`, `Optional`:

```java
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public static Cart create(Customer customer, Restaurant restaurant) {
        Cart cart = new Cart();
        cart.customer = customer;
        cart.restaurant = restaurant;
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

    public boolean belongsTo(Restaurant other) {
        return Objects.equals(restaurant.getId(), other.getId());
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Restaurant getRestaurant() {
        return restaurant;
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

Expected: PASS, 10 tests.

- [ ] **Step 6: Verify the entities still match the schema**

```bash
docker compose up -d --wait postgres
./mvnw -B test -Dtest=RestaurantApplicationTests
```

Expected: PASS. This catches a mistyped column name in the new mappings.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/mentorship/restaurant/cart/model/entity src/test/java/com/mentorship/restaurant/cart/model/entity
git commit -m "feat(cart): give the cart domain behaviour and the new fields"
```

---

### Task 3: Repository lookups

**Files:**
- Modify: `src/main/java/com/mentorship/restaurant/cart/repository/CartRepository.java`
- Test: `src/test/java/com/mentorship/restaurant/cart/repository/CartRepositoryTest.java`

**Interfaces:**
- Consumes: Task 2's entities.
- Produces: `CartRepository.findByCustomerId(Long): Optional<Cart>`, `CartRepository.findWithItemsByCustomerId(Long): Optional<Cart>`

- [ ] **Step 1: Write the failing repository test**

`@AutoConfigureTestDatabase(replace = NONE)` is essential — without it Boot substitutes an embedded database and the PostgreSQL migrations never run. Fixtures are the seeded rows.

```java
package com.mentorship.restaurant.cart.repository;

import java.util.Optional;

import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.Customer;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.cart.model.entity.Restaurant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartRepositoryTest {

    private static final Long SEEDED_CUSTOMER = 1L;
    private static final Long SEEDED_RESTAURANT = 1L;
    private static final Long SEEDED_MENU_ITEM = 1L;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void returnsEmptyWhenTheCustomerHasNoCart() {
        assertThat(cartRepository.findByCustomerId(SEEDED_CUSTOMER)).isEmpty();
    }

    @Test
    void findsACartByCustomerId() {
        cartRepository.save(Cart.create(customer(), restaurant()));

        Optional<Cart> found = cartRepository.findByCustomerId(SEEDED_CUSTOMER);

        assertThat(found).isPresent();
        assertThat(found.get().getRestaurant().getId()).isEqualTo(SEEDED_RESTAURANT);
    }

    @Test
    void loadsACartTogetherWithItsItems() {
        Cart cart = Cart.create(customer(), restaurant());
        cart.addItem(menuItem(), 2);
        cartRepository.save(cart);

        entityManager.flush();
        entityManager.clear();

        Optional<Cart> found = cartRepository.findWithItemsByCustomerId(SEEDED_CUSTOMER);

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(found.get().getItems().get(0).getPrice()).isEqualByComparingTo("185.00");
    }

    private Customer customer() {
        return entityManager.getReference(Customer.class, SEEDED_CUSTOMER);
    }

    private Restaurant restaurant() {
        return entityManager.find(Restaurant.class, SEEDED_RESTAURANT);
    }

    private MenuItem menuItem() {
        return entityManager.find(MenuItem.class, SEEDED_MENU_ITEM);
    }
}
```

`@DataJpaTest` is transactional and rolls back after each test, so the seeded data survives untouched.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./mvnw -B test -Dtest=CartRepositoryTest
```

Expected: compilation failure — `cannot find symbol: method findByCustomerId`.

- [ ] **Step 3: Add the query methods**

```java
package com.mentorship.restaurant.cart.repository;

import java.util.Optional;

import com.mentorship.restaurant.cart.model.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerId(Long customerId);

    @Query("""
            select c from Cart c
            left join fetch c.items i
            left join fetch i.menuItem
            where c.customer.id = :customerId
            """)
    Optional<Cart> findWithItemsByCustomerId(Long customerId);
}
```

The `join fetch` on `menuItem` matters: the mapper reads each line's name, and with `open-in-view=false` a lazy hop outside the transaction throws `LazyInitializationException`.

- [ ] **Step 4: Run the test to verify it passes**

```bash
docker compose up -d --wait postgres
./mvnw -B test -Dtest=CartRepositoryTest
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mentorship/restaurant/cart/repository src/test/java/com/mentorship/restaurant/cart/repository
git commit -m "feat(cart): add cart lookup queries"
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

- [ ] **Step 3: Verify it compiles**

```bash
./mvnw -B -q compile
```

Expected: no output, exit 0.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mentorship/restaurant/cart/exception
git commit -m "feat(cart): add add-to-cart rejection exceptions"
```

---

### Task 5: The service and the mapper

**Files:**
- Modify: `service/CartService.java`, `model/mapper/CartRestMapper.java`, `controller/response/CartResponse.java`, `controller/response/CartItemResponse.java`
- Create: `src/test/java/com/mentorship/restaurant/cart/model/mapper/CartFixtures.java`
- Test: `src/test/java/com/mentorship/restaurant/cart/model/mapper/CartRestMapperTest.java`
- Test: `src/test/java/com/mentorship/restaurant/cart/service/CartServiceTest.java`

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
        return Cart.create(null, RESTAURANT);
    }

    static Cart cartWith(String firstPrice, int firstQuantity, String secondPrice, int secondQuantity) {
        Cart cart = emptyCart();
        MenuItem first = TestEntities.menuItem(1L, MENU, "Kofta Platter", firstPrice, 20);
        MenuItem second = TestEntities.menuItem(2L, MENU, "Falafel Sandwich", secondPrice, 20);
        cart.addItem(first, firstQuantity);
        cart.addItem(second, secondQuantity);
        return cart;
    }

    private CartFixtures() {
    }
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
    void returnsAZeroTotalForAnEmptyCart() {
        CartResponse response = mapper.toResponse(CartFixtures.emptyCart());

        assertThat(response.total()).isEqualByComparingTo("0");
        assertThat(response.items()).isEmpty();
    }
}
```

- [ ] **Step 2: Write the failing service test**

```java
package com.mentorship.restaurant.cart.service;

import java.util.Optional;

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

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private MenuItemRepository menuItemRepository;

    private CartService cartService;

    private Customer customer;
    private Restaurant openRestaurant;
    private MenuItem kofta;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, customerRepository,
                menuItemRepository, new CartRestMapper());

        customer = TestEntities.customer(CUSTOMER_ID);
        openRestaurant = TestEntities.restaurant(1L, "Nile Kitchen", true);
        Menu menu = TestEntities.menu(openRestaurant);
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
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 6))
                .isInstanceOf(OutOfStockException.class)
                .hasMessage("Item is not in stock");
    }

    @Test
    void rejectsAnIncrementThatWouldExceedStock() {
        Cart cart = Cart.create(customer, openRestaurant);
        cart.addItem(kofta, 3);
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 3))
                .isInstanceOf(OutOfStockException.class)
                .hasMessage("Item is not in stock");
    }

    @Test
    void rejectsAnItemFromADifferentRestaurant() {
        Cart cart = Cart.create(customer, TestEntities.restaurant(2L, "Burger Yard", true));
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1))
                .isInstanceOf(DifferentRestaurantException.class)
                .hasMessage("Item is of different restaurant");
    }

    @Test
    void createsACartWhenTheCustomerHasNone() {
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(call -> call.getArgument(0));

        cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void reusesTheExistingCart() {
        Cart cart = Cart.create(customer, openRestaurant);
        when(cartRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(cart));

        cartService.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2);

        verify(cartRepository, never()).save(any(Cart.class));
    }
}
```

`rejectsAnIncrementThatWouldExceedStock` pins the resulting-quantity rule: stock 5, cart already holds 3, adding 3 more must fail even though 3 alone would pass.

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
        BigDecimal lineTotal
) {
}
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
        BigDecimal total
) {
}
```

`price` is the captured line price, not the current menu price.

- [ ] **Step 5: Implement the mapper**

```java
package com.mentorship.restaurant.cart.model.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.mentorship.restaurant.cart.controller.response.CartItemResponse;
import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CartRestMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Customer customer = cart.getCustomer();
        return new CartResponse(
                cart.getId(),
                customer == null ? null : customer.getId(),
                cart.getRestaurant().getId(),
                items,
                total
        );
    }

    private CartItemResponse toItemResponse(CartItem line) {
        return new CartItemResponse(
                line.getId(),
                line.getMenuItem().getId(),
                line.getMenuItem().getMenuItemName(),
                line.getPrice(),
                line.getQuantity(),
                line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity()))
        );
    }
}
```

The total derives from the captured line prices, so it is stable against menu price changes.

- [ ] **Step 6: Implement the service**

Add the mapper to the constructor and replace the body of `addItem`. Leave the other five methods throwing `UnsupportedOperationException` — they belong to #3, #4, #5, #6, #7.

Imports to add: `CartResponse`, the five exceptions, `Cart`, `CartItem`, `Customer`, `MenuItem`, `Restaurant`, `CartRestMapper`, `org.springframework.transaction.annotation.Transactional`.

```java
    private final CartRestMapper cartRestMapper;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CustomerRepository customerRepository,
            MenuItemRepository menuItemRepository,
            CartRestMapper cartRestMapper
    ) {
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

        Customer customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        MenuItem menuItem = menuItemRepository.findById(command.menuItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));
        Restaurant restaurant = menuItem.getRestaurant();

        // Step 2
        if (!restaurant.isOpen()) {
            throw new RestaurantClosedException("Restaurant is closed");
        }

        Cart cart = cartRepository.findByCustomerId(command.customerId()).orElse(null);

        // Step 3 - against the resulting quantity, so repeated adds cannot walk
        // past available stock one call at a time.
        int alreadyInCart = cart == null
                ? 0
                : cart.findItem(menuItem.getId()).map(CartItem::getQuantity).orElse(0);
        if (!menuItem.hasStockFor(alreadyInCart + command.quantity())) {
            throw new OutOfStockException("Item is not in stock");
        }

        // Step 4
        if (cart != null && !cart.belongsTo(restaurant)) {
            throw new DifferentRestaurantException("Item is of different restaurant");
        }

        // Step 5
        if (cart == null) {
            cart = cartRepository.save(Cart.create(customer, restaurant));
        }

        // Step 6 / Flow 1a
        cart.addItem(menuItem, command.quantity());

        return cartRestMapper.toResponse(cart);
    }
```

`cartItemRepository` stays injected but unused here — `CascadeType.ALL` on `Cart.items` persists new lines, and the other use-cases need the repository.

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./mvnw -B test -Dtest='CartServiceTest,CartRestMapperTest'
```

Expected: PASS, 11 tests.

- [ ] **Step 8: Commit**

```bash
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

No slice test here — Task 7 covers the endpoint end to end, and a `@WebMvcTest` would duplicate it.

- [ ] **Step 1: Add validation to the request record**

```java
package com.mentorship.restaurant.cart.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull Long customerId,
        @NotNull Long menuItemId,
        @NotNull @Min(1) Integer quantity
) {
}
```

- [ ] **Step 2: Create the error response record**

```java
package com.mentorship.restaurant.cart.controller.response;

public record ErrorResponse(String message) {
}
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

- [ ] **Step 5: Verify it compiles and existing tests still pass**

```bash
docker compose up -d --wait postgres
./mvnw -B test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mentorship/restaurant/cart/controller src/main/java/com/mentorship/restaurant/cart/model/request
git commit -m "feat(cart): expose POST /api/v1/carts/items"
```

---

### Task 7: End-to-end verification

Walks the main flow and the rejections that seeded data can express, over real HTTP against real PostgreSQL, following the pattern `HealthEndpointTest` established.

**Files:**
- Test: `src/test/java/com/mentorship/restaurant/cart/AddCartItemEndpointTest.java`

**Interfaces:**
- Consumes: Tasks 1–6.

Two constraints shape this test:

1. **`@Transactional` rolls back nothing the server did.** Requests run on server threads in their own transactions, and the compose volume persists between runs. A cart left behind breaks the next test, because `carts.customer_id` is unique. So the test deletes carts in `@BeforeEach`.
2. **Closed-restaurant and out-of-stock need fixtures the seed data lacks.** Both restaurants are open and every item has stock. Those flows are covered by `CartServiceTest` (Task 5), which is where they belong; this test proves what only end-to-end can — the wiring, the HTTP contract, and persistence.

- [ ] **Step 1: Write the end-to-end test**

```java
package com.mentorship.restaurant.cart;

import com.mentorship.restaurant.cart.repository.CartRepository;
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

    private static final long CUSTOMER = 1L;
    private static final long KOFTA = 1L;          // restaurant 1, 185.00
    private static final long FALAFEL = 2L;        // restaurant 1, 65.00
    private static final long CLASSIC_BURGER = 4L; // restaurant 2, 120.00

    @Autowired
    private RestTestClient client;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CartRepository cartRepository;

    @BeforeEach
    void clearCarts() {
        // Requests run in the server's own transactions, so nothing here rolls
        // back. carts.customer_id is unique, so leftovers break the next test.
        jdbcTemplate.update("DELETE FROM cart_items");
        jdbcTemplate.update("DELETE FROM carts");
    }

    @Test
    void addsAnItemAndCreatesTheCart() {
        client.post().uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(KOFTA, 2))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.restaurantId").isEqualTo(1)
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].menuItemName").isEqualTo("Kofta Platter")
                .jsonPath("$.items[0].quantity").isEqualTo(2)
                .jsonPath("$.total").isEqualTo(370.00);

        assertThat(cartRepository.findByCustomerId(CUSTOMER)).isPresent();
    }

    @Test
    void incrementsInsteadOfAddingASecondLine() {
        addItem(KOFTA, 2);

        client.post().uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(KOFTA, 1))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].quantity").isEqualTo(3)
                .jsonPath("$.total").isEqualTo(555.00);
    }

    @Test
    void keepsSeparateLinesForDifferentItemsOfTheSameRestaurant() {
        addItem(KOFTA, 1);

        client.post().uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(FALAFEL, 1))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(2)
                .jsonPath("$.total").isEqualTo(250.00);
    }

    @Test
    void rejectsAnItemFromADifferentRestaurant() {
        addItem(KOFTA, 1);

        client.post().uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(CLASSIC_BURGER, 1))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Item is of different restaurant");
    }

    @Test
    void rejectsAnUnknownItem() {
        client.post().uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(9999L, 1))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void rejectsAQuantityOfZero() {
        client.post().uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(KOFTA, 0))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private String body(long menuItemId, int quantity) {
        return """
                {"customerId": %d, "menuItemId": %d, "quantity": %d}
                """.formatted(CUSTOMER, menuItemId, quantity);
    }

    private void addItem(long menuItemId, int quantity) {
        client.post().uri("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(menuItemId, quantity))
                .exchange()
                .expectStatus().isCreated();
    }
}
```

- [ ] **Step 2: Run it**

```bash
docker compose up -d --wait postgres
./mvnw -B test -Dtest=AddCartItemEndpointTest
```

Expected: PASS, 6 tests.

- [ ] **Step 3: Run the whole suite from a clean database**

```bash
docker compose down -v
docker compose up -d --wait postgres
./mvnw -B verify
```

Expected: PASS. 10 (entities) + 3 (repository) + 11 (service, mapper) + 6 (endpoint) + 1 (health) + 1 (context) = 32 tests.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/mentorship/restaurant/cart/AddCartItemEndpointTest.java
git commit -m "test(cart): cover add-to-cart end to end"
```

---

### Task 8: Correct the use-case doc

The doc contradicts the schema on two points. Left uncorrected, the next reader implements the wrong thing.

**Files:**
- Modify: `.docs/use-cases/cart-management/add-cart-item/add-cart-item.md`

- [ ] **Step 1: Replace the Totals section**

It currently says totals are computed on read from the current menu price and that no snapshot exists. Replace with:

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
| `menu_items` | `stock_quantity` (integer, not null) | Step 3 — stock check. |
| `menu_items` | `menu_item_name` (varchar, not null) | The table held only `menu_item_code`; a cart response needs a readable name. |
| `carts` | `restaurant_id` (fk, not null) | Rule 2 — the cart's restaurant, stored rather than derived. |

Already present before this use-case: `cart_items.cart_item_price` (the price
snapshot) and `uq_cart_items_cart_menu_item`, which enforces Rule 1 in the
database.
```

- [ ] **Step 3: Update the sequence diagram note**

Its final note says totals are computed on read. Change it to say totals come from the captured line prices.

- [ ] **Step 4: Commit**

```bash
git add .docs/use-cases/cart-management/add-cart-item/add-cart-item.md
git commit -m "docs: correct add-cart-item totals to match the price snapshot"
```

- [ ] **Step 5: Push and open the pull request**

```bash
git push -u origin feat/GH-21-api-add-cart-item
gh pr create --base main --title "feat: add to cart API (GH-21)" --body "Implements #21. Closes #21."
```

---

## Out of Scope

- Item options and options-based uniqueness.
- Real opening hours — `is_open` is a flag.
- Authentication; `customerId` stays in the request body.
- **Stock decrement.** Stock is checked but never reduced; reserving stock belongs to checkout (#7). Two customers can both pass the check for the last item.
- Cart status / lifecycle. `carts.customer_id` stays unique, which checkout (#7) must resolve.
- The other five `CartService` methods (#3, #4, #5, #6, #7).
- The Surefire/Failsafe split (#26). This plan's end-to-end test runs under Surefire, like the health test.
