# Food Delivery System — Cart Management
## Use Case: Clear Cart

### 1. Overview

This document describes the **Clear Cart** functionality within the Cart Management module of a Food Delivery System.

The feature allows a customer to remove all items from their current cart in a single operation.

It covers:
- Clearing all items from the customer's cart
- Validating cart ownership
- Handling an empty or non-existing cart
- Removing or deactivating all cart items
- Returning the updated empty-cart state
- Maintaining transactional consistency

---

# 2. Use Case — Clear Cart

## 2.1 Use Case Information

| Attribute | Description |
|---|---|
| Use Case | Clear Cart |
| Actor | Customer |
| Module | Cart Management |
| Trigger | Customer clicks "Clear Cart" |
| Precondition | Customer is authenticated |
| Postcondition | All active items are removed from the cart |

## 2.2 Main Flow

1. Customer opens the Cart.
2. Customer clicks **Clear Cart**.
3. Frontend displays a confirmation message.
4. Customer confirms the action.
5. Frontend sends `DELETE /api/v1/cart/items`.
6. Cart Service identifies the customer's cart.
7. Cart Service validates cart ownership.
8. Cart Service removes or deactivates all cart items.
9. Cart Service updates the cart state.
10. Cart Service returns a successful response.
11. Frontend displays the empty-cart state.

## 2.3 Alternative Flows

### A1 — Customer Cancels

If the customer cancels the confirmation:
- No API request is sent.
- Cart remains unchanged.

### A2 — Cart Does Not Exist

If no cart exists:
- Treat the operation as idempotent.
- Return an empty-cart response.

### A3 — Cart Is Already Empty

If the cart contains no active items:
- No database changes are required.
- Return an empty-cart response.

### A4 — Database Operation Fails

If clearing fails:
- Roll back the transaction.
- Keep the existing cart unchanged.
- Return an appropriate error response.

### A5 — Concurrent Cart Modification

If another request modifies the cart at the same time:
- Apply the system's transaction/concurrency strategy.
- Ensure the final cart state is consistent.

---

# 3. Flow Chart

```text
                ┌──────────────┐
                │   Customer   │
                └──────┬───────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Open Cart       │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Click           │
              │ "Clear Cart"    │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Confirmation    │
              │ Dialog          │
              └────────┬────────┘
                       │
                  Confirm?
                  /       \
                No         Yes
                │           │
                ▼           ▼
        ┌──────────────┐  ┌─────────────────┐
        │ Keep Cart    │  │ DELETE          │
        │ Unchanged    │  │ /cart/items     │
        └──────────────┘  └────────┬────────┘
                                   │
                                   ▼
                          ┌─────────────────┐
                          │ Find Customer   │
                          │ Cart            │
                          └────────┬────────┘
                                   │
                                   ▼
                          ┌─────────────────┐
                          │ Cart Exists?    │
                          └────────┬────────┘
                             No    │    Yes
                          ┌────────┘      └────────┐
                          ▼                        ▼
                   ┌──────────────┐       ┌─────────────────┐
                   │ Return Empty │       │ Remove/Deactivate│
                   │ Cart         │       │ Cart Items       │
                   └──────┬───────┘       └────────┬────────┘
                          │                         │
                          │                         ▼
                          │                ┌─────────────────┐
                          │                │ Update Cart     │
                          │                │ State           │
                          │                └────────┬────────┘
                          │                         │
                          └────────────┬────────────┘
                                       ▼
                              ┌─────────────────┐
                              │ Return Success  │
                              │ Empty Cart      │
                              └────────┬────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │ Display Empty   │
                              │ Cart            │
                              └─────────────────┘
```

---

# 4. Sequence Diagram

```text
Customer        Frontend        Cart Service       Cart DB
   │                │                 │               │
   │ Click Clear    │                 │               │
   │───────────────>│                 │               │
   │                │                 │               │
   │                │ Confirmation    │               │
   │<───────────────│                 │               │
   │                │                 │               │
   │ Confirm        │                 │               │
   │───────────────>│                 │               │
   │                │                 │               │
   │                │ DELETE /cart/items              │
   │                │────────────────>│               │
   │                │                 │               │
   │                │                 │ Find Cart     │
   │                │                 │──────────────>│
   │                │                 │               │
   │                │                 │ Cart           │
   │                │                 │<──────────────│
   │                │                 │               │
   │                │                 │ Delete/       │
   │                │                 │ deactivate    │
   │                │                 │ cart items    │
   │                │                 │──────────────>│
   │                │                 │               │
   │                │                 │ Success        │
   │                │                 │<──────────────│
   │                │                 │               │
   │                │                 │ Update state  │
   │                │                 │──────────────>│
   │                │                 │               │
   │                │ Success / Empty Cart            │
   │                │<────────────────│               │
   │                │                 │               │
   │ Empty Cart     │                 │               │
   │<───────────────│                 │               │
```

---

# 5. API Design

## Clear Cart Endpoint

```http
DELETE /api/v1/cart/items
```

Example request:

```http
DELETE /api/v1/cart/items
Authorization: Bearer <token>
```

The backend should derive the customer identity from the authenticated security context rather than accepting an arbitrary `customerId`.

---

# 6. Successful Response

One possible response:

```http
HTTP/1.1 200 OK
```

```json
{
  "cartId": "C12345",
  "restaurant": null,
  "items": [],
  "subtotal": 0.00,
  "deliveryFee": 0.00,
  "discount": 0.00,
  "tax": 0.00,
  "total": 0.00
}
```

