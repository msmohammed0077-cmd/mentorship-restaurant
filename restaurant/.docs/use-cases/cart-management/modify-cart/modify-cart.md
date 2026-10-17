# Modify Cart Pseudocode

**Access:** a bearer token with `cart.write` (#83 part 4).

## Goal

This flow covers:

- viewing the cart before modification
- failing fast if the cart does not exist
- adding a new item through the add-cart-item flow
- updating an existing item quantity with stock validation
- updating item notes
- returning the refreshed cart at the end

## Authorisation

`PUT /api/v1/cart/{cartId}/items/{cartItemId}`. A token and `cart.write` are required (#83 part 4). The customer is the caller, taken from the token
(#83 part 2, ADR 0009). The endpoint keeps `{cartId}`, which names a resource rather than the caller,
and every lookup is scoped to `cart.id` **and** the caller's customer: someone else's cart is 404
"Cart not found", the same as a missing one. A caller who is not a customer gets 403 "Caller is not a
customer". A soft-deleted customer is 404 "Customer not found" before any cart lookup: their token outlives the delete, and the soft delete keeps the cart (#104). Before part 2 these endpoints checked no owner at all. The cart item
lookup carries all three: item, cart and the caller's customer.

## Main Flow

```text
PROCEDURE ModifyCart(cartId, action)

    cart ← FindCartById(cartId)

    IF cart DOES NOT EXIST THEN
        RAISE CartNotFoundException("Cart not found")
    END IF

    viewCart(cart)

    SWITCH action.type

        CASE "ADD_ITEM":
            result ← AddCartItem(action.customerId, action.menuItemId, action.quantity)

        CASE "UPDATE_QUANTITY":
            cartItem ← FindCartItemById(action.cartItemId)

            IF cartItem DOES NOT EXIST THEN
                RAISE CartItemNotFoundException("Cart item not found")
            END IF

            IF action.quantity <= 0 THEN
                RAISE InvalidQuantityException("Quantity must be greater than zero")
            END IF

            // As add-to-cart answers
            IF the item, its menu or its restaurant is deleted THEN
                RAISE MenuItemNotFoundException("Item not found")            (404)
            END IF
            IF the item's restaurant is closed THEN
                RAISE RestaurantClosedException("Restaurant is closed")      (409)
            END IF
            IF the item is unavailable THEN
                RAISE MenuItemUnavailableException("Item is unavailable")    (409)
            END IF

            stockAvailable ← CheckStock(cartItem.menuItemId, action.quantity)

            IF stockAvailable = false THEN
                RAISE OutOfStockException("Requested quantity is not available")
            END IF

            cartItem.quantity ← action.quantity
            SaveCartItem(cartItem)
            result ← cart

        CASE "ADD_NOTES":
            cartItem ← FindCartItemById(action.cartItemId)

            IF cartItem DOES NOT EXIST THEN
                RAISE CartItemNotFoundException("Cart item not found")
            END IF

            cartItem.notes ← NormalizeNotes(action.notes)
            SaveCartItem(cartItem)
            result ← cart

        DEFAULT:
            RAISE UnsupportedActionException("Unsupported cart modification")

    END SWITCH

    updatedCart ← ReloadCart(cart.id)

    viewUpdatedCart(updatedCart)

    RETURN updatedCart

END PROCEDURE
```

## Add Cart Item Flow

```text
PROCEDURE AddCartItem(customerId, menuItemId, quantity)

    IF quantity <= 0 THEN
        RAISE InvalidQuantityException("Quantity must be greater than zero")
    END IF

    menuItem ← FindMenuItemById(menuItemId)

    IF menuItem DOES NOT EXIST THEN
        RAISE MenuItemNotFoundException("Menu item not found")
    END IF

    stockAvailable ← CheckStock(menuItemId, quantity)

    IF stockAvailable = false THEN
        RAISE OutOfStockException("Requested quantity is not available")
    END IF

    cart ← FindActiveCartByCustomerId(customerId)

    IF cart DOES NOT EXIST THEN
        cart ← CreateCart(customerId)
    END IF

    cartItem ← BuildCartItem(menuItemId, quantity)
    SaveCartItem(cartItem)
    SaveCart(cart)

    RETURN cart

END PROCEDURE
```

## Validation Rules

```text
PROCEDURE viewCart(cart)

    DISPLAY cart

END PROCEDURE
```

```text
PROCEDURE viewUpdatedCart(cart)

    DISPLAY cart

END PROCEDURE
```

```text
PROCEDURE CheckStock(menuItemId, quantity)

    availableStock ← GetAvailableStock(menuItemId)

    IF availableStock < quantity THEN
        RETURN false
    END IF

    RETURN true

END PROCEDURE
```
