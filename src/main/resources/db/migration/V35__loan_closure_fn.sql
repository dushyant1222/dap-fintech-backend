ALTER TABLE loan_closures
ADD CONSTRAINT uk_loan_closures_loan_id
UNIQUE (loan_id);