CREATE TABLE loan_approvals (

    id UUID PRIMARY KEY,

    loan_id UUID NOT NULL,

    approved_by UUID,

    decision VARCHAR(50),

    remarks TEXT,

    approval_date TIMESTAMP,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_loan_approval_loan
        FOREIGN KEY(loan_id)
        REFERENCES loans(id),

    CONSTRAINT fk_loan_approval_user
        FOREIGN KEY(approved_by)
        REFERENCES users(id)
);