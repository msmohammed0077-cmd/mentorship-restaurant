# Restaurant App

Spring Boot backend for the restaurant domain with PostgreSQL and a Docker-based development setup.

## Tech Stack

| Component | Version | Notes |
| --- | --- | --- |
| Java | 17 | Project language level and runtime target |
| Spring Boot | 4.1.0 | Parent POM version |
| PostgreSQL | 16 | Docker Compose database container |
| Maven Wrapper | 3.3.4 | Wrapper version in `.mvn/wrapper/maven-wrapper.properties` |
| Maven | 3.9.16 | Wrapper downloads this distribution for local builds |
| Docker build Maven image | 3.9.11-eclipse-temurin-17 | Used in the multi-stage `Dockerfile` |
| Docker runtime image | Eclipse Temurin 17 JRE Jammy | Used in the final app image |

## What Runs

The Compose stack starts two containers:

- `restaurant-app`
- `restaurant-postgres`

The app listens on port `8080`.
The database is published on host port `55433`.

## Project Layout

- [`Dockerfile`](Dockerfile): multi-stage image build for the app
- [`compose.yaml`](compose.yaml): starts the app and PostgreSQL together
- [`src/main/resources/application.properties`](srcain/resources/application.properties): datasource and JPA config
- [`src/main/resources/db/migration/V1__create_core_tables.sql`](src/main/resources/db/migration/V1__create_core_tables.sql): SQL schema file
- [`src/main/java/com/mentorship/restaurant/persistence/entity`](src/main/java/com/mentorship/projects/persistence/entity): JPA entities

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

Example `psql` setup:

```bash
psql -U postgres
```

Then inside `psql`:

```sql
CREATE DATABASE restaurant;
\q
```

If PostgreSQL uses a different user, password, or port on your machine, use those values instead.

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

What this does:

- starts the Spring Boot app on `http://localhost:8080`
- connects to your local PostgreSQL instance
- creates the tables with Hibernate schema generation

Build a jar:

```bash
./mvnw clean package -DskipTests
```

## Database Schema

The app creates these tables on startup:

- `users`
- `customers`
- `restaurants`
- `menus`
- `menu_items`
- `carts`
- `cart_items`

The database is created by Hibernate schema generation in the current setup.

## Verify the Database

Check that Docker containers are running:

```bash
docker ps
```

Connect to PostgreSQL:

```bash
docker exec -it restaurant-postgres psql -U postgres -d restaurant
```

List tables inside `psql`:

```sql
\dt
```

List databases:

```sql
\l
```

Exit `psql`:

```sql
\q
```

## Authentication

Spring Security is enabled.

When the app starts, Spring Boot prints a generated development password to the container logs.

To view it:

```bash
docker logs restaurant-app
```

Look for the line that starts with `Using generated security password:`.

## Troubleshooting

If Docker Compose fails to start:

- make sure port `8080` is free
- make sure port `55433` is free
- confirm Docker is running
- check `docker compose logs -f postgres`
- check `docker compose logs -f app`

If the app cannot connect to PostgreSQL:

- verify `restaurant-postgres` is healthy with `docker compose ps`
- verify `SPRING_DATASOURCE_URL` points to `jdbc:postgresql://postgres:5432/restaurant`
- delete the volume and restart with `docker compose down -v` and `docker compose up --build`

If the app starts but you get a security response:

- use the generated password from the app logs
- or add a custom Spring Security configuration later
