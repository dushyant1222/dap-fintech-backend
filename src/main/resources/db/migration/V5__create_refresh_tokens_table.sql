CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID,
    refresh_token TEXT UNIQUE NOT NULL,
    expiry_date TIMESTAMP,
    is_revoked BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_refresh_user
    FOREIGN KEY(user_id)
    REFERENCES users(id)
);