# Restaurant App

Spring Boot backend for a restaurant ordering system, with PostgreSQL as the runtime database and H2 only for tests.

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
- [`restaurant/src/main/java/com/mentorship/restaurant/RestaurantApplication.java`](./restaurant/src/main/java/com/mentorship/restaurant/RestaurantApplication.java): application entry point
- [`restaurant/src/main/java/com/mentorship/restaurant/config/SecurityConfig.java`](./restaurant/src/main/java/com/mentorship/restaurant/config/SecurityConfig.java): security configuration
- [`restaurant/src/main/java/com/mentorship/restaurant/health/HealthController.java`](./restaurant/src/main/java/com/mentorship/restaurant/health/HealthController.java): global health endpoint
- [`restaurant/src/main/java/com/mentorship/restaurant/exception/GlobalExceptionHandler.java`](./restaurant/src/main/java/com/mentorship/restaurant/exception/GlobalExceptionHandler.java): global API error handling
- [`restaurant/src/main/java/com/mentorship/restaurant/exception/ApiErrorResponse.java`](./restaurant/src/main/java/com/mentorship/restaurant/exception/ApiErrorResponse.java): shared error payload

## Current Setup

- The app uses PostgreSQL for runtime.
- Local and IntelliJ runs use PostgreSQL on `localhost:55433`.
- Docker runs use PostgreSQL on the `postgres` service.
- JSON payloads use snake_case globally.
- Swagger UI is exposed at `/swagger-ui.html`.
- OpenAPI JSON is exposed at `/v3/api-docs`.

## Error Handling

Global API errors return a structured response from `com.mentorship.restaurant.exception`.

Typical statuses:

- `400 BAD_REQUEST` for validation errors
- `404 NOT_FOUND` for missing resources
- `409 CONFLICT` for stock violations
- `500 INTERNAL_SERVER_ERROR` for unexpected errors

## Database

Flyway migrations live under:

- [`restaurant/src/main/resources/db/migration`](./restaurant/src/main/resources/db/migration)

Current migrations:

- Core schema migration
- Reference data seed migration
- Sample data seed migration
- Stock migration
- Note migration

Seeded data includes the core application reference records used by the API.

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
