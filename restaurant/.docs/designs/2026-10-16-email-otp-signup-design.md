# #83 part 3 — Email-code login and sign-up through Keycloak

Part 3 of `2026-10-14-auth-roadmap.md` (#120), built on part 2 (`2026-10-15-authenticated-caller-design.md`). After part 2 every request is made by a resolved caller, but two holes remain: `POST /customers` creates a `users` row with a password and no Keycloak user, so the new customer can never log in; and a Keycloak user with no `users` row gets 401. Part 3 moves sign-up and login into Keycloak, makes login passwordless (a code sent by email), and creates our rows on the first request.

## Decisions

- **Login is an email code, and nothing else.** The person enters their email, receives a code, and enters it. No password exists anywhere: not in Keycloak, not in our database, not in the API.
- **Keycloak 26.5 has no email-code authenticator, so a community plugin provides it:** [`for-keycloak/email-otp-authenticator`](https://github.com/for-keycloak/email-otp-authenticator). It publishes a prebuilt JAR per Keycloak version (26.5.7 among them), is released under the Unlicense, can be the sole factor, and has the code length, lifetime, resend cooldown and per-user cap we want. ADR 0010 records why.
- **Sign-up happens on Keycloak's page.** Registration is switched on in the realm, without a password. The API has no sign-up endpoint and holds no Keycloak admin credential.
- **Our rows are created on the first request.** A verified token whose `sub` has no `users` row, and which holds `customer.write`, becomes a new user and customer. Anything else unknown stays a 401.
- **Every sign-up gets `CUSTOMER`** through the realm's default roles.
- **Keycloak owns the login email.** `PATCH /customers/me` no longer accepts `email`; our copy is written once, at first request. Name, phone, date of birth and gender stay ours and stay editable.

## The Keycloak side

**Image and plugin.** The Keycloak image moves from `26.5.2` to `26.5.7`, a patch release the plugin builds for. A new `restaurant/docker/keycloak/Dockerfile` (not under `keycloak/`, which is mounted as the import directory) starts from `quay.io/keycloak/keycloak:26.5.7` and adds `email-otp-authenticator-v1.7.0-kc-26.5.7.jar` (v1.8.1 was two days old when this was written; v1.7.0 has every setting used here) to `/opt/keycloak/providers/`, downloaded at a pinned version and checked against a pinned SHA-256. `compose.yaml` builds the `keycloak` service from it instead of pulling the image. No binary is committed; the first `docker compose up` builds the image once.

**Mail.** A `mailpit` service (`axllent/mailpit`, pinned) joins compose: SMTP on `1025` inside the compose network, its inbox at `http://localhost:8025`, bound to `127.0.0.1`. The realm's `smtpServer` points at `mailpit:1025` with a `noreply@restaurant.local` sender. Mailpit catches every message; nothing leaves the machine.

**Realm file (`keycloak/realm-restaurant.json`).**

- **Login flow `browser-email-otp`,** set as the realm's `browserFlow`: Cookie (alternative), then a sub-flow of Username Form → the plugin's email-code authenticator, both required. Swagger UI's login goes through it like any browser. The plugin's settings on that execution: 6-digit code, 600 s lifetime, 60 s resend cooldown, at most 5 codes per user per 900 s, trusted as the sole authenticator.
- **Registration:** `registrationAllowed` and `registrationEmailAsUsername` on; the registration flow keeps the user-profile step and drops the password-validation step, so the form asks only for email, first name and last name. **Email verification is on** (`verifyEmail`): Keycloak logs a new user in straight after the form, without the login flow, so the code never proves the address at sign-up; the verification link (to Mailpit) does.
- **`default-roles-restaurant` gains `CUSTOMER`.** Default roles apply to every user created in the realm, including through the admin console; an admin creating a restaurant owner removes `CUSTOMER` from that user, or the owner would become a customer on first login (see *Creating the account*).
- **The seeded users lose their `credentials`.** Their fixed ids, emails and roles stay. A developer logs in as a seeded user by entering its email and reading the code in Mailpit.

The exact provider id and configuration keys of the plugin are read from its JAR while planning; its README names the settings by their console labels only.

**Applying it locally:** the realm is imported only when it does not exist, so the existing recipe in `CLAUDE.md` › Authentication (drop and recreate the `keycloak` schema, restart Keycloak) applies; `docker compose up -d --build keycloak mailpit` picks up the new image.

## Creating the account on the first request

**`CallerAuthenticationConverter`**, after verification:

1. `userService.findCaller(sub)` finds a row → as today.
2. No row, the token's permissions include `customer.write`, and `email_verified` is `true` → `customerService.registerFromToken(keycloakId, email, name)`, which returns the new `Caller`.
3. No row, and no `customer.write` → `InvalidBearerTokenException("Unknown user")`, 401, as today.

The permission, not the role name, decides (ADR 0008). It reuses the authorities the converter already computes with `KeycloakPermissionsConverter`.

**Claims.** `email` is required; a token without one is a 401, and so is a token whose `email_verified` is not `true`, so an unproven address never gets a row. The name is the `name` claim, else the email. Keycloak puts both in the access token through its default `email` and `profile` scopes.

**`CustomerService.registerFromToken(UUID keycloakId, String email, String name)`**, `@Transactional` because it runs from the security filter, outside any use-case (like `UserService.findCaller`):

- lower-cases and trims the email, as `createCustomer` does today;
- if an active user already holds the email → the existing `EmailAlreadyInUseException`, which the converter catches and turns into 401 `InvalidBearerTokenException("Unknown user")`. Keycloak keeps emails unique among its users, so this only happens with rows created by the old `POST /customers` on an existing local database. They are not linked automatically.
- builds the `User` (name, email, `userKeycloakId`) and the `Customer` with their builders, saves both through `UserService.create` and `CustomerRepository`, and returns `new Caller(userId, customerId)`.

It lives in the customer domain because `CustomerService` already depends on `UserService`; putting it in `UserService` would close a cycle.

**Two first requests at once.** A page that fires two calls with a fresh token can race: the second insert violates `uq_users_keycloak_id`. The converter catches `DataIntegrityViolationException` from `registerFromToken` and calls `findCaller(sub)` once more; if that is still empty, it rethrows. The insert's transaction has rolled back by then, so the retry reads the winner's committed rows.

**Soft-deleted users.** `findCaller` still resolves soft-deleted rows (part 2), so a deleted customer's token resolves to their old row and gets today's 404s; it is never re-registered. A soft-deleted row's email is free again (`uq_users_active_email` is partial), so a *different* Keycloak user with that email can register.

## Removed and changed

**Removed:**

- `POST /api/v1/customers`, `CustomerController.createCustomer`, `CustomerService.createCustomer`, `CreateCustomerRequest`, and its public entry in `SecurityConfig`. Public paths become health, Swagger and `/error`.
- `PUT /api/v1/customers/me/password`, `ChangePasswordRequest`, `CustomerService.changePassword` and `ensureCurrentPasswordMatches`, `IncorrectPasswordException`.
- `PasswordEncoderConfig`, `MaxUtf8Bytes` and `MaxUtf8BytesValidator`; `spring-security-crypto` stays, since Spring Security brings it.
- `User.userPassword`.
- Whatever becomes unused once those go and `updateCustomer` loses its email handling (`ensureEmailAvailable`, `UserService.isEmailTakenByOther`); `UserService.isEmailTaken` and `EmailAlreadyInUseException` stay for `registerFromToken`. The plan confirms each.

**Changed:**

- `UpdateCustomerRequest` loses `email`; `CustomerService.updateCustomer` loses its email branch.
- **`V17__drop_user_password.sql`** drops `users.user_password`, and V15's comment about accounts without a Keycloak user is answered in V17's header (V15 itself is never edited).

## Testing

All over HTTP with signed tokens; no test talks to Keycloak or Mailpit.

**`JwtTestSupport`** gains a way to sign a token with `email` and `name` claims and the permissions of a realm role, for a `sub` with no row.

**New `customer/FirstLoginEndpointTest`**, each case on `GET /api/v1/customers/me` with a fresh UUID and email, cleaning up only the rows it created:

- `CUSTOMER` permissions + `email` + `name` → 200 with that name and email; SQL finds one `users` row linked to the `sub` and one `customers` row;
- the same token twice → the same ids, one row each;
- a token without `name` → 200, the name is the email;
- a 151-character name → stored cut to 150 (`users.user_name` is `VARCHAR(150)`; Keycloak allows longer);
- no `customer.write` → 401, no rows;
- no `email` claim → 401, no rows;
- `email_verified` false → 401, no rows;
- the email held by an active row with no Keycloak id → 401; that row is unchanged and nothing is inserted;
- the email held only by a soft-deleted row → 200, a new row.

**The race** cannot be triggered over HTTP reliably, so a mocked unit test of `CallerAuthenticationConverter` covers it: `registerFromToken` throws `DataIntegrityViolationException`, the second `findCaller` returns the winner, the token carries it; and when the second lookup is also empty, the exception propagates.

**Deleted:** `CreateCustomerEndpointTest`, `ChangePasswordEndpointTest`, and their rows in `EndpointAuthorizationTest`. The `POST /customers` public row there becomes a check that the endpoint is gone (whatever Spring returns for a path with no matching method, pinned).

**Changed:** `UpdateCustomerEndpointTest` loses its email cases and gains one that sends `email` and checks the stored email is unchanged, pinning whether Jackson ignores the field or rejects it. `CustomerEndpointTestSupport` and every helper or test that writes `user_password` stop doing so. `CallerResolutionEndpointTest`'s unknown-`sub` case keeps a permission-less token, so it still proves 401.

**Manual check, in the PR description**, after `docker compose up -d --build`:

1. Swagger UI → Authorize → *Register*, a new email, first and last name; follow the verification link in Mailpit; read the code at `http://localhost:8025`, enter it; `GET /customers/me` → 200 with that email.
2. Log out; log in as `ahmed.ali@example.com` with a code from Mailpit; `GET /customers/me` → customer 1.
3. In the Keycloak console, create a user, remove `CUSTOMER` from it, log in as it; any protected endpoint → 401.
4. Ask for a sixth code within 15 minutes → refused.

## Documentation

- ADR `0010-email-code-login-and-keycloak-sign-up.md`: the plugin over passwords and over our own plugin; Keycloak-hosted sign-up with rows on first request over the admin API.
- Use-cases: `customer-management/create-customer/` becomes sign-up (Keycloak's page, then the first request); `update-customer` loses email; any change-password section goes.
- `CLAUDE.md` › Authentication: seeded users log in with their email and a code from Mailpit (`http://localhost:8025`); public paths are health, Swagger and `/error`; an unknown `sub` holding `customer.write` becomes a customer on its first request; the realm-reset recipe also needs `--build` once; `admin@example.com` logs in by code too.
- The roadmap: part 3's row and its decisions; the stale "PR not opened yet" cells become #116, #121 and #124; part 3's open questions move to decided.

## Delivery

Branch `feat/GH-120-email-otp-signup`, from `feat/GH-119-authenticated-caller`; one PR based on it, closing #120. It is the last part: once #116, #121, #124 and this PR are merged, #83 can be closed. Commits, each green: design → Keycloak side (Dockerfile, Mailpit, realm) → first-request registration and its tests → removals and V17 → docs and ADR → formatting.

## Rejected alternatives

- **Keycloak passwords.** Built in and no plugin, but the issue asked for passwordless login.
- **`mesutpiskin/keycloak-2fa-email-authenticator`.** Documented only as a second factor after the password; no build found for 26.5.
- **Our own Keycloak authenticator plugin.** A second Java module and a custom image to maintain, for what the community plugin already does.
- **Sign-up through `POST /customers`, the API creating the Keycloak user via the admin API.** The API would hold a privileged Keycloak credential and a new dependency, tests would need a fake Keycloak, and a Keycloak user created before our insert fails needs compensation.
- **Every unknown `sub` becomes a customer.** A restaurant owner created in Keycloak would gain a customer row on first login.
- **Link an unlinked row by email on first login.** It would hand an old account to whoever controls that email in Keycloak; the case exists only on old local databases.
- **Keep `email` in `PATCH /customers/me`.** Our copy and Keycloak's login email would drift apart.
- **Commit the plugin JAR and mount it.** No build step, but a binary in the repository.

## Out of scope

- **Deleting a customer** still only soft-deletes our row. The Keycloak user stays: they can log in and meet 404s, and their email cannot sign up again. Fixing it needs the admin API.
- Restaurant-owner sign-up, and linking an owner's Keycloak user to their row other than by seeding.
- Phone codes; passkeys.
- Copying profile edits back to Keycloak.
- Production mail delivery.
