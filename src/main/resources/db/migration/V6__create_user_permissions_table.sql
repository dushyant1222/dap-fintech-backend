CREATE TABLE user_permissions
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    permission_id UUID NOT NULL,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_user_permission_user
        FOREIGN KEY(user_id)
            REFERENCES users(id),

    CONSTRAINT fk_user_permission_permission
        FOREIGN KEY(permission_id)
            REFERENCES permissions(id)
);