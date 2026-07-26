CREATE TABLE permissions(
id UUID PRIMARY KEY,
permission_key VARCHAR(150) UNIQUE NOT NULL,
module_name VARCHAR(100),
description TEXT,
created_at TIMESTAMP,
updated_at TIMESTAMP
);