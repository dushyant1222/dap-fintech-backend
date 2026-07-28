CREATE TABLE day_book (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    date DATE NOT NULL,
    opening_balance DECIMAL(15,2) DEFAULT 0.00,
    collections DECIMAL(15,2) DEFAULT 0.00,
    incoming_transfers DECIMAL(15,2) DEFAULT 0.00,
    spends DECIMAL(15,2) DEFAULT 0.00,
    loans_disbursed DECIMAL(15,2) DEFAULT 0.00,
    outgoing_transfers DECIMAL(15,2) DEFAULT 0.00,
    office_remittance DECIMAL(15,2) DEFAULT 0.00,
    closing_balance DECIMAL(15,2) DEFAULT 0.00,
    status VARCHAR(50) DEFAULT 'OPEN',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE personal_ledger (
    id BIGSERIAL PRIMARY KEY,
    admin_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    category VARCHAR(255) NOT NULL,
    remarks TEXT,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_pl_admin FOREIGN KEY (admin_id) REFERENCES users(id)
);

CREATE TABLE internal_transfers (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    transfer_date TIMESTAMP NOT NULL,
    category VARCHAR(255),
    remarks VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_it_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_it_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
);
