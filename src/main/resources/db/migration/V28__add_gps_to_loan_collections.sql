ALTER TABLE loan_collections
ADD COLUMN collected_by UUID;

ALTER TABLE loan_collections
ADD COLUMN latitude DECIMAL(10,8);

ALTER TABLE loan_collections
ADD COLUMN longitude DECIMAL(11,8);

ALTER TABLE loan_collections
ADD CONSTRAINT fk_collection_employee
FOREIGN KEY (collected_by)
REFERENCES users(id);