ALTER TABLE day_book ADD COLUMN IF NOT EXISTS transfer_to_others DECIMAL(15, 2) DEFAULT 0.00;
ALTER TABLE market_day_book ADD COLUMN IF NOT EXISTS total_transfer_to_others DECIMAL(18, 2) DEFAULT 0.00;
