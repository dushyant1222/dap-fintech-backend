CREATE TABLE loan_closures (

    id UUID PRIMARY KEY,

    loan_id UUID UNIQUE NOT NULL,

    closure_date TIMESTAMP,

    remarks TEXT,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_closure_loan
        FOREIGN KEY (loan_id)
        REFERENCES loans(id)
);