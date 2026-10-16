# 0010 — Email-code login and sign-up through Keycloak

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** Mohammed Gomaa (#83, #120)

## Context

#83 asked for passwordless login. Keycloak (ADR 0007) owns credentials, but 26.5 has no
authenticator that emails a one-time code: its built-in passwordless option is passkeys. Until
now `POST /customers` stored a BCrypt password and created no Keycloak user, so a new customer
could never log in, and a Keycloak user without a `users` row got 401 (ADR 0009).

## Decision

- **Login is an emailed code and nothing else**, through the community plugin
  `for-keycloak/email-otp-authenticator` (Unlicense), pinned by version and SHA-256 in
  `docker/keycloak/Dockerfile`. The realm's browser flow is Username Form → the plugin.
- **Sign-up happens on Keycloak's registration page**, with no password step and with email
  verification on: Keycloak logs a new user in without the login flow, so only the link proves
  the address. Every new user gets the `CUSTOMER` realm role by default.
- **Our rows are created on the first request.** A verified token with no `users` row becomes a
  user and customer (`CustomerService.registerFromToken`, called from
  `CallerAuthenticationConverter`) if it is a sign-up: `email_verified: true`, `customer.write`,
  no kitchen permission, and issued to a client in `app.security.sign-up-clients`. Anything else
  unknown stays a 401. An active account already holding the email is refused, not linked.
- **Keycloak's email wins.** When a verified token's email differs from ours, ours is updated, so
  a stale copy cannot block whoever registers with the freed address.
- **Brute-force protection is on**: a 6-digit code would otherwise allow unlimited guesses.
- The API stores no password: `user_password`, the BCrypt bean, `POST /customers` and the
  change-password endpoint are gone. Keycloak owns the login email, so `PATCH /customers/me` no
  longer accepts one.

## Consequences

- No Keycloak admin credential in the API, and no write to two systems in one use-case.
- Upgrading Keycloak needs a plugin build for the new version; the plugin publishes one per
  release, and the Dockerfile pins both together.
- Development needs Mailpit (`http://localhost:8025`) to log in at all.
- A user an admin creates in the console also gets `CUSTOMER`. Staff are safe once they hold a
  kitchen permission; a console user with only the default role becomes a customer on first login.
- Deleting a customer only soft-deletes our row; the Keycloak user remains, can log in and meets
  404s, and their email cannot sign up again. Fixing that needs the admin API.
- Rejected: Keycloak passwords (not passwordless); a plugin of our own (a second module to
  maintain); sign-up through our API calling Keycloak's admin API (privileged credential, fake
  Keycloak in tests, compensation when one write fails).
