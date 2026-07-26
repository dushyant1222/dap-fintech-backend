CREATE TABLE loan_disbursements (

    id UUID PRIMARY KEY,

    loan_id UUID NOT NULL,

    amount DECIMAL(18,2),

    transaction_reference VARCHAR(255),

    disbursement_status VARCHAR(50),

    disbursement_date TIMESTAMP,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_loan_disbursement_loan
        FOREIGN KEY(loan_id)
        REFERENCES loans(id)
);