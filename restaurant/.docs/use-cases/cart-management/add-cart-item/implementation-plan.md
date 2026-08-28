# Add To Cart API (GH-21) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `POST /api/v1/cart/items` so a customer can add a menu item to their cart, per `add-cart-item.md` in this directory.

**Architecture:** Follows `ModifyCartItemHandler`, the pattern already merged on `main`. `AddToCartHandler` is an `@Service` holding the use-case logic; `CartService` is a thin façade delegating to it; all lookups go through repositories; entities stay anemic Lombok data holders; guards are private methods on the handler; `CartMapper` builds the response.

**Tech Stack:** Java 17, Spring Boot 4.1.0, Spring Data JPA, Lombok, Flyway, PostgreSQL 16 (via `compose.yaml`), H2 in tests, JUnit 5 + Mockito + RestTestClient, Spotless.

> **Superseded in review:** flow 1a changed. Adding an item the cart already holds is now
> rejected with 409 rather than incrementing the line, so quantity changes belong entirely to
> modify-cart. The stock check is against the requested quantity alone, and `validateQuantity`
> was dropped from the handler in favour of the request's validation annotations. `add-cart-item.md`
> is the current specification; this plan records how the work was built.

**Baseline:** `main` as of the Spotless commit. Migrations run to `V5`, so this plan's migration is **`V6`**.

---

## Conventions this plan follows

Taken from `ModifyCartItemHandler` and the code around it, not invented here:

- **Logic lives in a per-use-case `@Service` handler**, annotated `@Transactional`, with guards as private methods (`validateQuantity`, `ensureStockAvailable`).
- **Entities are anemic**: `@Getter @Setter @NoArgsConstructor(access = PROTECTED)`, no behaviour, no factories.
- **Every lookup is a repository method.** Nothing queries or filters inside an entity.
- **`CartService` only delegates.** One method per use-case, no logic.
- **Exceptions carry their own message** and are mapped in `com.mentorship.restaurant.exception.GlobalExceptionHandler`.
- **Requests are Lombok `@Data` classes** with Jakarta validation annotations; responses likewise.
- **Controller returns `ResponseEntity<CartResponse>`** under `/api/v1/cart`.
- **Run `./mvnw spotless:apply` before every commit.**

## Decisions

**1. The cart's restaurant is derived, not stored.** Per the team decision there is no `carts.restaurant_id`. The cart's restaurant is whatever its lines point at, obtained through a repository query (`findRestaurantIdsByCartId`). An empty cart returns no rows and therefore accepts any restaurant — exactly the doc's "cart is empty OR same restaurant" rule.

**2. The price snapshot already exists and is already inferred.** `cart_items.cart_item_price` was created by `V1` (NOT NULL), `CartItem.itemPrice` maps it, and `ModifyCartItemHandler` already sets it from `menuItem.getItemPrice()`. This plan adds no column and does the same on insert. Totals come from the snapshot — `CartMapper` already computes them that way.

**3. `menu_item_name` is added.** `MenuItem` has `menuItemCode`, `note`, `stock` and `itemPrice`, but no name — which is why `CartItemMapper` currently puts `getMenuItemCode()` into the response's `itemName`. V6 adds the column; Task 2 fixes the mapper.

**4. Stock already exists** as `menu_items.menu_item_stock`, mapped as `MenuItem.stock`. No new column.

**5. Options remain out of scope.** `uq_cart_items_cart_menu_item` already enforces one line per item.

**6. Concurrency is out of scope.** Two simultaneous adds by the same customer can both find no cart, and the second violates `carts.customer_id`'s unique constraint as a 500. The constraints prevent corruption. Recorded on PR #27.

## Global Constraints

- Java 17, Spring Boot 4.1.0. Package root `com.mentorship.restaurant.cart`.
- `ddl-auto=validate`; Flyway owns the schema.
- Rejection messages verbatim from the spec: `"Restaurant is closed"`, `"Item is not in stock"`, `"Item is of different restaurant"`.
- Stock is checked against the **resulting** quantity (existing line + requested).
- Auth out of scope: `customerId` arrives in the request body.
- Every task ends with `./mvnw spotless:apply` and a commit. Branch: `feat/GH-21-api-add-cart-item`.

## Schema as it stands

```text
users(user_id, user_name, user_email UQ, user_password)
customers(customer_id, user_id UQ -> users)
restaurants(restaurant_id, user_id UQ -> users, restaurant_name, restaurant_description)
menus(menu_id, restaurant_id -> restaurants, menu_code UQ, menu_category)
menu_items(menu_item_id, menu_id -> menus, menu_item_code UQ, menu_item_image_url,
           menu_item_price NUMERIC(12,2) CHECK >= 0,
           menu_item_stock INTEGER NOT NULL, menu_item_note VARCHAR(255))
carts(cart_id, customer_id UQ -> customers)
cart_items(cart_item_id, cart_id -> carts, menu_item_id -> menu_items,
           cart_item_quantity INTEGER CHECK > 0,
           cart_item_price NUMERIC(12,2) CHECK >= 0,
           cart_item_note VARCHAR(255),
           UNIQUE (cart_id, menu_item_id))
```

