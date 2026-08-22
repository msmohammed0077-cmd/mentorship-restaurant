# Modify Cart Pseudocode

## Goal

This flow covers:

- viewing the cart before modification
- failing fast if the cart does not exist
- adding a new item through the add-cart-item flow
- updating an existing item quantity with stock validation
- updating item notes
- returning the refreshed cart at the end

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
