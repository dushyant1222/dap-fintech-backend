CREATE TABLE customers(
    id UUID PRIMARY KEY,

    customer_code VARCHAR(50) UNIQUE,

    first_name VARCHAR(100),

    last_name VARCHAR(100),

    mobile_number VARCHAR(20),

    alternate_mobile_number VARCHAR(20),

    email VARCHAR(150),

    date_of_birth DATE,

    gender VARCHAR(20),

    aadhaar_number VARCHAR(20),

    pan_number VARCHAR(20),

    occupation VARCHAR(100),

    monthly_income DECIMAL(18,2),

    status VARCHAR(50),

    created_at TIMESTAMP,

    updated_at TIMESTAMP
);