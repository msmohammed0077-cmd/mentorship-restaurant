# 0009 — The caller is resolved after verification

- **Status:** Accepted
- **Date:** 2026-10-15
- **Deciders:** Mohammed Gomaa (#83, #119)

## Context
ADR 0007 made Keycloak the identity provider and linked a Keycloak user to our `users` row through `users.user_keycloak_id`; ADR 0008 made endpoints check permissions. Callers still named themselves through `customerId`, `restaurantId` and `role` parameters, so any caller with a permission could act as anyone. The API needs our ids — the user's, and the customer's when the user is one — for every ownership rule. They can travel in the token as custom claims, or the API can look them up from `sub`.

## Decision
- **The token carries only Keycloak's identity.** After Spring verifies a token, a `Converter<Jwt, AbstractAuthenticationToken>` resolves `sub` → `users` → `customers` with one query and makes the result, a `Caller(userId, customerId)`, the authentication's principal. Controllers read it with `@AuthenticationPrincipal`.
- **A token whose `sub` has no `users` row is rejected with 401.**
- **A customer never names themselves in a request.** Customer-side queries are scoped by the caller's customer id, and an empty result is a 404 whether the thing is missing or someone else's.
- **A restaurant owner names the restaurant,** because one user may come to act for several; the query that checks it includes the caller.
- **Nothing bypasses ownership,** including `ADMIN`.

## Consequences
- `users` stays the only source of our ids. The realm file needs no user-profile attributes or mappers, and no attribute can be edited to impersonate someone.
- Every authenticated request costs one indexed lookup. There is no cache until a measurement asks for one.
- A Keycloak user exists to the API only once it has a `users` row; part 3 decides how sign-up creates it.
- Role and permission changes still reach a user with their next token (ADR 0007, 0008); a deleted user's token keeps resolving until it expires, and the use-cases' soft-delete rules answer it.
