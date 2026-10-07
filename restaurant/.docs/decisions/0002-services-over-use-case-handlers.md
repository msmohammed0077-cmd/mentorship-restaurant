# 0002 — Services over use-case handlers

- **Status:** Accepted
- **Date:** 2026-10-07
- **Deciders:** Mohammed Gomaa, mentorship review (#84)

## Context
Every use-case was a `<UseCase>Handler` `@Service`, and each domain's `*Service` only delegated to its handlers, one method each. The service layer had no behaviour of its own, so every operation was spread over two classes. The mentorship review of #84 asked for the conventional controller → service shape, and #81 (module boundaries) was closed on the same grounds.

## Decision
- **One service per controller holds the logic:** `CartService`; `CustomerService`, `AddressService`, `PaymentMethodService`; `OrderService`, `OrderStatusService`, `OrderHistoryService`, `OrderRatingService`. Related services share their domain's directory.
- **Shared guards go on the domain's primary service.** A guard two services in a domain need lives on `OrderService` or `CustomerService`, which the others inject. Dependencies run one way.
- **Create-order keeps its chain of responsibility.** It is a pipeline of steps (validate the cart, address and items; pay, save, notify), which is what the pattern is for.
- **No vertical-slice example yet.** Layered everywhere keeps the move small; a slice can be trialled later on one use-case.
- **Across domains, inject the other domain's service, never its repository.** Existing violations are left for sub-project C of #84, because fixing them naively creates constructor-injection cycles that need ownership decisions first.

## Consequences
- An operation's logic is in the class its controller calls. Thirty handler classes go and three services arrive, 27 classes fewer in total.
- Services are larger. A guard needed by two services must be placed on the primary service deliberately.
- Calls between methods of one service skip Spring's proxy, so per-call transactions need `TransactionTemplate`, as in auto-reject.
- Cross-domain violations remain until C, and are listed in `CLAUDE.md` so they are not copied.
