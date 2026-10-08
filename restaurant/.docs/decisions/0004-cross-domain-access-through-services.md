# 0004 — Cross-domain access through services

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** Mohammed Gomaa, mentorship review (#84)

## Context
ADR 0002 wrote down that a service injects only its own domain's repositories, and listed the services that did not. ADR 0003 put each class in the domain that owns it, which made the list final: eleven injections of another domain's repository. Fixing them naively creates constructor-injection cycles (`CustomerService ↔ OrderService` through delete-customer's active-order check and create-order's address lookup; `CustomerService ↔ CartService` through add-to-cart's customer check and delete-customer's cart delete), and Spring refuses to start with either.

## Decision
- **A service injects only its own domain's repositories.** For another domain's data or actions it calls that domain's service. New services give the domains without one a door: `UserService`, `RestaurantService`.
- **Methods other domains call carry no `@Transactional`**; they join the caller's transaction, like the shared guards of ADR 0002.
- **Entity mappings across domains are allowed** (`Order.customer`, `CartItem.menuItem`, `Customer.paymentMethods`), and JPQL may join through them: they are the schema's foreign keys. This answers #81's open question. Only bean injection is restricted.
- **A use-case whose dependencies would close a cycle gets its own service.** Delete-customer is the only customer use-case that needs order, and order needs customer through cart and address. In `CustomerDeletionService` nothing points back at it.
- **Delete-customer keeps the cart**, following the mentor's "no need" on that delete. That removed the second cycle outright.

## Alternatives rejected
- **An `*Api` leaf per domain** that injects only repositories: cycle-free by construction, but a second kind of class per domain, and unnecessary once delete-customer is out.
- **Domain events** for delete-customer: they cannot express "refuse while orders are active" without a veto by exception from a listener.
- **`@Lazy`**: hides the cycle instead of removing it.
- **Dropping the up-front "is the customer active?" checks** as an authentication concern: right once auth exists, but until then it turns unknown customer ids into 500s. The checks stay, through `CustomerService`.

## Consequences
- The rule is checkable with one grep (in `CLAUDE.md`), and Spring starting is the proof there is no cycle.
- Create-order's "Cart Not Found" and "Address Not Found" became the cart and address services' "Cart not found" and "Address not found". Status codes are unchanged.
- A soft-deleted customer's cart is kept, and the cart-by-id endpoints can still reach it: #104.
- The saved-card list reads `Customer.paymentMethods` lazily (two queries instead of one join fetch). The order comes from the mapping's `@OrderBy`, unchanged.