`V2` seeds users 1–4, customers 1–2, restaurants 1–2, menus 1–2, menu items 1–6. `V3` seeds carts 1–2 with three lines.

**Both seeded customers already own a cart**, so nothing exercises "create a cart when the customer has none". V6 seeds a third, cart-less customer as this use-case's fixture.

**Seed strategy note:** global seeds shared by every test are brittle — one test's cleanup breaks another's fixture. The agreed direction is for each test to seed only what it needs. That refactor is not in this plan; V6's third customer is a stopgap that keeps this use-case from mutating existing seeds.

## The identity sequence bug

`V2` and `V3` insert explicit primary keys into `GENERATED BY DEFAULT AS IDENTITY` columns, which does not advance the sequences. The first row the application inserts therefore reuses id 1. Verified against a freshly migrated database:

```text
INSERT INTO carts (customer_id) VALUES (2) RETURNING cart_id;
ERROR:  duplicate key value violates unique constraint "carts_pkey"
DETAIL:  Key (cart_id)=(1) already exists.
```

This blocks the first cart the API creates. V6 resynchronises every seeded table's sequence.

## What exists and what is missing

| Piece | State |
| --- | --- |
| `CartItem.itemPrice`, `.note`, `.quantity` | Exists |
| `MenuItem.stock`, `.itemPrice`, `.menuItemCode` | Exists |
| `Cart.items` (`@OneToMany`) | Exists |
| `CartMapper`, `CartItemMapper`, `CartResponse`, `CartItemResponse` | Exist |
| `InvalidQuantityException`, `OutOfStockException`, `CartItemNotFoundException` | Exist |
| `GlobalExceptionHandler` | Exists |
| `CartItemRepository` | Exists (one method) |
| `MenuItem.name`, `Restaurant.open` | **Missing** |
| `CartRepository`, `CustomerRepository`, `MenuItemRepository` | **Missing — deleted from main** |
| `RestaurantClosedException`, `DifferentRestaurantException`, `CustomerNotFoundException`, `MenuItemNotFoundException` | **Missing** |
| `AddToCartHandler` | **Empty class** |
| Any test beyond `RestaurantApplicationTests` | **Missing** — `ModifyCartItemHandler` has none |

---

### Task 1: Migration V6

**Files:**
- Create: `src/main/resources/db/migration/V6__add_cart_item_fields.sql`

**Interfaces:**
- Produces: `restaurants.is_open BOOLEAN NOT NULL`, `menu_items.menu_item_name VARCHAR(150) NOT NULL`, customer 3 with no cart, resynchronised identity sequences.

- [ ] **Step 1: Write the migration**

```sql
-- Step 2 of the use-case: restaurant open/closed. A flag, not opening hours.
ALTER TABLE restaurants
    ADD COLUMN is_open BOOLEAN NOT NULL DEFAULT TRUE;

-- menu_items has menu_item_code and menu_item_note, but no readable name.
-- CartItemMapper currently puts the code into the response's itemName.
ALTER TABLE menu_items
    ADD COLUMN menu_item_name VARCHAR(150);

UPDATE menu_items SET menu_item_name = 'Kofta Platter'    WHERE menu_item_code = 'NK-KOFTA-01';
UPDATE menu_items SET menu_item_name = 'Falafel Sandwich' WHERE menu_item_code = 'NK-FALAFEL-01';
UPDATE menu_items SET menu_item_name = 'Koshari Bowl'     WHERE menu_item_code = 'NK-KOSHARI-01';
UPDATE menu_items SET menu_item_name = 'Classic Burger'   WHERE menu_item_code = 'BY-CLASSIC-01';
UPDATE menu_items SET menu_item_name = 'Chicken Burger'   WHERE menu_item_code = 'BY-CHICKEN-01';
UPDATE menu_items SET menu_item_name = 'Fries'            WHERE menu_item_code = 'BY-FRIES-01';

-- No such rows exist today; this only makes the NOT NULL below safe.
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
-- which does not advance the identity. Without this the first row the
-- application inserts reuses id 1 and violates the primary key.
-- ALTER ... RESTART WITH is understood by both PostgreSQL and H2, unlike
-- setval(pg_get_serial_sequence(...)). The literals are one past the highest
-- seeded id in each table.
ALTER TABLE users       ALTER COLUMN user_id      RESTART WITH 6;
ALTER TABLE customers   ALTER COLUMN customer_id  RESTART WITH 4;
ALTER TABLE restaurants ALTER COLUMN restaurant_id RESTART WITH 3;
ALTER TABLE menus       ALTER COLUMN menu_id      RESTART WITH 3;
ALTER TABLE menu_items  ALTER COLUMN menu_item_id RESTART WITH 7;
ALTER TABLE carts       ALTER COLUMN cart_id      RESTART WITH 3;
ALTER TABLE cart_items  ALTER COLUMN cart_item_id RESTART WITH 4;
```

