CREATE TABLE loan_documents (

    id UUID PRIMARY KEY,

    loan_id UUID NOT NULL,

    document_type VARCHAR(100),

    file_name VARCHAR(255),

    file_path TEXT,

    verification_status VARCHAR(50),

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_loan_document_loan
        FOREIGN KEY(loan_id)
        REFERENCES loans(id)
);