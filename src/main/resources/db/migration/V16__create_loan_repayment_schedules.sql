CREATE TABLE loan_repayment_schedules (

    id UUID PRIMARY KEY,

    loan_id UUID NOT NULL,

    installment_number INTEGER,

    due_date DATE,

    due_amount DECIMAL(18,2),

    paid_amount DECIMAL(18,2),

    outstanding_amount DECIMAL(18,2),

    payment_date DATE,

    repayment_status VARCHAR(50),

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_repayment_schedule_loan
        FOREIGN KEY(loan_id)
        REFERENCES loans(id)
);