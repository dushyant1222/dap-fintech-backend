-- Performance Indexes for high-frequency queries
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(loan_status);
CREATE INDEX IF NOT EXISTS idx_loans_customer_id ON loans(customer_id);
CREATE INDEX IF NOT EXISTS idx_loans_collector_id ON loans(collector_id);
CREATE INDEX IF NOT EXISTS idx_loans_created_by ON loans(created_by);
CREATE INDEX IF NOT EXISTS idx_loans_disbursement_date ON loans(disbursement_date);
CREATE INDEX IF NOT EXISTS idx_loans_loan_type ON loans(loan_type);

CREATE INDEX IF NOT EXISTS idx_schedules_loan_id ON loan_repayment_schedules(loan_id);
CREATE INDEX IF NOT EXISTS idx_schedules_status ON loan_repayment_schedules(repayment_status);
CREATE INDEX IF NOT EXISTS idx_schedules_due_date ON loan_repayment_schedules(due_date);
CREATE INDEX IF NOT EXISTS idx_schedules_loan_status ON loan_repayment_schedules(loan_id, repayment_status);

CREATE INDEX IF NOT EXISTS idx_collections_employee_id ON loan_collections(employee_id);
CREATE INDEX IF NOT EXISTS idx_collections_loan_id ON loan_collections(loan_id);
CREATE INDEX IF NOT EXISTS idx_collections_date ON loan_collections(collection_date);

CREATE INDEX IF NOT EXISTS idx_customers_mobile ON customers(mobile_number);
CREATE INDEX IF NOT EXISTS idx_customers_market_id ON customers(market_id);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);

CREATE INDEX IF NOT EXISTS idx_users_mobile ON users(mobile_number);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(user_id, is_read);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action_time ON audit_logs(action_time);

CREATE INDEX IF NOT EXISTS idx_employee_permissions_emp_id ON employee_permissions(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_permissions_perm_id ON employee_permissions(permission_id);
