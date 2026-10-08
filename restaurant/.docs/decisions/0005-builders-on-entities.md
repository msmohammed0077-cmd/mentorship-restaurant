# 0005 — Builders on entities

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** Mohammed Gomaa, mentorship review (#84)

## Context
Services built entities with `new` and a run of setters, one line per field. The mentorship review asked for builders there ("create builder"). Two entities already carried an unused `@Builder` with a public all-args constructor (`Transaction`, `OrderTracking`), and most entities had a public no-args constructor although `CLAUDE.md` named a protected one the default: `new` from a service in another package needed it.

## Decision
- **Every entity the application creates gets a builder:** `@Builder`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` for Hibernate, `@AllArgsConstructor(access = AccessLevel.PRIVATE)` for the builder. Twelve entities: `User`, `Customer`, `Address`, `PaymentMethod`, `Transaction`, `Cart`, `CartItem`, `Order`, `OrderItem`, `OrderRating`, `OrderStatusHistory`, `OrderTracking`.
- **Initialised fields carry `@Builder.Default`.** Lombok's builder ignores a field initialiser unless told otherwise, so `items = new ArrayList<>()` would arrive null. Four fields: `Customer.addresses`, `Customer.paymentMethods`, `Cart.items`, `Order.items`.
- **Builders create; setters update.** Updates rely on dirty checking (`setStatus`, `setUserDeletedAt`), so setters stay. Entities stay anemic: a builder is generated construction, not behaviour.
- **Entities the application never creates** (`Restaurant`, `Menu`, `MenuItem`) keep only the protected no-args constructor.

## Consequences
- Construction reads as one expression per entity, and no constructor is callable from outside the entity, so the public no-args constructors are gone.
- A new initialised field without `@Builder.Default` is a silent null; `CLAUDE.md` names the trap.
- Where two entities point at each other (`Order` and its `OrderItem`s), one is built first and the link set after.
