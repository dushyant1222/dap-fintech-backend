ALTER TABLE day_book ADD COLUMN cash_incoming_transfers NUMERIC(38, 2) DEFAULT 0.0;
ALTER TABLE day_book ADD COLUMN cash_outgoing_transfers NUMERIC(38, 2) DEFAULT 0.0;
