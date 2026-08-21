# Restaurant App

Spring Boot backend for the restaurant app. The project uses a DDD package layout, Docker for local infrastructure, Swagger/OpenAPI for API docs, and H2 for test isolation.

## Stack

- Java 17
- Spring Boot 4.1.0
- PostgreSQL 16
- H2 for tests
- Docker Compose
- SpringDoc OpenAPI UI

## Project Layout

- [`Dockerfile`](./Dockerfile): multi-stage image build for the app
- [`compose.yaml`](./compose.yaml): starts the app and PostgreSQL together
- [`src/main/resources/application.properties`](./src/main/resources/application.properties): runtime datasource and JPA config
- [`src/test/resources/application.properties`](./src/test/resources/application.properties): test-only H2 configuration
- [`src/main/java/com/mentorship/restaurant/cart/controller`](./src/main/java/com/mentorship/restaurant/cart/controller): REST controller layer
- [`src/main/java/com/mentorship/restaurant/cart/service`](./src/main/java/com/mentorship/restaurant/cart/service): cart service and command/query handlers
- [`src/main/java/com/mentorship/restaurant/cart/model`](./src/main/java/com/mentorship/restaurant/cart/model): entities, requests, responses, and mappers
- [`src/main/java/com/mentorship/restaurant/cart/repository`](./src/main/java/com/mentorship/restaurant/cart/repository): Spring Data repositories
- [`src/main/java/com/mentorship/restaurant/config`](./src/main/java/com/mentorship/restaurant/config): application security configuration

## Current Setup

- The app is configured to connect to PostgreSQL on `localhost:55433` by default.
- Docker Compose starts `restaurant-app` and `restaurant-postgres` together.
- Swagger UI is exposed at `/swagger-ui.html`.
- OpenAPI JSON is exposed at `/v3/api-docs`.
- Tests run against an in-memory H2 database so `mvn clean install` does not require a live PostgreSQL server.

## Requirements

Install these before running locally:

- Java 17
- Docker
- Docker Compose v2

Check your tools:

```bash
java -version
docker --version
docker compose version
```

## Build

Run the full build:

```bash
./mvnw clean install
```

What this does:

- compiles the application
- runs the Spring Boot test context against H2
- packages the application artifact

## Run with Docker

From the `restaurant` directory:

```bash
docker compose up --build
```

What this does:

- builds the app image with the `Dockerfile`
- pulls `postgres:16`
- starts PostgreSQL
- waits for PostgreSQL to become healthy
- starts the Spring Boot app

After startup:

- App URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Database host: `localhost:55433`
- Database name: `restaurant`
- Database user: `postgres`
- Database password: `postgres`

Useful commands:

```bash
docker compose ps
docker compose logs -f
docker compose logs -f postgres
docker compose logs -f app
```

Stop the stack:

```bash
docker compose down
```

Stop the stack and delete the database volume:

```bash
docker compose down -v
```

## Run Locally

If you want to run the app outside Docker:

1. Start PostgreSQL 16 locally.
2. Create a database named `restaurant`.
3. Set the datasource environment variables.
4. Start the app with Maven.

Example environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/restaurant
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
```

Start the app:

```bash
./mvnw spring-boot:run
```

## Database

The app currently uses JPA entities under `cart/model/entity` and Spring Data repositories under `cart/repository`.

Flyway is included in the build. If migration scripts are present under `src/main/resources/db/migration`, they will run on startup.

## Security

Spring Security is enabled.

The current security config permits the Swagger endpoints and application routes, so the app is effectively open during development.

## Troubleshooting

If Docker Compose fails to start:

- make sure port `8080` is free
- make sure port `55433` is free
- confirm Docker is running
- check `docker compose logs -f postgres`
- check `docker compose logs -f app`

If the app cannot connect to PostgreSQL:

- verify `restaurant-postgres` is healthy with `docker compose ps`
- verify `SPRING_DATASOURCE_URL` points to the correct host and port
- delete the volume and restart with `docker compose down -v` and `docker compose up --build`
