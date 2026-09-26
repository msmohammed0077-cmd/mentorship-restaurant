# Food Delivery System --- Cart Management

## Use Case: View Cart

### 1. Overview

This document describes the **View Cart** functionality within the Cart Management module of a Food Delivery System.

The feature covers:

-   Viewing the current customer's cart
-   Retrieving cart items
-   Validating current product information
-   Calculating cart totals
-   Handling an empty or unavailable cart
-   Returning the cart to the frontend for display

------------------------------------------------------------------------

## 2.1 Use Case Information

| Attribute | Description                                |
|---|--------------------------------------------|
| Use Case | View Cart                                  |
| Actor | Customer                                   |
| Module | Cart Management                            |
| Trigger | Customer clicks the Cart button               |
| Precondition | Customer can access their cart                 |
| Postcondition | Current cart contents and totals are displayed |

## 2.2 Main Flow

1.  Customer clicks **Cart**.
2.  Frontend sends `GET /api/v1/cart`.
3.  Cart Service identifies the customer's cart.
4.  Cart Service retrieves the cart and its items.
5.  System retrieves current product/restaurant information if required.
6.  System validates the cart items.
7.  System calculates:
    -   Item quantities
    -   Item subtotals
    -   Delivery fee
    -   Discounts/promotions
    -   Tax
    -   Grand total
8.  Cart Service returns the cart response.
9.  Frontend displays the cart to the customer.

## 2.3 Alternative Flows

### A1 --- Cart Does Not Exist

If the customer does not have an existing cart:

1.  Cart Service searches for the customer's cart.
2.  No cart is found.
3.  System returns an empty cart response.
4.  Frontend displays an empty-cart state.

### A2 --- Cart Is Empty

If the cart exists but contains no items:

1.  Cart Service retrieves the cart.
2.  No active items are found.
3.  System returns an empty cart.
4.  Frontend displays:

> Your cart is empty.

### A3 --- Product Is No Longer Available

If an item in the cart is no longer available:

1.  Cart Service retrieves the cart item.
2.  Product information is checked.
3.  Product is identified as unavailable.
4.  Item is marked as unavailable in the cart response.
5.  Frontend displays a warning to the customer.
6.  Checkout should not proceed with the unavailable item.

### A4 --- Product Price Has Changed

If the current product price differs from the price stored in the cart:

1.  Cart Service retrieves the current product information.
2.  Current price is compared with the cart price.
3.  Updated price is returned to the frontend.
4.  Cart totals are recalculated.
5.  Frontend informs the customer about the price change.

### A5 --- Restaurant Is Closed

If the restaurant associated with the cart is currently closed:

1.  Cart Service retrieves restaurant status.
2.  Restaurant is identified as closed.
3.  Cart is returned with a restaurant-closed warning.
4.  Checkout is disabled until the restaurant becomes available.

------------------------------------------------------------------------

# 3. Flow Chart

``` text
                ┌──────────────┐
                │   Customer   │
                └──────┬───────┘
                       │
                       ▼
                ┌──────────────┐
                │ Click "Cart" │
                └──────┬───────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Request Cart    │
              │ GET /cart       │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Find Customer   │
              │ Cart            │
              └────────┬────────┘
                       │
                       ▼
                ┌──────────────┐
                │ Cart exists? │
                └──────┬───────┘
                  No   │   Yes
               ┌──────┘     └──────┐
               ▼                   ▼
        ┌──────────────┐    ┌──────────────┐
        │ Return Empty │    │ Get Cart     │
        │ Cart         │    │ Items        │
        └──────┬───────┘    └──────┬───────┘
               │                    │
               │                    ▼
               │             ┌──────────────┐
               │             │ Validate     │
               │             │ Items/Prices │
               │             └──────┬───────┘
               │                    │
               │                    ▼
               │             ┌──────────────┐
               │             │ Calculate    │
               │             │ Totals       │
               │             └──────┬───────┘
               │                    │
               └──────────┬─────────┘
                          ▼
                  ┌──────────────┐
                  │ Return Cart  │
                  │ Response     │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ Display Cart │
                  └──────────────┘
```

------------------------------------------------------------------------

# 4. Sequence Diagram

The following sequence assumes a microservice-based architecture.

``` text
Customer        Frontend        Cart Service       Cart DB       Product Service
   │                │                 │               │                │
   │  Click Cart    │                 │               │                │
   │───────────────>│                 │               │                │
   │                │                 │               │                │
   │                │ GET /cart       │               │                │
   │                │────────────────>│               │                │
   │                │                 │               │                │
   │                │                 │ Find Cart     │                │
   │                │                 │──────────────>│                │
   │                │                 │               │                │
   │                │                 │ Cart + Items  │                │
   │                │                 │<──────────────│                │
   │                │                 │               │                │
   │                │                 │ Get Product Details            │
   │                │                 │────────────────────────────────>│
   │                │                 │               │                │
   │                │                 │ Product Data  │                │
   │                │                 │<────────────────────────────────│
   │                │                 │               │                │
   │                │                 │ Calculate totals              │
   │                │                 │───────┐       │                │
   │                │                 │       │       │                │
   │                │                 │<──────┘       │                │
   │                │                 │               │                │
   │                │ Cart Response   │               │                │
   │                │<────────────────│               │                │
   │                │                 │               │                │
   │  Display Cart  │                 │               │                │
   │<───────────────│                 │               │                │
```

