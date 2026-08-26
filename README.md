# Restaurant App

Spring Boot backend for a restaurant cart workflow. The current scope is the modify-cart API, with PostgreSQL as the runtime database and H2 only for tests.

## Stack

- Java 17
- Spring Boot 4.1.0
- PostgreSQL runtime database
- H2 in-memory test database
- Flyway
- Spring Security
- SpringDoc OpenAPI UI

## Project Layout

- [`restaurant/Dockerfile`](./restaurant/Dockerfile): multi-stage build image
- [`restaurant/compose.yaml`](./restaurant/compose.yaml): runs the app with PostgreSQL
- [`restaurant/src/main/resources/application.properties`](./restaurant/src/main/resources/application.properties): shared app config
- [`restaurant/src/main/resources/application-local.properties`](./restaurant/src/main/resources/application-local.properties): local PostgreSQL database
- [`restaurant/src/main/resources/application-docker.properties`](./restaurant/src/main/resources/application-docker.properties): Docker PostgreSQL database
- [`restaurant/src/main/java/com/mentorship/restaurant/cart/controller`](./restaurant/src/main/java/com/mentorship/restaurant/cart/controller): cart REST controller
- [`restaurant/src/main/java/com/mentorship/restaurant/cart/service`](./restaurant/src/main/java/com/mentorship/restaurant/cart/service): cart service
- [`restaurant/src/main/java/com/mentorship/restaurant/cart/service/handler`](./restaurant/src/main/java/com/mentorship/restaurant/cart/service/handler): cart command handlers
- [`restaurant/src/main/java/com/mentorship/restaurant/cart/model`](./restaurant/src/main/java/com/mentorship/restaurant/cart/model): entities, requests, responses, and mappers
- [`restaurant/src/main/java/com/mentorship/restaurant/exception`](./restaurant/src/main/java/com/mentorship/restaurant/exception): global API error handling

## Current Setup

- The app uses PostgreSQL for runtime.
- Local and IntelliJ runs use PostgreSQL on `localhost:55433`.
- Docker runs use PostgreSQL on the `postgres` service.
- JSON payloads use snake_case globally.
- Swagger UI is exposed at `/swagger-ui.html`.
- OpenAPI JSON is exposed at `/v3/api-docs`.

## API

The current API scope is modify-cart:

- `PUT /api/v1/cart/{cartId}/items/{cartItemId}`

Request body:

```json
{
  "quantity": 2,
  "note": "No onions"
}
```

Rules:

- quantity must be positive
- quantity cannot exceed menu item stock
- note is persisted on the cart item

Response fields are returned in snake_case, for example:

```json
{
  "cart_id": 1,
  "customer_id": 1,
  "items": [],
  "total": 185.00
}
```

## Error Handling

Global API errors return a structured response from `com.mentorship.restaurant.exception`.

Typical statuses:

- `400 BAD_REQUEST` for validation errors
- `404 NOT_FOUND` for missing cart items
- `409 CONFLICT` for stock violations
- `500 INTERNAL_SERVER_ERROR` for unexpected errors

## Database

Flyway migrations live under:

- [`restaurant/src/main/resources/db/migration`](./restaurant/src/main/resources/db/migration)

Current migrations:

- `V1__create_core_tables.sql`
- `V2__seed_reference_data.sql`
- `V3__seed_cart_data.sql`
- `V4__add_menu_item_stock.sql`
- `V5__add_cart_item_note.sql`

Seeded data includes users, customers, restaurants, menus, menu items, carts, and cart items.

## Requirements

- Java 17
- Docker

Check your tools:

```bash
java -version
docker --version
docker compose version
```

## Run Locally

Start the app from the `restaurant` directory:

```bash
./mvnw spring-boot:run
```

This uses PostgreSQL on `localhost:55433` by default.

Expected local credentials:

- Database: `restaurant`
- User: `postgres`
- Password: `postgres`

## Run with Docker

From the `restaurant` directory:

```bash
docker compose up --build
```

This:

- builds the app image with the multi-stage `Dockerfile`
- starts the `postgres` and `app` services together
- exposes PostgreSQL on `localhost:55433`
- starts the app against the Docker PostgreSQL service

After startup:

- App URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Database host: `localhost:55433`
- Database name: `restaurant`
- Database user: `postgres`
- Database password: `postgres`

## Security

Spring Security is enabled, but the current development configuration permits:

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`

## Troubleshooting

If PostgreSQL does not start:

- make sure port `55433` is free
- confirm Docker is running
- check `docker compose logs -f postgres`

If the app cannot connect to PostgreSQL:

- verify the `postgres` container is healthy
- verify the local datasource URL is correct
- check `docker compose logs -f app`

If Docker fails:

- confirm `8080` is free
- confirm the `restaurant/data` directory is writable
- check `docker compose logs -f`
