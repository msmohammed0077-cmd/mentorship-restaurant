# Goal

Add an item to the customer's cart.

## Actor

Customer

## Preconditions

1. Customer is logged in / registered.
2. Restaurant exists.
3. Item exists.

## Business Rules

1. A `CartItem` is unique by **(item + selected options)**. The same item with *different* options is a separate line, not an increment.
2. A cart holds items from a **single restaurant** at a time.

## Main Success Scenario

1. Customer adds an item to the cart (with quantity and options).
2. System verifies the restaurant is open.
3. System verifies the item is in stock for the requested quantity.
4. System verifies the cart is empty OR the item is from the same restaurant as the cart.
5. If no cart exists, System creates one.
6. System adds the `CartItem` and recalculates totals.

## Exception Flows

Each branch is labelled by the Main Flow step it extends.

- **1a. Item (same options) already in cart:** increment the `CartItem` quantity instead of inserting a new line; still enforce stock (step 3); recalculate totals.
- **2a. Restaurant is closed:** reject — "Restaurant is closed".
- **3a. Item is out of stock:** reject — "Item is not in stock".
- **4a. Item is from a different restaurant:** reject — "Item is of different restaurant"; prompt the customer to clear the cart or continue with the current restaurant.

## Postconditions

1. `Cart` exists (created if it didn't).
2. `CartItem` is present in `Cart` (inserted or incremented).
3. Totals are recalculated.

## Diagram

1. [Flowchart](images/flowchart.png)
2. [Sequence Diagram](images/sequence-diagram.png)
3. [Pseudocode](images/pseduocode.txt)

# Time Estimate
1. Assuming system has been already set-up, i.e. deployed, integrated with e.g. Redis/DB, etc...
2. Assuming knowledge of Java/Java Spring.
3. 12-16 hours
    3. 6-8 hours domain research for different solutions.
    4. 4 hours implementation of the use-case.
    5. 1-2 hours testing of the use-case.
    6. 1-2 wasted time (e.g. meetings)

# Notes

1. Should we split this use-case into `increment-cart-item` / `decrement-cart-item`?
