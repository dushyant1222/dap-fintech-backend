-- Add onesignal_id column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS onesignal_id VARCHAR(255);
