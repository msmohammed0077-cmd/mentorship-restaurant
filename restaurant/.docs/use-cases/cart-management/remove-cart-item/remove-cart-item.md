# Remove Items from Cart - Technical Documentation

## Overview
This document describes the functionality for removing items from a shopping cart in the Good Delivery application. The operation allows users to remove individual items or multiple items from their cart.

## Pseudo Code

```text
Transactional method
Cart Function removeCartItems(cartId, cartItemIds) {

validateInputs;

//Retrieve the customer's cart
cart = getCartById.orThrow(CART_NOT_FOUND_EXCEPTION);

cartItems = cart.getCartItems;

for (cartItemId : cartItemIds) {
cartItem = cartItems.findCartItemById(cartItemId)
.orThrow(CART_ITEM_NOT_FOUND_EXCEPTION);

cartItems.remove(cartItem);
}

if cartItems.size == 0 {
deleteCart(cart);
return null;
}

cart.recalculateTotals()
updatedCart = saveCart(cart);

return updated cart;

}
```

## Sequence Diagram
![Sequence diagram](images/sequence-diagram.png)

## Flow Diagram
![Flow diagram](images/flow-diagram.png)