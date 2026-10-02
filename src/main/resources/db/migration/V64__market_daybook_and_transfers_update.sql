ALTER TABLE market_day_book ADD COLUMN IF NOT EXISTS total_cash_incoming_transfers DECIMAL(18, 2) DEFAULT 0.00;
ALTER TABLE market_day_book ADD COLUMN IF NOT EXISTS total_cash_outgoing_transfers DECIMAL(18, 2) DEFAULT 0.00;

ALTER TABLE internal_transfers ADD COLUMN IF NOT EXISTS sender_market_id UUID;
ALTER TABLE internal_transfers ADD COLUMN IF NOT EXISTS receiver_market_id UUID;
