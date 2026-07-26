ALTER TABLE loans
ADD COLUMN created_by UUID;

ALTER TABLE loans
ADD CONSTRAINT fk_loans_created_by
FOREIGN KEY (created_by)
REFERENCES users(id);