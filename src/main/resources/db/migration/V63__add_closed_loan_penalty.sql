-- Add closed loan penalty columns to loans table
ALTER TABLE loans ADD COLUMN IF NOT EXISTS closed_loan_penalty NUMERIC(15, 2) DEFAULT 0.00;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS closed_penalty_remarks TEXT;
