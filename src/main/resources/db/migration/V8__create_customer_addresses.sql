CREATE TABLE customer_addresses(
    id UUID PRIMARY KEY,

    customer_id UUID,

    address_type VARCHAR(50),

    address_line_1 VARCHAR(255),

    address_line_2 VARCHAR(255),

    city VARCHAR(100),

    state VARCHAR(100),

    pincode VARCHAR(20),

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_customer_address_customer
        FOREIGN KEY(customer_id)
        REFERENCES customers(id)
);