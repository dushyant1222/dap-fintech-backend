CREATE TABLE customer_guarantors(
    id UUID PRIMARY KEY,

    customer_id UUID,

    guarantor_name VARCHAR(255),

    mobile_number VARCHAR(20),

    relationship VARCHAR(100),

    address TEXT,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_customer_guarantor_customer
        FOREIGN KEY(customer_id)
        REFERENCES customers(id)
);