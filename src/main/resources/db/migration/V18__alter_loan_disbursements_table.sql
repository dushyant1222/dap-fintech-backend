ALTER TABLE loan_disbursements
ADD COLUMN approved_amount DECIMAL(18,2);

ALTER TABLE loan_disbursements
ADD COLUMN total_charges DECIMAL(18,2);

ALTER TABLE loan_disbursements
ADD COLUMN net_disbursed_amount DECIMAL(18,2);

ALTER TABLE loan_disbursements
ADD COLUMN disbursement_mode VARCHAR(50);

ALTER TABLE loan_disbursements
ADD COLUMN remarks TEXT;