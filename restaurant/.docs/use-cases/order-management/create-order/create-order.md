### Pseudocode

```text
FUNCTION createOrder(request):

    cart = find cart by request.cartId
    IF cart does not exist:
        THROW CartNotFoundException

    address = find address by request.addressId
    IF address does not exist:
        THROW AddressNotFoundException

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
### Sequence Diagram

```text

Client          CreateOrderHandler       CartRepository
  |                     |                      |
  |-- createOrder() --->|                      |
  |                     |-- findById(cartId) ->|
  |                     |<----- Cart ----------|
  |                     |                      |
  |                     |---- findById(addressId) ---> AddressRepository
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
