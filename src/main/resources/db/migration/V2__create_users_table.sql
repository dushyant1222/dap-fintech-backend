CREATE TABLE users(
id UUID PRIMARY KEY,
full_name VARCHAR(255) NOT NULL,
mobile_number VARCHAR(20) UNIQUE NOT NULL,
password_hash TEXT NOT NULL,
role_id UUID,
status VARCHAR(50),
created_at TIMESTAMP,
updated_at TIMESTAMP,

CONSTRAINT fk_user_role
FOREIGN KEY(role_id)
REFERENCES roles(id)
);