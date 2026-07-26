CREATE TABLE roles(
id UUID PRIMARY KEY,
role_name VARCHAR(100) UNIQUE NOT NULL,
    role_description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP

);