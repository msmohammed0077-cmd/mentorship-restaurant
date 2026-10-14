-- Keycloak keeps its tables in their own schema, under a role that cannot touch
-- the application's tables (#83). Postgres runs this only on an empty volume. On
-- an existing one, apply it once without losing data:
--   docker exec -i restaurant-postgres psql -U postgres -d restaurant < docker/postgres/init/01-keycloak.sql
CREATE ROLE keycloak LOGIN PASSWORD 'keycloak';
CREATE SCHEMA keycloak AUTHORIZATION keycloak;
