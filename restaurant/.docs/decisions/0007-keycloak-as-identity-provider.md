# 0007 — Keycloak as identity provider

- **Status:** Accepted
- **Date:** 2026-10-14
- **Deciders:** Mohammed Gomaa (#83)

## Context
Nothing is authenticated: every endpoint is open, and callers name themselves through `customerId`, `restaurantId` and `role` parameters. The application stores passwords itself. #83 asked how to log users in (password or OTP), how to model access (roles or permissions), whether to build it ourselves or use Keycloak, and whether to use JWTs or sessions. Building it ourselves means owning credentials, OTP delivery and rate limiting, token revocation and refresh, key rotation, and an admin interface for roles.

## Decision
- **Keycloak is the identity provider.** It owns credentials, login (passwordless OTP later), sessions and refresh tokens, roles and groups, and the admin interface.
- **The API is an OAuth2 resource server that validates JWTs locally** (`spring-boot-starter-oauth2-resource-server`), checking signature, expiry, issuer and audience against Keycloak's published keys. No call to Keycloak per request.
- **Our `users` row links to the Keycloak user** through `users.user_keycloak_id`, matched against the token's `sub`. Our primary keys stay as they are.
- **Ownership stays in our database.** Keycloak roles say what kind of user someone is; which customer, restaurant, cart or order is theirs stays in Postgres and in the services' guards.
- **Keycloak's tables live in a `keycloak` schema of our database**, owned by a `keycloak` login role, created by a Postgres init script. Flyway manages `public` only.

## Consequences
- We write no login, OTP, password-reset or token code, and get an admin console for users and roles.
- One more service to run locally; realm configuration is a committed file, and changing it needs a database reset.
- Each user exists twice: in Keycloak and in `users`. Part 3 of the #83 roadmap decides how a new Keycloak user gets its row.
- Role changes reach the API when the user's next token is issued (about five minutes by default), not instantly.
- Tests sign their own tokens with a test key, so the API is tested without Keycloak; the realm file is checked by hand.