No stock column — `V4` already provides `menu_item_stock`. No price column — `V1` already provides `cart_item_price`. No `carts.restaurant_id` — the restaurant is derived.

- [ ] **Step 2: Apply it and confirm the sequence fix**

```bash
docker compose down -v
docker compose up -d --wait postgres
./mvnw -B test -Dtest=RestaurantApplicationTests \
  -Dspring.datasource.url=jdbc:postgresql://localhost:55433/restaurant \
  -Dspring.datasource.driver-class-name=org.postgresql.Driver \
  -Dspring.datasource.username=postgres -Dspring.datasource.password=postgres
```

Expected: PASS, log shows `Successfully applied 6 migrations`. Then:

```bash
docker exec restaurant-postgres psql -U postgres -d restaurant \
  -c "INSERT INTO carts (customer_id) VALUES (3) RETURNING cart_id;"
docker exec restaurant-postgres psql -U postgres -d restaurant \
  -c "DELETE FROM carts WHERE customer_id = 3;"
```

Expected: returns `cart_id` 3, not a duplicate-key error.

Both statements were verified against PostgreSQL 16 and H2 2.4 in PostgreSQL mode: after `RESTART WITH 3`, an insert with no explicit id returns `cart_id` 3 on both.

- [ ] **Step 3: Format and commit**

```bash
./mvnw -B spotless:apply
git add src/main/resources/db/migration/V6__add_cart_item_fields.sql
git commit -m "feat(db): add open flag, item name, a cart-less customer and sequence resync"
```

---

### Task 2: Entity fields and the mapper fix

Fields only — no behaviour. `Restaurant` and `MenuItem` gain the two columns V6 added, and `CartItemMapper` starts reporting the real name.

**Files:**
- Modify: `model/entity/Restaurant.java`, `model/entity/MenuItem.java`, `model/mapper/CartItemMapper.java`

**Interfaces:**
- Produces: `Restaurant.isOpen(): boolean` (Lombok, from `private boolean open`), `MenuItem.getName(): String`.

- [ ] **Step 1: Add the fields**

`Restaurant.java`, after `restaurantDescription`:

```java
  @Column(name = "is_open", nullable = false)
  private boolean open;
```

Lombok's `@Getter` generates `isOpen()` for a primitive `boolean`.

`MenuItem.java`, after `menuItemCode`:

```java
  @Column(name = "menu_item_name", nullable = false, length = 150)
  private String name;
```

- [ ] **Step 1b: Widen the constructor on the two entities the app creates**

`AddToCartHandler` calls `new Cart()` and `new CartItem()` from another package, where a `PROTECTED` constructor is not visible. On `Cart.java` and `CartItem.java` only, change:

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

to:

```java
@NoArgsConstructor
```

Leave `Restaurant`, `Menu`, `MenuItem`, `Customer` and `User` as they are — nothing constructs them.

- [ ] **Step 2: Report the real name in the response**

`CartItemMapper.toResponse` currently passes `cartItem.getMenuItem().getMenuItemCode()` into the response's `itemName`, which was a stand-in. Change that one argument:

```java
        cartItem.getMenuItem().getName(),
```

- [ ] **Step 3: Verify the entities still match the schema**

```bash
./mvnw -B test -Dtest=RestaurantApplicationTests
```

Expected: PASS. Fails with `missing column` if either mapping is misspelled.

- [ ] **Step 4: Format and commit**

```bash
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart/model
git commit -m "feat(cart): map the restaurant open flag and the menu item name"
```

---

### Task 3: Repositories

Every lookup this use-case needs, as a repository method. `CartRepository`, `CustomerRepository` and `MenuItemRepository` were deleted from `main` and come back here.

**Files:**
- Create: `repository/CartRepository.java`, `repository/CustomerRepository.java`, `repository/MenuItemRepository.java`
- Modify: `repository/CartItemRepository.java`

**Interfaces:**
- Produces:
  - `CartRepository.findByCustomer_Id(Long): Optional<Cart>`
  - `CartRepository.findRestaurantIdsByCartId(Long): List<Long>`
  - `CartItemRepository.findByCart_IdAndMenuItem_Id(Long, Long): Optional<CartItem>`
  - `CustomerRepository`, `MenuItemRepository` — plain `JpaRepository`

