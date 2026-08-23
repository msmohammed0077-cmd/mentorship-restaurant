# Remove Items from Cart - Technical Documentation

## Overview
This document describes the functionality for removing items from a shopping cart in the Good Delivery application. The operation allows users to remove individual items or multiple items from their cart.

## Pseudo Code

```
Function removeCartItems(cartId, cartItemIds) {

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
return cart.empty;
}

cart.recalculateTotals()
updatedCart = saveCart(cart);

return updated cart;

}
```

## Sequence Diagram
![mermaid-diagram-1787311601791.png](Sequence-Diagram.png)

## Flow Diagram
![Flow-Diagram.png](Flow-Diagram.png)