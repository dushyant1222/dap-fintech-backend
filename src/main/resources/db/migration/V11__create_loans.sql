CREATE TABLE loans (

    id UUID PRIMARY KEY,

    customer_id UUID NOT NULL,

    loan_type VARCHAR(50) NOT NULL,

    loan_amount DECIMAL(18,2) NOT NULL,

    approved_amount DECIMAL(18,2),

    disbursed_amount DECIMAL(18,2),

    interest_type VARCHAR(50),

    interest_rate DECIMAL(10,2),

    tenure INTEGER,

    repayment_frequency VARCHAR(50),

    loan_status VARCHAR(50),

    application_date TIMESTAMP,

    approval_date TIMESTAMP,

    disbursement_date TIMESTAMP,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_loan_customer
        FOREIGN KEY(customer_id)
        REFERENCES customers(id)
);