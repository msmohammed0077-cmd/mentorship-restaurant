-- Profile fields live on users, beside name/email/password; customers stays a
-- thin link table like restaurants. All nullable: a customer can sign up
-- without them.
ALTER TABLE users
    ADD COLUMN user_phone VARCHAR(20),
    ADD COLUMN user_date_of_birth DATE,
    ADD COLUMN user_gender VARCHAR(20),
    ADD COLUMN user_deleted_at TIMESTAMPTZ;

-- The plain UNIQUE from V1 was case-sensitive and counted soft-deleted rows.
-- Emails are compared case-insensitively, and a soft-deleted user's email
-- must be reusable, so uniqueness applies to lower(email) among active users.
ALTER TABLE users DROP CONSTRAINT users_user_email_key;

CREATE UNIQUE INDEX uq_users_active_email
    ON users (lower(user_email))
    WHERE user_deleted_at IS NULL;
