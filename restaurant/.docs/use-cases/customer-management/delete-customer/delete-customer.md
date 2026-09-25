# Goal

Let a customer close their account without losing the history other people depend on.

Issue [#72](https://github.com/msmohammed0077-cmd/mentorship-restaurant/issues/72), under the
Customer Management umbrella [#68](https://github.com/msmohammed0077-cmd/mentorship-restaurant/issues/68).

## Actor

Any caller. There is no auth yet (see *Authorisation*).

## Preconditions

The customer exists, is not soft-deleted, and has no order in flight.

## Scope

`DELETE /api/v1/customers/{customerId}` — **204**, no body. A **soft** delete.

Builds on #73, #69 and #71 (`CustomerController`, `CustomerService`,
`CustomerRepository.findActiveById`). Adds `DeleteCustomerHandler`,
`CustomerHasActiveOrdersException`, `OrderRepository.existsByCustomer_IdAndStatusIn` and
`CartRepository.deleteByCustomer_Id`.

Also makes the older customer lookups outside this umbrella exclude soft-deleted customers (see
*Soft-deleted customers elsewhere*).

## Business Rules

1. An **unknown or already soft-deleted** customer is refused with 404, "Customer not found"
   (`findActiveById`). Deleting twice is therefore a 404 the second time, not a silent 204.
2. A customer with an order in a **non-terminal** status — `PLACED`, `ACCEPTED`, `PREPARING`,
   `READY_FOR_PICKUP`, `PICKED_UP` — is refused with **409**, "Customer has active orders"
   (`CustomerHasActiveOrdersException`). A restaurant must never lose the customer of an order in
   flight. The set of active statuses is derived from `OrderStatus.isTerminal()`, so a new status
   is classified in one place.
3. Otherwise, in one transaction:
   - `user_deleted_at` is set to now on the customer's user;
   - the customer's cart is deleted in **one** statement; its `cart_items` go with it by
     `ON DELETE CASCADE`.
4. **Past orders, ratings and addresses are kept** for history. Nothing else is deleted.
5. Afterwards the customer **does not exist** to the API: `GET`, `PATCH`, the password endpoint,
   every address operation, add-to-cart, order history, cancel and rate all return 404, "Customer
   not found". Their email is **reusable** by a new sign-up, because the partial unique index
   `uq_users_active_email` only covers active users.

## Authorisation

None, per #34's trade. This is an **IDOR**: anyone can delete any customer's account by id.
Closing it belongs to auth, not to this umbrella.

## API

```http
DELETE /api/v1/customers/4
```

Response, **204**, no body.

## Data Access

```java
boolean existsByCustomer_IdAndStatusIn(Long customerId, Collection<OrderStatus> statuses);
```

A derived `exists` query: one `select … limit 1`, no entity loaded.

```java
@Modifying(flushAutomatically = true)
@Query("delete from Cart cart where cart.customer.id = :customerId")
int deleteByCustomer_Id(Long customerId);
```

**One statement, scoped to the customer.** A derived `deleteBy…` without `@Query` would load the
cart and delete it entity by entity.

**Bulk query vs managed entity.** The handler sets `userDeletedAt` on the user it loaded, then runs
the bulk delete. `flushAutomatically = true` flushes the soft-delete `UPDATE` before the `DELETE`
runs, and the handler reads nothing afterwards, so there is no stale entity to map. No
`clearAutomatically` is needed.

## Soft-deleted customers elsewhere

#68 says soft-deleted customers do not exist to the API, but the address, cart and order handlers
predate that and looked customers up with plain `findById` / `existsById`. They now filter
`user.userDeletedAt is null` too:

| Handler | Before | Now |
| --- | --- | --- |
| `ViewAddressesHandler` | `findByIdWithAddresses` (no filter) | `findByIdWithAddresses` (filters soft-deleted) |
| `AddAddressHandler` | `findById` | `findActiveById` |
| `AddToCartHandler` | `findById` | `findActiveById` |
| `ViewOrderHistoryHandler` | `existsById` | `existsActiveById` (new) |

Update / set-default / delete address and cancel / rate order look up the address or order, not the
customer, so there was no customer lookup to filter. [#78](https://github.com/msmohammed0077-cmd/mentorship-restaurant/issues/78)
added an `existsActiveById` check before those lookups (for cancel, in the `CUSTOMER` ownership
branch of `UpdateOrderStatusHandler`). A deleted customer has no cart, so the cart-by-id endpoints
have nothing to reach.

## Main Success Scenario

1. The caller asks to delete a customer by id.
2. The system loads the active customer and its user.
3. The system checks the customer has no order in a non-terminal status.
4. The system sets `user_deleted_at` on the user.
5. The system deletes the customer's cart (flushing the soft-delete first).
6. The system returns 204.

## Exception Flows

- **1a. Id is not a number:** 400 (type mismatch, before the handler runs).
- **2a. No active customer with that id** (unknown, or already deleted): 404, "Customer not found".
- **3a. An order is still in flight:** 409, "Customer has active orders". Nothing changes.

## Postconditions

- `users.user_deleted_at` is set; the `users` and `customers` rows remain.
- The customer has no `carts` or `cart_items` rows.
- Orders, order items, ratings and addresses are unchanged.

## Diagram

### Sequence Diagram

```mermaid
sequenceDiagram
    actor Caller
    participant C as CustomerController
    participant S as CustomerService
    participant H as DeleteCustomerHandler
    participant CR as CustomerRepository
    participant OR as OrderRepository
    participant CaR as CartRepository

    Caller->>C: DELETE /api/v1/customers/{customerId}
    C->>S: deleteCustomer(customerId)
    S->>H: deleteCustomer(customerId)
    H->>CR: findActiveById(customerId)
    CR-->>H: Optional<Customer> (user join-fetched)
    alt empty (unknown or soft-deleted)
        H-->>C: CustomerNotFoundException (404)
        C-->>Caller: 404 Not Found
    end
    H->>OR: existsByCustomer_IdAndStatusIn(customerId, active statuses)
    OR-->>H: boolean
    alt an order is in flight
        H-->>C: CustomerHasActiveOrdersException (409)
        C-->>Caller: 409 Conflict
    end
    H->>H: user.setUserDeletedAt(now) (dirty checking)
    H->>CaR: deleteByCustomer_Id(customerId)
    Note over H,CaR: flushAutomatically writes the UPDATE first,<br/>then one DELETE; cart_items cascade
    H-->>C: void
    C-->>Caller: 204 No Content
```

## Structure

```text
CustomerController -> CustomerService        (delegates only)
                   -> DeleteCustomerHandler  (@Service, @Transactional)
                   -> CustomerRepository     (findActiveById)
                   -> OrderRepository        (existsByCustomer_IdAndStatusIn)
                   -> CartRepository         (deleteByCustomer_Id, bulk)
```

## Testing

`DeleteCustomerEndpointTest`, end-to-end, on customers the test creates through
`POST /api/v1/customers`. Seeded customers 1–3 are never touched.

| Case | Expected |
| --- | --- |
| Delete a new customer | 204; then `GET` → 404; a second `DELETE` → 404, "Customer not found" |
| Customer with a cart (item added via the cart API) | 204; no `carts` row left for the customer |
| Customer with a `PLACED` order (seeded via SQL) | 409, "Customer has active orders"; `GET` still 200 |
| Unknown id `999999` | 404, "Customer not found" |
| Address list for a deleted customer | 404, "Customer not found" |

Every email the test creates starts with `delete.customer.test.`, and cleanup deletes only those
users. The `customers` rows, and the carts, cart items and orders seeded for them, go with them by
cascade.

# Notes

1. **Address update / set-default / delete, and order cancel / rate** used to reach a deleted
   customer's rows; closed by #78 (see *Soft-deleted customers elsewhere*).
2. **No restore.** Undeleting is not exposed; it would also have to handle the email having been
   reused meanwhile.
3. **A check-then-act race.** An order placed between the active-order check and the commit is not
   seen. There is no checkout for a customer without a cart, and the cart is deleted in the same
   transaction, so the window is small. Accepted.
4. **IDOR** is #34's accepted trade, tracked with auth.
