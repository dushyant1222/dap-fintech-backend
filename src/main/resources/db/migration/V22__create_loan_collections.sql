CREATE TABLE loan_collections (

    id UUID PRIMARY KEY,

    loan_id UUID NOT NULL,

    repayment_schedule_id UUID NOT NULL,

    receipt_number VARCHAR(100) UNIQUE,

    collected_amount DECIMAL(18,2),

    collection_date TIMESTAMP,

    collection_mode VARCHAR(50),

    collection_status VARCHAR(50),

    remarks TEXT,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_collection_loan
        FOREIGN KEY (loan_id)
        REFERENCES loans(id),

    CONSTRAINT fk_collection_schedule
        FOREIGN KEY (repayment_schedule_id)
        REFERENCES loan_repayment_schedules(id)
);