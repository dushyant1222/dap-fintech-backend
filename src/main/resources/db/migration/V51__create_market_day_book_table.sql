CREATE TABLE market_day_book (
    id UUID PRIMARY KEY,
    market_id UUID NOT NULL,
    date DATE NOT NULL,
    total_opening_balance DECIMAL(18, 2) DEFAULT 0.00,
    total_collections DECIMAL(18, 2) DEFAULT 0.00,
    total_incoming_transfers DECIMAL(18, 2) DEFAULT 0.00,
    total_spends DECIMAL(18, 2) DEFAULT 0.00,
    total_loans_disbursed DECIMAL(18, 2) DEFAULT 0.00,
    total_outgoing_transfers DECIMAL(18, 2) DEFAULT 0.00,
    total_office_remittance DECIMAL(18, 2) DEFAULT 0.00,
    total_closing_balance DECIMAL(18, 2) DEFAULT 0.00,
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_market_day_book_market FOREIGN KEY (market_id) REFERENCES markets(id)
);
