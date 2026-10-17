-- The realm's admin@example.com (#83 part 2), so its token resolves to a caller. No customer
-- or restaurant points at it: it owns nothing, and ownership guards refuse it like anyone else.
-- No explicit user_id: the identity column assigns one, so nothing needs resyncing.
INSERT INTO users (user_name, user_email, user_password, user_keycloak_id) VALUES
    ('Administrator', 'admin@example.com', '$2a$10$placeholder.admin',
     '00000000-0000-4000-a000-000000000001');