- [ ] **Step 1: Create the three repositories**

`CartRepository.java`:

```java
package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.Cart;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartRepository extends JpaRepository<Cart, Long> {

  Optional<Cart> findByCustomer_Id(Long customerId);

  /**
   * The cart's restaurant is not stored, so it is read from the cart's lines. An empty cart returns
   * no rows, which is what lets it accept an item from any restaurant.
   */
  @Query(
      """
      select distinct m.restaurant.id
      from CartItem ci
      join ci.menuItem mi
      join mi.menu m
      where ci.cart.id = :cartId
      """)
  List<Long> findRestaurantIdsByCartId(Long cartId);
}
```

`CustomerRepository.java`:

```java
package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {}
```

`MenuItemRepository.java`:

```java
package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {}
```

- [ ] **Step 2: Add the line lookup**

In `CartItemRepository`, beside the existing `findByIdAndCart_Id`:

```java
  Optional<CartItem> findByCart_IdAndMenuItem_Id(Long cartId, Long menuItemId);
```

This is what decides insert versus increment — a query, not a scan of the cart's collection.

- [ ] **Step 3: Verify it compiles, format and commit**

```bash
./mvnw -B -q compile
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart/repository
git commit -m "feat(cart): add the lookups add-to-cart needs"
```

---

### Task 4: Exceptions

Four new ones, wired into the existing `GlobalExceptionHandler`.

**Files:**
- Create: `exception/RestaurantClosedException.java`, `DifferentRestaurantException.java`, `CustomerNotFoundException.java`, `MenuItemNotFoundException.java`
- Modify: `com/mentorship/restaurant/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: four `RuntimeException` subclasses with a `(String message)` constructor, each mapped to a status.

- [ ] **Step 1: Create the four exceptions**

Each follows the shape of `OutOfStockException`. `RestaurantClosedException.java`:

```java
package com.mentorship.restaurant.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RestaurantClosedException extends RuntimeException {
  public RestaurantClosedException(String message) {
    super(message);
  }
}
```

`DifferentRestaurantException.java` — identical but for the name:

```java
package com.mentorship.restaurant.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DifferentRestaurantException extends RuntimeException {
  public DifferentRestaurantException(String message) {
    super(message);
  }
}
```

`CustomerNotFoundException.java`:

```java
package com.mentorship.restaurant.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends RuntimeException {
  public CustomerNotFoundException(String message) {
    super(message);
  }
}
```

`MenuItemNotFoundException.java`:

```java
package com.mentorship.restaurant.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MenuItemNotFoundException extends RuntimeException {
  public MenuItemNotFoundException(String message) {
    super(message);
  }
}
```

- [ ] **Step 2: Map them in GlobalExceptionHandler**

`@ResponseStatus` on the exception is **ignored** once an `@ExceptionHandler` matches, and the handler's `@ExceptionHandler(Exception.class)` catches everything unlisted — so an unlisted exception returns 500. Add the four to the cart handler and extend the status decision:

```java
  @ExceptionHandler({
    CartItemNotFoundException.class,
    CustomerNotFoundException.class,
    MenuItemNotFoundException.class,
    InvalidQuantityException.class,
    OutOfStockException.class,
    RestaurantClosedException.class,
    DifferentRestaurantException.class
  })
  public ResponseEntity<ApiErrorResponse> handleCartExceptions(
      RuntimeException exception, HttpServletRequest request) {
    HttpStatus status = statusOf(exception);
    return buildResponse(status, exception.getMessage(), request.getRequestURI());
  }

  private HttpStatus statusOf(RuntimeException exception) {
    if (exception instanceof CartItemNotFoundException
        || exception instanceof CustomerNotFoundException
        || exception instanceof MenuItemNotFoundException) {
      return HttpStatus.NOT_FOUND;
    }
    if (exception instanceof OutOfStockException
        || exception instanceof RestaurantClosedException
        || exception instanceof DifferentRestaurantException) {
      return HttpStatus.CONFLICT;
    }
    return HttpStatus.BAD_REQUEST;
  }
