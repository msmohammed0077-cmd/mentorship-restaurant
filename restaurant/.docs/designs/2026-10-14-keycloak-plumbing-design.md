# #83 part 1 — Keycloak plumbing

Part 1 of `2026-10-14-auth-roadmap.md`. It runs Keycloak locally, makes the API validate Keycloak's JWTs, links Keycloak users to our `users` rows and lets tests sign their own tokens. It **requires** nothing yet: a request without a token behaves exactly as today. Part 4 starts requiring tokens and roles.

## Local setup

### Compose (`restaurant/compose.yaml`)

- **`postgres`** (existing) mounts `./docker/postgres/init` at `/docker-entrypoint-initdb.d` (read-only). `01-keycloak.sql` creates the login role `keycloak` (development password `keycloak`) and `CREATE SCHEMA keycloak AUTHORIZATION keycloak`. Keycloak's `db-schema` option selects a schema but does not create it, so it must exist first. Keycloak connects as that role, so it cannot touch our tables; Flyway keeps managing `public`.
- **`keycloak`** (new, no profile, so `docker compose up` starts it):
  - image `quay.io/keycloak/keycloak:26.5.2`, command `start-dev --import-realm`;
  - `KC_DB=postgres`, `KC_DB_URL=jdbc:postgresql://postgres:5432/restaurant`, `KC_DB_SCHEMA=keycloak`, user and password `keycloak`;
  - `KC_BOOTSTRAP_ADMIN_USERNAME` / `KC_BOOTSTRAP_ADMIN_PASSWORD` `admin` / `admin` (development only);
  - `KC_HOSTNAME=http://localhost:8180`, port `8180:8080`, so every token's issuer is `http://localhost:8180/realms/restaurant`, whoever requested it;
  - `depends_on` postgres healthy, and a healthcheck so `docker compose up --wait` works;
  - `./keycloak/realm-restaurant.json` mounted into `/opt/keycloak/data/import`.

**Init scripts run only on an empty volume.** An existing checkout needs `docker compose down -v` once.

**`--import-realm` skips a realm that already exists.** Keycloak keeps the realm in Postgres, so an edit to the realm file takes effect only after `docker compose down -v`. `CLAUDE.md` gets this note.

### The realm (`restaurant/keycloak/realm-restaurant.json`)

- Realm `restaurant`.
- Client **`restaurant-api`**: the audience the API checks. No login flow of its own.
- Client **`restaurant-swagger`**: public, authorization code with PKCE, redirect URI `http://localhost:8080/swagger-ui/oauth2-redirect.html`, web origin `http://localhost:8080`. An audience mapper adds `restaurant-api` to the `aud` claim of its access tokens.
- **Users:** one per seeded `users` row (ids 1–6: customers 1, 2, 5 and restaurants 3, 4, 6), each with a fixed UUID, the seeded email (as username and email, marked verified), first name from the seeded name, and password `password`. The passwords go in part 3.
- No roles (part 4).

## The API as a resource server

**Dependency:** `spring-boot-starter-oauth2-resource-server`. Reason, recorded in the pom: it validates Keycloak-issued JWTs, and it brings `spring-security-oauth2-jose`, whose `NimbusJwtEncoder` the tests use to sign tokens.

**Configuration:**

```properties
# application.properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/restaurant
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/restaurant/protocol/openid-connect/certs
spring.security.oauth2.resourceserver.jwt.audiences=restaurant-api

# application-docker.properties (the app inside compose cannot reach localhost:8180)
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://keycloak:8080/realms/restaurant/protocol/openid-connect/certs
```

- With `jwk-set-uri` set, Spring Boot does not contact Keycloak at startup; it fetches the keys when the first token arrives. **The application starts even when Keycloak is down.**
- `issuer-uri` and `audiences` still add their validators: `iss` must be `http://localhost:8180/realms/restaurant` and `aud` must contain `restaurant-api`.
- Inside compose only the key URL changes. The issuer stays `localhost:8180`, which is what the browser's token carries, so the two cannot disagree.

**`SecurityConfig`:**

- adds `.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))`;
- removes `.httpBasic(...)`;
- sets `SessionCreationPolicy.STATELESS` explicitly; CSRF stays disabled;
- keeps `anyRequest().permitAll()`, with a comment that part 4 tightens it.

**Behaviour:**

