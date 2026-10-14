-- Links a login account to its Keycloak user: the access token's sub claim (#83).
-- Nullable: accounts created through POST /customers have no Keycloak user until
-- sign-up moves to Keycloak (#83 part 3).
ALTER TABLE users ADD COLUMN user_keycloak_id UUID;

ALTER TABLE users ADD CONSTRAINT uq_users_keycloak_id UNIQUE (user_keycloak_id);

-- The seeded users exist in keycloak/realm-restaurant.json under these ids.
UPDATE users SET user_keycloak_id = '00000000-0000-4000-8000-000000000001' WHERE user_id = 1;
UPDATE users SET user_keycloak_id = '00000000-0000-4000-8000-000000000002' WHERE user_id = 2;
UPDATE users SET user_keycloak_id = '00000000-0000-4000-8000-000000000003' WHERE user_id = 3;
UPDATE users SET user_keycloak_id = '00000000-0000-4000-8000-000000000004' WHERE user_id = 4;
UPDATE users SET user_keycloak_id = '00000000-0000-4000-8000-000000000005' WHERE user_id = 5;
UPDATE users SET user_keycloak_id = '00000000-0000-4000-8000-000000000006' WHERE user_id = 6;
