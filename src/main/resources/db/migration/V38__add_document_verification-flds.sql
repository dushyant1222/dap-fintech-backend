ALTER TABLE customer_documents
ADD COLUMN IF NOT EXISTS verification_remark VARCHAR(500);

ALTER TABLE customer_documents
ADD COLUMN IF NOT EXISTS verified_by UUID;

UPDATE customer_documents
SET verification_status = 'PENDING'
WHERE verification_status IS NULL;

ALTER TABLE customer_documents
ALTER COLUMN verification_status SET NOT NULL;