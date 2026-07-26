ALTER TABLE loan_repayment_schedules
ADD COLUMN principal_amount DECIMAL(18,2);

ALTER TABLE loan_repayment_schedules
ADD COLUMN interest_amount DECIMAL(18,2);

ALTER TABLE loan_repayment_schedules
ADD COLUMN installment_amount DECIMAL(18,2);