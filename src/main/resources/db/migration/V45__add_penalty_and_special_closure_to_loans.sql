ALTER TABLE loans ADD COLUMN penalty_rate NUMERIC(10, 2) DEFAULT 0.50;
ALTER TABLE loans ADD COLUMN penalty_waived_percent NUMERIC(10, 2) DEFAULT 0.00;
ALTER TABLE loans ADD COLUMN closed_special_condition BOOLEAN DEFAULT FALSE;
ALTER TABLE loans ADD COLUMN special_closure_remarks TEXT;
