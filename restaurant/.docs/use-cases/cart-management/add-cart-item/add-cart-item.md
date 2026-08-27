# Goal

Add an item to the customer's cart.

## Actor

Customer

## Preconditions

1. Customer is logged in / registered.
2. Restaurant exists.
3. Item exists.

## Business Rules

1. A `CartItem` is unique by **item**. Adding an item already in the cart increments the
   existing line rather than inserting a new one.
2. A cart holds items from a **single restaurant** at a time.

## Main Success Scenario

1. Customer adds an item to the cart (with quantity).
2. System verifies the restaurant is open.
3. System verifies the item has stock for the **resulting** quantity — the quantity already
   in the cart for that item plus the requested quantity.
4. System verifies the cart is empty OR the item is from the same restaurant as the cart.
5. If no cart exists, System creates one for the customer and the item's restaurant.
6. System inserts or increments the `CartItem`.

## Exception Flows

Each branch is labelled by the Main Flow step it extends.

- **1a. Item already in cart:** increment the `CartItem` quantity instead of inserting a new
  line; still enforce stock (step 3) against the resulting quantity.
- **2a. Restaurant is closed:** reject — "Restaurant is closed".
- **3a. Item is out of stock:** reject — "Item is not in stock".
- **4a. Item is from a different restaurant:** reject — "Item is of different restaurant";
  prompt the customer to clear the cart or continue with the current restaurant.

## Postconditions

1. `Cart` exists (created if it didn't), bound to exactly one restaurant.
2. `CartItem` is present in `Cart` (inserted or incremented).

## Totals

`CartItem` captures the item's price when the line is created
(`cart_items.cart_item_price`, mapped as `CartItem.itemPrice`). Totals are computed from
those captured prices, never from the current menu price.

A cart therefore shows what the customer agreed to pay. If a restaurant changes a price,
carts already holding that item are unaffected; the new price applies only to lines
created afterwards. Incrementing an existing line keeps its original price.

## Data Model

Added alongside this use-case (`V6`):

| Table | Column | Purpose |
| --- | --- | --- |
| `restaurants` | `is_open` (boolean, not null) | Step 2 — open/closed check. A flag, not opening hours. |
| `menu_items` | `menu_item_name` (varchar, not null) | The table held only `menu_item_code`; a cart line needs a readable name. |

Already present: `menu_items.menu_item_stock` for the stock check,
`cart_items.cart_item_price` for the price snapshot, and `uq_cart_items_cart_menu_item`,
which enforces Rule 1 in the database.

The cart's restaurant is **not stored**. It is read from the cart's lines
(`CartRepository.findRestaurantIdsByCartId`), and an empty cart has none — which is why an
empty cart accepts an item from any restaurant.

## Diagram

### Flowchart

```mermaid
flowchart TD
    Start([Customer adds item: itemId, quantity]) --> Open{Restaurant open?}
    Open -- No --> RejClosed[/Reject: Restaurant is closed/]
    Open -- Yes --> Line[Look up existing cart line for this item]
    Line --> Stock{"stock >= existing + requested?"}
    Stock -- No --> RejStock[/Reject: Item is not in stock/]
    Stock -- Yes --> HasCart{Cart exists?}
    HasCart -- No --> Create[Create cart for customer]
    HasCart -- Yes --> Same{"Cart empty, or same restaurant?"}
    Same -- No --> RejOther[/Reject: Item is of different restaurant/]
    Same -- Yes --> InCart{Item already in cart?}
    Create --> Insert[Insert CartItem]
    InCart -- No --> Insert
    InCart -- Yes --> Incr[Increment CartItem quantity]
    Insert --> Done([Cart updated])
    Incr --> Done
```

### Sequence Diagram

```mermaid
sequenceDiagram
    actor Customer
    participant C as CartController
    participant S as CartService
    participant MIR as MenuItemRepository
    participant CR as CartRepository
    participant CIR as CartItemRepository

    Customer->>C: POST /api/carts/items (itemId, quantity)
    C->>S: addItem(customerId, menuItemId, quantity)

    S->>MIR: findById(menuItemId)
    MIR-->>S: MenuItem (+ Menu -> Restaurant)

    alt restaurant is closed
        S-->>C: RestaurantClosedException
    end

    S->>CR: findByCustomerId(customerId)
    CR-->>S: Cart or empty

    opt cart exists
        S->>CIR: findByCartIdAndMenuItemId(cartId, menuItemId)
        CIR-->>S: CartItem or empty
    end

    alt stock < existing + requested
        S-->>C: OutOfStockException
    else cart exists and belongs to another restaurant
        S-->>C: DifferentRestaurantException
    end

    opt no cart
        S->>CR: save(new Cart(customer))
        CR-->>S: Cart
    end

    alt line exists
        S->>CIR: increment quantity
    else
        S->>CIR: save(new CartItem(cart, menuItem, quantity))
    end

    S-->>C: cart view (totals from the captured line prices)
    C-->>Customer: 200 OK
```

### Pseudocode

1. [Pseudocode](images/pseduocode.txt)

# Notes

1. Should we split this use-case into `increment-cart-item` / `decrement-cart-item`?
2. **Options are out of scope.** Uniqueness is by item alone. If item options are ever
   modelled, Rule 1 widens to (item + options) and flow 1a's match key widens with it.
3. **`is_open` is a flag, not a schedule.** Real opening hours (open/close times, timezone,
   per-day overrides) are a separate use-case.
4. **One cart per customer.** `carts.customer_id` is unique, so a customer has at most one
   cart row ever. That is fine here, but checkout must decide: either empty and reuse the
   row, or give `Cart` a status and drop the unique constraint. Deliberately left open.