```

The nested ternary this replaces does not extend to seven types legibly.

- [ ] **Step 3: Verify it compiles, format and commit**

```bash
./mvnw -B -q compile
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant
git commit -m "feat(cart): add add-to-cart rejections and map them"
```

---

### Task 5: AddToCartHandler

The use-case logic, mirroring `ModifyCartItemHandler`.

**Files:**
- Modify: `service/handler/AddToCartHandler.java`
- Test: `src/test/java/com/mentorship/restaurant/cart/service/handler/AddToCartHandlerTest.java`

**Interfaces:**
- Consumes: Tasks 2–4.
- Produces: `AddToCartHandler.addItem(Long customerId, Long menuItemId, Integer quantity, String note): CartResponse`

- [ ] **Step 1: Write the failing test**

```java
package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.exception.RestaurantClosedException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.entity.Customer;
import com.mentorship.restaurant.cart.model.entity.Menu;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.cart.model.entity.Restaurant;
import com.mentorship.restaurant.cart.model.mapper.CartItemMapper;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.cart.repository.CustomerRepository;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddToCartHandlerTest {

  private static final Long CUSTOMER_ID = 3L;
  private static final Long MENU_ITEM_ID = 1L;
  private static final Long CART_ID = 9L;

  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private MenuItemRepository menuItemRepository;

  private AddToCartHandler handler;

  private Customer customer;
  private MenuItem kofta;
  private Cart cart;

  @BeforeEach
  void setUp() {
    handler =
        new AddToCartHandler(
            cartRepository,
            cartItemRepository,
            customerRepository,
            menuItemRepository,
            new CartMapper(new CartItemMapper()));

    customer = new Customer();
    customer.setId(CUSTOMER_ID);

    kofta = menuItem(MENU_ITEM_ID, restaurant(1L, true), "Kofta Platter", "185.00", 5);

    cart = new Cart();
    cart.setId(CART_ID);
    cart.setCustomer(customer);

    lenient().when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
    lenient().when(menuItemRepository.findById(MENU_ITEM_ID)).thenReturn(Optional.of(kofta));
    lenient()
        .when(cartItemRepository.findByCart_IdAndMenuItem_Id(CART_ID, MENU_ITEM_ID))
        .thenReturn(Optional.empty());
    lenient().when(cartRepository.findRestaurantIdsByCartId(CART_ID)).thenReturn(List.of());
    lenient().when(cartItemRepository.save(any(CartItem.class))).thenAnswer(c -> c.getArgument(0));
  }

  @Test
  void rejectsAQuantityOfZeroOrLess() {
    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 0, null))
        .isInstanceOf(InvalidQuantityException.class);
  }

  @Test
  void rejectsAnUnknownCustomer() {
    when(customerRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.addItem(99L, MENU_ITEM_ID, 1, null))
        .isInstanceOf(CustomerNotFoundException.class);
  }

  @Test
  void rejectsAnUnknownMenuItem() {
    when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, 99L, 1, null))
        .isInstanceOf(MenuItemNotFoundException.class);
  }

  @Test
  void rejectsAClosedRestaurant() {
    MenuItem fries = menuItem(MENU_ITEM_ID, restaurant(2L, false), "Fries", "35.00", 5);
    when(menuItemRepository.findById(MENU_ITEM_ID)).thenReturn(Optional.of(fries));

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1, null))
        .isInstanceOf(RestaurantClosedException.class)
        .hasMessage("Restaurant is closed");
  }

  @Test
  void rejectsAQuantityBeyondStock() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 6, null))
        .isInstanceOf(OutOfStockException.class)
        .hasMessage("Item is not in stock");
  }

  @Test
  void rejectsAnIncrementThatWouldExceedStock() {
    CartItem existing = line(cart, kofta, 3);
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCart_IdAndMenuItem_Id(CART_ID, MENU_ITEM_ID))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 3, null))
        .isInstanceOf(OutOfStockException.class)
        .hasMessage("Item is not in stock");
  }

  @Test
  void rejectsAnItemFromADifferentRestaurant() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));
    when(cartRepository.findRestaurantIdsByCartId(CART_ID)).thenReturn(List.of(2L));

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1, null))
        .isInstanceOf(DifferentRestaurantException.class)
        .hasMessage("Item is of different restaurant");
  }

  @Test
  void acceptsAnyRestaurantIntoAnEmptyCart() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));
    when(cartRepository.findRestaurantIdsByCartId(CART_ID)).thenReturn(List.of());

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1, null);

    verify(cartItemRepository).save(any(CartItem.class));
  }

  @Test
  void createsACartWhenTheCustomerHasNone() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.empty());
    when(cartRepository.save(any(Cart.class)))
        .thenAnswer(
            call -> {
              Cart saved = call.getArgument(0);
              saved.setId(CART_ID);
              return saved;
            });

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2, null);

    verify(cartRepository).save(any(Cart.class));
  }

  @Test
  void reusesTheExistingCart() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2, null);

    verify(cartRepository, never()).save(any(Cart.class));
  }

  @Test
  void capturesTheMenuPriceOnTheNewLine() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2, "extra spicy");

    assertThat(cart.getItems()).hasSize(1);
    assertThat(cart.getItems().get(0).getItemPrice()).isEqualByComparingTo("185.00");
    assertThat(cart.getItems().get(0).getNote()).isEqualTo("extra spicy");
  }

  @Test
  void incrementsTheExistingLineAndKeepsItsPrice() {
    CartItem existing = line(cart, kofta, 1);
    existing.setItemPrice(new BigDecimal("150.00"));
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCart_IdAndMenuItem_Id(CART_ID, MENU_ITEM_ID))
        .thenReturn(Optional.of(existing));

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2, null);

    assertThat(existing.getQuantity()).isEqualTo(3);
    assertThat(existing.getItemPrice()).isEqualByComparingTo("150.00");
    verify(cartItemRepository, never()).save(any(CartItem.class));
  }

  private Restaurant restaurant(Long id, boolean open) {
    Restaurant restaurant = new Restaurant();
    restaurant.setId(id);
    restaurant.setRestaurantName("Restaurant " + id);
    restaurant.setOpen(open);
    return restaurant;
  }

  private MenuItem menuItem(Long id, Restaurant restaurant, String name, String price, int stock) {
    Menu menu = new Menu();
    menu.setRestaurant(restaurant);
    MenuItem item = new MenuItem();
    item.setId(id);
    item.setMenu(menu);
    item.setName(name);
    item.setItemPrice(new BigDecimal(price));
    item.setStock(stock);
    return item;
  }

  private CartItem line(Cart cart, MenuItem menuItem, int quantity) {
    CartItem item = new CartItem();
    item.setCart(cart);
    item.setMenuItem(menuItem);
    item.setQuantity(quantity);
    item.setItemPrice(menuItem.getItemPrice());
    cart.getItems().add(item);
    return item;
  }
}
```

The entities are anemic with public setters, so the test builds them directly — no reflection helper needed. `rejectsAnIncrementThatWouldExceedStock` pins the resulting-quantity rule: stock 5, cart holds 3, adding 3 more fails even though 3 alone would pass.

Lombok's `@NoArgsConstructor(access = PROTECTED)` means `new Cart()` compiles only from the same package. These tests live in `...cart.service.handler`, so **add a package-private static factory to the test itself** if the constructor is inaccessible — or, simpler, keep the builders above in a `src/test/java/com/mentorship/restaurant/cart/model/entity/EntityFixtures.java` inside the entity package and call it from here. Confirm which is needed when the test first compiles.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./mvnw -B test -Dtest=AddToCartHandlerTest
```

