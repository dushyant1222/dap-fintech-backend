CREATE TABLE IF NOT EXISTS capital_in (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount NUMERIC(15, 2) NOT NULL,
    capital_date TIMESTAMP NOT NULL,
    source VARCHAR(255),
    remarks VARCHAR(255),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(50) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    expense_date TIMESTAMP NOT NULL,
    remarks VARCHAR(255),
    employee_id UUID REFERENCES users(id),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cash_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES users(id),
    amount_settled NUMERIC(15, 2) NOT NULL,
    settlement_date TIMESTAMP NOT NULL,
    received_by_admin_id UUID REFERENCES users(id),
    remarks VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
