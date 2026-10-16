-- Keycloak owns credentials and login is an emailed code (#83 part 3, ADR 0010): the API stores
-- no password. Answers V15's note: every account is now created from a Keycloak token, so new
-- rows always carry user_keycloak_id; only rows created by the old POST /customers lack one.
ALTER TABLE users DROP COLUMN user_password;
