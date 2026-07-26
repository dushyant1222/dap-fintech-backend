ALTER TABLE audit_logs
ADD COLUMN user_id UUID;

CREATE INDEX idx_audit_logs_user_id_action_time
ON audit_logs(user_id, action_time DESC);