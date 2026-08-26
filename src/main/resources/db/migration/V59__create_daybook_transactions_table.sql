CREATE TABLE daybook_transactions (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL,
    daybook_id UUID,
    CONSTRAINT fk_daybook_transaction_daybook FOREIGN KEY (daybook_id) REFERENCES day_book(id)
);
