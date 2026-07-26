ALTER TABLE customers

ADD COLUMN created_by UUID;

ALTER TABLE customers

ADD CONSTRAINT fk_customers_created_by
FOREIGN KEY (created_by)
REFERENCES users(id);