| Request | Result |
|---|---|
| No `Authorization` header | As today |
| Valid bearer token | Authenticated; the principal's name is `sub` |
| Malformed, expired, wrongly signed, wrong issuer or wrong audience | 401, `WWW-Authenticate: Bearer error="invalid_token"` (Spring's response) |

A bad token is rejected even on a `permitAll` endpoint: the bearer-token filter runs before authorization.

**Swagger:** an OAuth2 security scheme with the authorization code flow and Keycloak's authorization and token URLs; `springdoc.swagger-ui.oauth.client-id=restaurant-swagger` and `springdoc.swagger-ui.oauth.use-pkce-with-authorization-code-grant=true`. Swagger UI shows **Authorize**, logs in through Keycloak's page and sends the token on every call.

## Linking users

Migration `V15__add_user_keycloak_id.sql` (check `main` for the number before merging):

- `ALTER TABLE users ADD COLUMN user_keycloak_id UUID`, with a unique index. Nullable: rows created through `POST /customers` have no Keycloak account until part 3.
- `UPDATE users SET user_keycloak_id = '<uuid>' WHERE user_id = <n>` for ids 1–6, the same UUIDs as the realm file.

`User` gains `private UUID userKeycloakId;` mapped to `user_keycloak_id`. No repository lookup yet: part 2 adds `findByKeycloakId` when a caller needs it.

## Testing

**Key pair.** `src/test/resources/jwt/test-private.pem` and `test-public.pem` are committed; a comment beside them says they are test-only and sign nothing real.

**A test `JwtDecoder` replaces Spring Boot's.** Tests run with the `local` and `test` profiles together, so the `jwk-set-uri` from `application.properties` reaches them, and a property cannot unset it: Boot's condition treats an empty value as set. Instead, a test configuration imported by the endpoint-test support classes declares a `JwtDecoder` bean, and Boot's own backs off (it is `@ConditionalOnMissingBean`). The bean is `NimbusJwtDecoder.withPublicKey(test public key)` with the same validators Boot adds: the default timestamp check, the issuer from `issuer-uri` and the audience from `audiences`, read from the same properties, so the values have one source. Nothing in the test context ever contacts Keycloak.

**`support/JwtTestSupport`** signs with the test private key through `NimbusJwtEncoder`:

- `tokenFor(UUID keycloakId)`: a valid, Keycloak-shaped token (`iss`, `aud=restaurant-api`, `sub`, five-minute expiry);
- a builder for the broken variants: expired, wrong issuer, wrong audience, signed with a throwaway key.

Part 4 adds `realm_access.roles`.

**`BearerTokenEndpointTest`** (`RANDOM_PORT` + `RestTestClient`, against `GET /api/v1/health`, which needs no seeded data):

| Case | Expected |
|---|---|
| No `Authorization` header | 200 |
| Valid token | 200 |
| Expired | 401 |
| Signed with another key | 401 |
| Wrong issuer | 401 |
| Wrong audience | 401 |
| `Bearer not-a-jwt` | 401 |

Every 401 also asserts that `WWW-Authenticate` starts with `Bearer`. The 401s are what prove validation runs: `permitAll` would otherwise let them through.

**Unchanged:** every existing test, since nothing requires a token. CI runs neither Keycloak nor the init script; the tests do not need the `keycloak` schema.

**Manual check, in the PR description:** `docker compose down -v && docker compose up --wait`, open Swagger UI, **Authorize** as `ahmed.ali@example.com` / `password`, and call `/api/v1/health` with the token.

## Documentation

- ADR `0007-keycloak-as-identity-provider.md`, committed with this design.
- The implementation PR updates `CLAUDE.md`: the Keycloak service and port, the one-time `docker compose down -v`, realm edits needing a reset, and `JwtTestSupport` for tests that send tokens.

## Rejected alternatives

- **Opaque-token introspection.** Revocation takes effect at once, but every request calls Keycloak, so Keycloak being slow or down takes the API down with it. It also needs a confidential client and secret, and tests would need a fake introspection endpoint. If revocation ever matters: short-lived tokens, and a deny-list if needed.
- **Keycloak's Spring adapter.** Deprecated and removed; Keycloak points to Spring Security's resource server.
- **Keycloak's policy enforcer (Authorization Services).** Moves authorization rules out of our code; we need roles, not a policy engine.
- **`spring-addons`.** Would save the role converter in part 4 at the cost of a dependency.
- **Keycloak's own development storage, or a database or container of its own.** The user chose a `keycloak` schema in our database: its state survives restarts, without another container.
- **A Flyway migration creating the `keycloak` schema.** It would put local Keycloak infrastructure into every environment's schema history, CI and tests included, and a migration cannot sensibly create login roles.
- **Testcontainers Keycloak in tests.** Faithful, but a dependency and 10–20 seconds per run; signed test tokens cover the API's side. Revisit if the claim shape ever drifts unnoticed.
- **The password grant for getting tokens by hand.** Dropped from OAuth 2.1, and meaningless once login is passwordless; Swagger's PKCE flow keeps working through part 3.

## Out of scope

- Roles, the `realm_access` → `ROLE_*` converter, `@PreAuthorize`, requiring a token (part 4).
- Replacing the `customerId`, `restaurantId` and `role` parameters; `findByKeycloakId` (part 2).
- Registration, OTP, removing `user_password` and the change-password endpoint (part 3).
- Production Keycloak: deployment, TLS, secrets. No production environment exists yet.
