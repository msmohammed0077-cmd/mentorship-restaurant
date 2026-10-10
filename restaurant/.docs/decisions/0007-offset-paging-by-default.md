# 0007 — Offset paging by default, keyset for deep append-heavy lists

- **Status:** Accepted
- **Date:** 2026-10-10
- **Deciders:** Mohammed Gomaa, restaurant CRUD design (#85)

## Context
The restaurant index (#88) is the second paged endpoint. Order history ([0006](./0006-keyset-paging-with-the-scroll-api.md)) pages by keyset through the Scroll API, but Spring Data has no web argument resolver or JSON form for a keyset position, so every keyset endpoint writes its own cursor parameters and conversion. Offset paging is built in end to end: `Pageable` is resolved from `?page=&size=`, `spring.data.web.pageable.max-page-size` caps it, and `PagedModel` serialises the page with its metadata.

## Decision
- **Offset (`Pageable` → `Page` → `PagedModel`) is the default** for paged endpoints.
- **Keyset is for lists that are deep and appended to constantly**, where offset gets slow and pages shift under the reader. Order history is one; the restaurant directory, small and admin-curated, is not.
- The service fixes the sort (`PageRequest.of(page, size, Sort.by("id"))`): a client's `?sort=` is ignored, so pages are stable and no unindexed column can be requested.
- Size is clamped, not rejected: `max-page-size=50`.

## Consequences
- An offset endpoint needs no cursor code and no hand-written validation.
- Each page costs a count query as well as the select.
- Whether order history keeps its hand-built cursor is revisited in #110.
