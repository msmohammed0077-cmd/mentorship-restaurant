### Pseudocode

```text
FUNCTION createOrder(request):

    cart = find cart by request.cartId
    IF cart does not exist:
        THROW CartNotFoundException

    address = find address by request.addressId
    IF address does not exist:
        THROW AddressNotFoundException

    restaurant = the restaurant of the cart's items
    IF restaurant is deleted:
        THROW RestaurantNotFoundException        (404, cart kept, nothing charged)
    IF restaurant is closed:
        THROW RestaurantClosedException          (409, cart kept, nothing charged)

    transaction = null

    IF payment method is CARD:
        transaction = process payment using request.cardId

    order = create new Order using:
            request
            cart
            address
            transaction

    savedOrder = save order

    RETURN order response

    TODO: Notify restaurant
    TODO: Find and notify driver
END FUNCTION
```

### Exception Flows

Only the flows added since this spec was written; `CreateOrderEndpointTest` pins the rest.

- **Restaurant closed:** the cart holds items of a restaurant that has closed since they were added.
  409, "Restaurant is closed" — the same answer as add-to-cart. `RestaurantValidatorHandler`
  checks it after the items and before payment, so nothing is charged, and the cart is kept.
- **Restaurant deleted:** the cart holds items of a restaurant the admin has deleted since they
  were added. 404, "Restaurant not found". The same `RestaurantValidatorHandler` check, before
  payment; the cart is kept.

### Sequence Diagram

```text

Client          OrderService             CartService
  |                     |                      |
  |-- createOrder() --->|                      |
  |                     |-- findCart(cartId) ->|
  |                     |<----- Cart ----------|
  |                     |                      |
  |                     |---- findAddress(addressId) ---> AddressService
  |                     |<--------- Address ------------|
  |                     |                      |
  |                     |                      |
  |                     |-- process(cardId) --> PaymentProcessor
  |                     |<---- Transaction ----|   [CARD only]
  |                     |                      |
  |                     |-- create Order -----> OrderMapper
  |                     |<------ Order --------|
  |                     |                      |
  |                     |---- save(Order) ----> OrderRepository
  |                     |<--- Saved Order -----|
  |                     |                      |
  |<-- OrderResponse ---|                      |
```

### Flow Diagram
 ```text
                 ┌───────────────┐
                │ Start         │
                └───────┬───────┘
                        │
                        ▼
              ┌───────────────────┐
              │ Find Cart          │
              └─────────┬─────────┘
                        │
                 Cart found?
                  /          \
                No            Yes
                │              │
                ▼              ▼
        ┌─────────────┐   ┌───────────────────┐
        │ Throw        │   │ Find Address      │
        │ CartNotFound │   └─────────┬─────────┘
        └─────────────┘             │
                              Address found?
                               /          \
                             No            Yes
                             │              │
                             ▼              ▼
                    ┌──────────────┐   ┌─────────────────┐
                    │ Throw         │   │ Payment Method  │
                    │ AddressNotFound│  │ is CARD?        │
                    └──────────────┘   └────────┬────────┘
                                                │
                                           Yes /   \ No
                                             /       \
                                            ▼         ▼
                                  ┌──────────────┐   │
                                  │ Process      │   │
                                  │ Payment      │   │
                                  └──────┬───────┘   │
                                         │           │
                                         └─────┬─────┘
                                               ▼
                                  ┌────────────────────┐
                                  │ Create Order       │
                                  └──────────┬─────────┘
                                             │
                                             ▼
                                  ┌────────────────────┐
                                  │ Save Order         │
                                  └──────────┬─────────┘
                                             │
                                             ▼
                                  ┌────────────────────┐
                                  │ Return             │
                                  │ OrderResponse      │
                                  └────────────────────┘
 
 ```