Expected: compilation failure — `AddToCartHandler` has no constructor and no `addItem`.

- [ ] **Step 3: Implement the handler**

```java
package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.exception.RestaurantClosedException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.entity.Customer;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.cart.model.entity.Restaurant;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.cart.repository.CustomerRepository;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddToCartHandler {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final CustomerRepository customerRepository;
  private final MenuItemRepository menuItemRepository;
  private final CartMapper cartMapper;

  public AddToCartHandler(
      CartRepository cartRepository,
      CartItemRepository cartItemRepository,
      CustomerRepository customerRepository,
      MenuItemRepository menuItemRepository,
      CartMapper cartMapper) {
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.customerRepository = customerRepository;
    this.menuItemRepository = menuItemRepository;
    this.cartMapper = cartMapper;
  }

  @Transactional
  public CartResponse addItem(Long customerId, Long menuItemId, Integer quantity, String note) {
    validateQuantity(quantity);

    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    MenuItem menuItem =
        menuItemRepository
            .findById(menuItemId)
            .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));

    ensureRestaurantOpen(menuItem.getMenu().getRestaurant());

    Cart cart = cartRepository.findByCustomer_Id(customerId).orElse(null);

    Optional<CartItem> existingLine =
        cart == null
            ? Optional.empty()
            : cartItemRepository.findByCart_IdAndMenuItem_Id(cart.getId(), menuItem.getId());

    ensureStockAvailable(existingLine, quantity, menuItem);

    if (cart != null) {
      ensureSameRestaurant(cart, menuItem.getMenu().getRestaurant());
    } else {
      cart = createCartFor(customer);
    }

    if (existingLine.isPresent()) {
      CartItem line = existingLine.get();
      line.setQuantity(line.getQuantity() + quantity);
      if (note != null) {
        line.setNote(note);
      }
    } else {
      cart.getItems().add(newLine(cart, menuItem, quantity, note));
    }

    return cartMapper.toResponse(cart);
  }

  private void validateQuantity(Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new InvalidQuantityException("Quantity must be greater than zero");
    }
  }

  private void ensureRestaurantOpen(Restaurant restaurant) {
    if (!restaurant.isOpen()) {
      throw new RestaurantClosedException("Restaurant is closed");
    }
  }

  /**
   * Checked against the resulting quantity, so repeated adds cannot walk past available stock one
   * call at a time.
   */
  private void ensureStockAvailable(
      Optional<CartItem> existingLine, Integer requested, MenuItem menuItem) {
    int alreadyInCart = existingLine.map(CartItem::getQuantity).orElse(0);
    Integer available = menuItem.getStock();
    if (available == null || alreadyInCart + requested > available) {
      throw new OutOfStockException("Item is not in stock");
    }
  }

  /** An empty cart has no restaurant of its own, so it accepts an item from any of them. */
  private void ensureSameRestaurant(Cart cart, Restaurant restaurant) {
    List<Long> restaurantIds = cartRepository.findRestaurantIdsByCartId(cart.getId());
    if (!restaurantIds.isEmpty() && !restaurantIds.contains(restaurant.getId())) {
      throw new DifferentRestaurantException("Item is of different restaurant");
    }
  }

  private Cart createCartFor(Customer customer) {
    Cart cart = new Cart();
    cart.setCustomer(customer);
    return cartRepository.save(cart);
  }

  private CartItem newLine(Cart cart, MenuItem menuItem, Integer quantity, String note) {
    CartItem line = new CartItem();
    line.setCart(cart);
    line.setMenuItem(menuItem);
    line.setQuantity(quantity);
    line.setNote(note);
    // The price is captured from the menu item, never stored separately, so a
    // later menu price change does not reprice what is already in the cart.
    line.setItemPrice(menuItem.getItemPrice());
    return cartItemRepository.save(line);
  }
}
```

