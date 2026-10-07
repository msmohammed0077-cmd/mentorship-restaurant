# 0003 — Domain layout

- **Status:** Accepted
- **Date:** 2026-10-07
- **Deciders:** Mohammed Gomaa, mentorship review (#84)

## Context
Code sat in whichever domain needed it first. Restaurants, menus and menu items lived in `cart/`; saved cards and the `User` login account in `customer/`; the payment processor in a generic `support/` package; `Transaction` in `order/`. The restaurant and menu CRUD in #85–#101 needed a home, and the cross-domain rule from ADR 0002 cannot be applied while ownership is wrong.

## Decision
- **`restaurant/`** owns restaurants, menus, menu items and their stock. The package path `com.mentorship.restaurant.restaurant` is accepted: `catalog/` was considered and rejected because nobody looking for restaurant CRUD would look there, and renaming the root package is out of proportion.
- **`payment/`** owns saved cards (`PaymentMethod` and everything around it), the processor (`PaymentProcessor`, its misspelling fixed), and `Transaction`: how a customer pays, the act of paying, and its record. The order request's `PaymentMethod` enum became `PaymentType` to stop sharing a name with the entity.
- **`user/`** owns `User`, `Gender` and `UserRepository`, because a restaurant has a login account too.
- **Exceptions move with their subject**, under a base class per domain (`RestaurantException`, `PaymentException`), each registered with one handler in `GlobalExceptionHandler`.
- `Address` stays in `customer/`: it is owned by the customer and deleted with them.

## Consequences
- Each domain's code is where its name says. #85–#101 build on `restaurant/`.
- The moves turned two same-domain repository uses into cross-domain ones (`CustomerService` → `UserRepository`, `PaymentMethodService` → `CustomerRepository`). They join the known violations that ADR 0004 and C2 of #84 resolve.
- A new domain needs an exception base and a handler method.