An alternative is:

```http
HTTP/1.1 204 No Content
```

if the frontend does not need the updated cart representation.

For a frontend application, `200 OK` with the empty cart can be convenient because the UI can immediately use the response.

---

# 7. Error Responses

### Unauthorized

```http
HTTP/1.1 401 Unauthorized
```

Customer is not authenticated.

### Forbidden

```http
HTTP/1.1 403 Forbidden
```

Customer is authenticated but does not have permission to access the cart.

### Internal Server Error

```http
HTTP/1.1 500 Internal Server Error
```

An unexpected persistence or service error occurred.

---

# 8. Database Operation

There are two common approaches.

## Option 1 — Hard Delete

```sql
DELETE FROM cart_item
WHERE cart_id = :cartId;
```

Advantages:
- Simple
- No inactive records remain

Disadvantages:
- Cart-item history is lost
- Auditing is harder

## Option 2 — Soft Delete / Deactivation

```sql
UPDATE cart_item
SET status = 'REMOVED',
    updated_at = CURRENT_TIMESTAMP
WHERE cart_id = :cartId
  AND status = 'ACTIVE';
```

Advantages:
- Maintains history
- Better auditability

Disadvantages:
- Historical records remain
- Queries must filter inactive items

The choice depends on whether cart history/auditing is a requirement.

---

# 9. Transaction Management

Clearing the cart should normally be performed inside a database transaction.

```text
BEGIN TRANSACTION

    Find customer's cart

    Remove/deactivate all active cart items

    Update cart state

COMMIT
```

If an error occurs:

```text
BEGIN TRANSACTION

    Remove/deactivate cart items
          │
          X Error
          │
       ROLLBACK

Cart remains unchanged
```

This prevents a partially cleared cart.

---

# 10. Main Components

```text
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
   └──────────────► Cart Database
```

### Frontend

- Display Clear Cart action.
- Ask for confirmation.
- Send DELETE request.
- Handle success/error responses.
- Update UI to show an empty cart.

### API Gateway

- Route the request to Cart Service.
- Handle authentication/authorization where applicable.

### Cart Service

- Identify authenticated customer.
- Find customer's cart.
- Validate cart ownership.
- Remove/deactivate cart items.
- Maintain transactional consistency.
- Return updated cart state.

### Cart Database

Stores:
- Cart
- Cart items
- Customer/cart relationship
- Product references
- Quantities
- Item status
- Timestamps

---

# 11. Functional Requirements

The Clear Cart functionality should:

1. Allow an authenticated customer to clear their cart.
2. Require confirmation in the UI.
3. Remove all active cart items.
4. Only allow the customer to modify their own cart.
5. Handle a non-existing cart safely.
6. Handle an already-empty cart safely.
7. Be idempotent.
8. Maintain database consistency.
9. Return an appropriate success response.
10. Return an appropriate error if the operation fails.
11. Update the frontend after successful clearing.

---

# 12. Non-Functional Considerations

## Performance

Prefer a bulk database operation:

```sql
DELETE FROM cart_item
WHERE cart_id = :cartId;
```

instead of deleting each item individually.

## Security

Avoid:

```http
DELETE /api/v1/cart/items?customerId=123
```

Prefer:

```http
DELETE /api/v1/cart/items
Authorization: Bearer <token>
```

The Cart Service obtains the customer identity from the authenticated request.

## Idempotency

Calling Clear Cart multiple times should produce the same final state:

```text
Initial:
Cart = [Pizza, Coke, Burger]

Clear Cart
    ↓
Cart = []

Clear Cart again
    ↓
Cart = []
```

## Concurrency

The system should define behavior for simultaneous requests such as:

```text
Request A: Clear Cart
Request B: Add Item
```

The transaction/concurrency strategy should ensure a predictable and consistent final state.

---

# 13. Suggested Domain Model

```text
Customer
   │
   │ 1
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
```

Example:

```text
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
status
createdAt
updatedAt
```

Possible cart statuses:

```text
ACTIVE
CHECKED_OUT
ABANDONED
```

Possible cart-item statuses:

```text
ACTIVE
REMOVED
```

---

# 14. Clear Cart — End-to-End Flow

```text
Customer
   │
   │ Click "Clear Cart"
   ▼
Frontend
   │
   │ Show confirmation
   ▼
Customer
   │
   │ Confirm
   ▼
Frontend
   │
   │ DELETE /api/v1/cart/items
   ▼
API Gateway
   │
   ▼
Cart Service
   │
   ├── Identify authenticated customer
   │
   ├── Find customer's cart
   │
   ├── Validate cart ownership
   │
   ├── Remove/deactivate cart items
   │
   ├── Update cart state
   │
   └── Commit transaction
   │
   ▼
Cart Response
   │
   ▼
Frontend
   │
   ▼
Display Empty Cart
```

---

# 15. View Cart vs Clear Cart

| Aspect | View Cart | Clear Cart |
|---|---|---|
| HTTP Method | GET | DELETE |
| Endpoint | `/api/v1/cart` | `/api/v1/cart/items` |
| Changes data | No | Yes |
| Reads Cart DB | Yes | Yes |
| Product Service | Usually yes | Usually no |
| Restaurant Service | Possibly | Usually no |
| Transaction | Usually read-only | Yes |
| Idempotent | Yes | Yes |
| Main purpose | Display cart | Remove all items |
| Result | Current cart | Empty cart |

---