------------------------------------------------------------------------

# 5. API Design

## 5.1 View Cart Endpoint

``` http
GET /api/v1/cart
```

### Request

The customer identity can be obtained from the authenticated
session/JWT.

Example:

``` http
GET /api/v1/cart
Authorization: Bearer <token>
```

### Successful Response

``` json
{
  "cartId": "C12345",
  "restaurant": {
    "id": "R100",
    "name": "Pizza House"
  },
  "items": [
    {
      "productId": "P101",
      "name": "Margherita Pizza",
      "quantity": 2,
      "unitPrice": 12.50,
      "subtotal": 25.00
    },
    {
      "productId": "P205",
      "name": "Coke",
      "quantity": 1,
      "unitPrice": 2.50,
      "subtotal": 2.50
    }
  ],
  "subtotal": 27.50,
  "deliveryFee": 3.00,
  "discount": 2.50,
  "tax": 1.50,
  "total": 29.50
}
```

------------------------------------------------------------------------

# 6. Important Business Rule --- One Restaurant Per Cart

For a food-delivery application, an important design decision is whether
a cart can contain products from multiple restaurants.

A recommended approach is:

> **One cart belongs to one restaurant.**

This makes the ordering and checkout process significantly simpler.

## Example

Customer currently has:

``` text
Pizza House
- Margherita Pizza x2
- Coke x1
```

The customer then tries to add:

``` text
Burger House
- Cheeseburger x1
```

The system should prevent the item from being added directly and display
a message such as:

> Your cart contains items from another restaurant. Do you want to clear
> your cart and add this item?

If the customer confirms:

1.  Existing cart items are removed.
2.  Burger House becomes the active restaurant.
3.  Cheeseburger is added to the new cart.

------------------------------------------------------------------------

# 7. Main Components

A possible architecture for the View Cart functionality is:

``` text
Customer
   │
   ▼
Frontend / Mobile App
   │
   ▼
API Gateway
   │
   ▼
Cart Service
   │
   ├──────────────► Cart Database
   │
   ├──────────────► Product Service
   │
   └──────────────► Restaurant Service
```
------------------------------------------------------------------------

# 8. Functional Requirements

The View Cart functionality should:

1.  Allow an authenticated customer to view their cart.
2.  Return all active cart items.
3.  Return the restaurant associated with the cart.
4.  Return item quantities.
5.  Return item prices.
6.  Calculate item subtotals.
7.  Calculate the cart subtotal.
8.  Apply applicable discounts.
9.  Calculate delivery fees.
10. Calculate taxes where applicable.
11. Return the final total.
12. Detect unavailable products.
13. Detect relevant price changes.
14. Detect restaurant availability/status.
15. Handle an empty cart.

------------------------------------------------------------------------

# 9. Non-Functional Considerations

## Performance

Viewing a cart is a high-frequency operation, so the endpoint should
have low latency.

Potential improvements:

-   Minimize calls to Product Service.
-   Batch product lookups instead of making one request per item.
-   Cache relatively static product information where appropriate.
-   Avoid unnecessary recalculation when data has not changed.

## Consistency

The cart should reflect sufficiently current:

-   Product availability
-   Product price
-   Restaurant status
-   Promotions

The final checkout operation should perform another validation because
cart information may change between viewing the cart and placing the
order.

## Security

The customer must only be able to access their own cart.

For example:

``` text
GET /api/v1/cart
```

should derive the customer identity from the authenticated context
rather than allowing the client to request an arbitrary `customerId`.

------------------------------------------------------------------------

# 10. Suggested Domain Model

A simple domain model could be:

``` text
Customer
   │
   │ 1
   │
   │
   ▼
 Cart
   │
   │ 1..*
   ▼
 CartItem
   │
   │ *
   ▼
 Product

Cart ───────────────► Restaurant
```

Example entities:

``` text
Cart
---------------------
id
customerId
restaurantId
status
createdAt
updatedAt


CartItem
---------------------
id
cartId
productId
quantity
unitPrice
createdAt
updatedAt
```

------------------------------------------------------------------------

# 11. View Cart --- End-to-End Flow

``` text
Customer
   │
   │ Click Cart
   ▼
Frontend
   │
   │ GET /api/v1/cart
   ▼
API Gateway
   │
   ▼
Cart Service
   │
   ├── Find cart by authenticated customer
   │
   ├── Retrieve cart items
   │
   ├── Retrieve current product information
   │
   ├── Validate availability
   │
   ├── Retrieve restaurant status
   │
   ├── Calculate subtotal
   │
   ├── Calculate discounts
   │
   ├── Calculate delivery fee
   │
   ├── Calculate tax
   │
   └── Calculate total
   │
   ▼
Cart Response
   │
   ▼
Frontend
   │
   ▼
Customer sees cart
```

------------------------------------------------------------------------