**Entity constructor visibility:** `new Cart()` and `new CartItem()` are called from `cart.service.handler`, a different package from `cart.model.entity`, so Lombok's `PROTECTED` no-arg constructor is not visible. Task 2 therefore widens the annotation on **`Cart` and `CartItem` only** — the two entities the application creates — from `@NoArgsConstructor(access = AccessLevel.PROTECTED)` to `@NoArgsConstructor`. The others keep `PROTECTED`, since nothing constructs them. A static factory was the alternative, but the conventions say entities carry no behaviour, and a factory is behaviour.

- [ ] **Step 4: Run the test to verify it passes**

```bash
./mvnw -B test -Dtest=AddToCartHandlerTest
```

Expected: PASS, 12 tests.

- [ ] **Step 5: Format and commit**

```bash
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart/service/handler src/test/java/com/mentorship/restaurant/cart
git commit -m "feat(cart): implement the add-to-cart handler"
```

---

### Task 6: Service, request and endpoint

**Files:**
- Modify: `service/CartService.java`, `controller/CartController.java`
- Create: `model/request/AddCartItemRequest.java`

**Interfaces:**
- Consumes: Task 5.
- Produces: `POST /api/v1/cart/items` returning `201 Created` with a `CartResponse`.

- [ ] **Step 1: Create the request**

Follows `UpdateCartItemRequest`'s shape.

```java
package com.mentorship.restaurant.cart.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequest {

  @NotNull private Long customerId;

  @NotNull private Long menuItemId;

  // Max keeps the handler's int arithmetic safe: without it a quantity of
  // Integer.MAX_VALUE would wrap negative when added to the existing line's
  // quantity and slip past the stock check.
  @NotNull @Positive @Max(1000) private Integer quantity;

  private String note;
}
```

- [ ] **Step 2: Delegate from the service**

`CartService` gains one method and one dependency; it holds no logic.

```java
  private final AddToCartHandler addToCartHandler;
```

Add it to the constructor alongside `modifyCartItemHandler`, then:

```java
  public CartResponse addItem(Long customerId, Long menuItemId, Integer quantity, String note) {
    return addToCartHandler.addItem(customerId, menuItemId, quantity, note);
  }
```

- [ ] **Step 3: Add the endpoint**

In `CartController`, beside `modifyItem`:

```java
  @PostMapping("/items")
  public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
    CartResponse response =
        cartService.addItem(
            request.getCustomerId(),
            request.getMenuItemId(),
            request.getQuantity(),
            request.getNote());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
```

The cart is resolved from `customerId` rather than a path variable, because this endpoint creates the cart when none exists and the caller has no `cartId` before the first add. `customerId` moves to the authenticated principal once auth lands.

- [ ] **Step 4: Verify the suite still passes, format and commit**

```bash
./mvnw -B test
./mvnw -B spotless:apply
git add src/main/java/com/mentorship/restaurant/cart
git commit -m "feat(cart): expose POST /api/v1/cart/items"
```

---

### Task 7: End-to-end verification

**Files:**
- Modify: `pom.xml`
- Test: `src/test/java/com/mentorship/restaurant/cart/AddCartItemEndpointTest.java`

**Interfaces:**
- Consumes: Tasks 1–6.

Three constraints shape this test:

1. **`spring-boot-starter-webmvc-test` was removed from the pom** with the health test. It brings `spring-boot-resttestclient`, so `RestTestClient` is unavailable until restored.
2. **`@Transactional` rolls back nothing the server did** — requests run on server threads in their own transactions. The test uses **customer 3**, seeded cart-less by V6, and cleans up only that customer's cart. Carts 1 and 2 are never touched.
3. **Closed-restaurant and out-of-stock have no seeded fixture** — both restaurants are open and every item has stock 40+. `AddToCartHandlerTest` covers those.

