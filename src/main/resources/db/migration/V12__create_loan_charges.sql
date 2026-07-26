CREATE TABLE loan_charges (

    id UUID PRIMARY KEY,

    loan_id UUID NOT NULL,

    charge_type VARCHAR(100),

    charge_amount DECIMAL(18,2),

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_loan_charge_loan
        FOREIGN KEY(loan_id)
        REFERENCES loans(id)
);