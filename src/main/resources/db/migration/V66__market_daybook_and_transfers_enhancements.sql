ALTER TABLE daybook_transactions ADD COLUMN IF NOT EXISTS market_id UUID;
ALTER TABLE daybook_transactions ADD COLUMN IF NOT EXISTS market_daybook_id UUID;
ALTER TABLE internal_transfers ALTER COLUMN receiver_id DROP NOT NULL;
