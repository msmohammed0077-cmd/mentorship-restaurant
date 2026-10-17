# 0008 — Permissions as Keycloak client roles

- **Status:** Accepted
- **Date:** 2026-10-14
- **Deciders:** Mohammed Gomaa (#83)

## Context
ADR 0007 made Keycloak the identity provider and kept ownership in our database. Part 4 of #83 decides what kind of user may call each endpoint. Two actor kinds have endpoints today (customers and restaurant owners), more are listed in `actors.md` (cashier, courier, support), and an administrator should be able to create a new role from existing capabilities without a code change or a deploy. Plain role checks (`hasAnyRole("CUSTOMER", "RESTAURANT_OWNER")`) would put role names in every endpoint shared by several roles, so a new role means editing each of them.

## Decision
- **Each capability is a permission, defined as a client role of `restaurant-api`** (`order.accept`, `cart.write`, …).
- **Roles are Keycloak realm roles that are composites of those permissions.** Keycloak expands composites when it issues a token, so the token's `resource_access.restaurant-api.roles` lists the user's effective permissions.
- **The API checks permissions only.** A converter maps `resource_access.restaurant-api.roles` to authorities with no prefix; each controller method carries `@PreAuthorize("hasAuthority('<permission>')")`. The API never reads `realm_access` and never names a role.
- **Ownership stays ours,** as ADR 0007 decided: a permission says the caller may accept orders, not which orders.

## Consequences
- A new role is data: an admin composes it from existing permissions in the console. A new permission is code: an endpoint has to check it, and the realm file has to define it.
- The role–permission matrix lives in Keycloak. The committed realm file is the development baseline; roles created in a console drift from it until someone exports them.
- Tests read the realm file to build tokens with each role's permissions, so CI checks the composites without running Keycloak.
- A permission change reaches a user with their next token, as ADR 0007 noted for roles.
