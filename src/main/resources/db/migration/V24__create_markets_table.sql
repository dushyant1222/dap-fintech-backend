CREATE TABLE markets (

    id UUID PRIMARY KEY,

    market_code VARCHAR(50) UNIQUE,

    market_name VARCHAR(255) NOT NULL,

    city VARCHAR(100),

    state VARCHAR(100),

    description TEXT,

    status VARCHAR(50),

    created_at TIMESTAMP,

    updated_at TIMESTAMP
);