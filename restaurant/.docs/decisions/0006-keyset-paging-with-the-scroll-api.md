# 0006 — Keyset paging with the Scroll API

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** Mohammed Gomaa, mentorship review (#84)

## Context
Order history paged by keyset with two hand-written `@Query` methods (the first page, and pages after a cursor) and a `KeysetPage<T>` helper that read `limit + 1` rows, trimmed, and answered "is there another page". The mentorship review pointed at "JPA Pagination". Spring Data JPA has keyset scrolling built in: a repository method takes a `ScrollPosition` and returns a `Window<T>`.

## Decision
- **History uses the Scroll API.** One derived method, `findByCustomer_IdOrderByCreatedAtDescIdDesc(customerId, ScrollPosition, Limit)`, returns a `Window<Order>`. Spring writes the `createdAt`/`id` keyset condition, reads one extra row for `hasNext()`, and continues from `ScrollPosition.forward(keys)`. `KeysetPage` is gone.
- **The method is derived because it has to be.** Spring Data scrolls derived queries, Query-by-Example and Querydsl, not `@Query` ("Scrolling with String-based query methods is not yet supported"). Rows are therefore entities, mapped by `OrderMapper.toSummary`, not a DTO built in the query.
- **The item count is one grouped query per page** (`OrderItemRepository.countItemsByOrderIds`), the one piece a derived query cannot express. Rejected: a count per row (N+1), `@Formula` on `Order` (a query on an entity), loading the lines to count them.
- **The entity graph fetches the restaurant and the transaction.** `Order.transaction` is an eager inverse one-to-one; without it in the graph, Hibernate issues one SELECT per order.
- **The API does not change:** the cursor stays `cursor.createdAt` / `cursor.orderId`, converted to and from Spring's keyset position.

## Consequences
- The keyset maths, the over-fetch and the first-page/next-page split are Spring's, not ours.
- A history page is two SELECTs instead of one.
- A future keyset endpoint follows the same shape: a derived `Window` method, and a projection query for anything it cannot express.
