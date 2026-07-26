CREATE TABLE customer_documents(
    id UUID PRIMARY KEY,

    customer_id UUID,

    document_type VARCHAR(50),

    file_name VARCHAR(255),

    file_path TEXT,

    verification_status VARCHAR(50),

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_customer_document_customer
        FOREIGN KEY(customer_id)
        REFERENCES customers(id)
);