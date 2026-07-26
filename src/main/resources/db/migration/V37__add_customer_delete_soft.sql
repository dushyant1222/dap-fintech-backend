ALTER TABLE customers
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE customers
ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE customers
ADD COLUMN deleted_by UUID NULL;

CREATE INDEX idx_customers_deleted
ON customers(deleted);