- [ ] **Step 1: Restore the test dependency**

In `pom.xml`, after `spring-boot-starter-test`:

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
```

- [ ] **Step 2: Write the test**

```java
package com.mentorship.restaurant.cart;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AddCartItemEndpointTest {

  /** Seeded cart-less by V6, reserved for this test. Carts 1 and 2 are never touched. */
  private static final long CUSTOMER = 3L;

  private static final long KOFTA = 1L; // restaurant 1, 185.00
  private static final long FALAFEL = 2L; // restaurant 1, 65.00
  private static final long CLASSIC_BURGER = 4L; // restaurant 2, 120.00

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbcTemplate;

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
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(KOFTA, 2))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.customerId")
        .isEqualTo(3)
        .jsonPath("$.items.length()")
        .isEqualTo(1)
        .jsonPath("$.items[0].itemName")
        .isEqualTo("Kofta Platter")
        .jsonPath("$.items[0].quantity")
        .isEqualTo(2)
        .jsonPath("$.total")
        .isEqualTo(370.00);
  }

  @Test
  void incrementsInsteadOfAddingASecondLine() {
    addItem(KOFTA, 2);

    client
        .post()
        .uri("/api/v1/cart/items")
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

    client
        .post()
        .uri("/api/v1/cart/items")
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

    client
        .post()
        .uri("/api/v1/cart/items")
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
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(9999L, 1))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void rejectsAQuantityOfZero() {
    client
        .post()
        .uri("/api/v1/cart/items")
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
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(menuItemId, quantity))
        .exchange()
        .expectStatus()
        .isCreated();
  }
}
```

`$.message` matches `ApiErrorResponse`, which `GlobalExceptionHandler` returns.

- [ ] **Step 3: Run it, then the whole suite**

```bash
./mvnw -B test -Dtest=AddCartItemEndpointTest
./mvnw -B verify
```

Expected: 6 tests, then the full suite green — 12 (handler) + 6 (endpoint) + 1 (context) = 19.

- [ ] **Step 4: Format and commit**

```bash
./mvnw -B spotless:apply
git add pom.xml src/test/java/com/mentorship/restaurant/cart/AddCartItemEndpointTest.java
git commit -m "test(cart): cover add-to-cart end to end"
```

---

### Task 8: Correct the use-case doc

**Files:**
- Modify: `add-cart-item.md` in this directory

- [ ] **Step 1: Replace the Totals section**

```markdown
## Totals

`CartItem` captures the item's price when the line is created
(`cart_items.cart_item_price`, mapped as `CartItem.itemPrice`). Totals are
computed from those captured prices, never from the current menu price.

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
| `menu_items` | `menu_item_name` (varchar, not null) | The table held only `menu_item_code`; a cart line needs a readable name. |

Already present: `menu_items.menu_item_stock` for the stock check,
`cart_items.cart_item_price` for the price snapshot, and
`uq_cart_items_cart_menu_item`, which enforces Rule 1 in the database.

The cart's restaurant is **not stored**. It is read from the cart's lines
(`CartRepository.findRestaurantIdsByCartId`), and an empty cart has none — which
is why an empty cart accepts an item from any restaurant.
```

- [ ] **Step 3: Update the diagrams**

The flowchart and sequence diagram describe totals computed on read and a cart that stores its restaurant. Update the mapper note to say totals come from the captured line prices, and the cart-creation step to `Create cart for customer`, with the restaurant check reading the cart's existing lines.

- [ ] **Step 4: Commit and push**

```bash
git add add-cart-item.md
git commit -m "docs: correct add-cart-item totals and the derived restaurant"
git push
```

PR #27 already exists for this branch.

---

## Out of Scope

- Item options and options-based uniqueness.
- Real opening hours — `is_open` is a flag.
- Authentication; `customerId` stays in the request body.
- **Concurrency.** Two simultaneous adds by the same customer can both find no cart, and the second violates `carts.customer_id`'s unique constraint as a 500; concurrent increments can lose an update. The constraints prevent corruption. Noted on PR #27.
- **Stock decrement.** Stock is checked but never reduced; reserving it belongs to checkout (#7).
- Cart status / lifecycle. `carts.customer_id` stays unique, which checkout (#7) must resolve.
- **Per-test seed data.** The agreed direction is each test seeding what it needs; V6's cart-less customer is a stopgap, not that refactor.
- **Tests for `ModifyCartItemHandler`**, which currently has none.
- The other four `CartService` use-cases (#4, #5, #6, #7).
- The Surefire/Failsafe split (#26).
