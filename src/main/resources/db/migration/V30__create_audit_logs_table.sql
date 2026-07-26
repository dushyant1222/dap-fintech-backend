CREATE TABLE audit_logs (

    id UUID PRIMARY KEY,

    user_name VARCHAR(100),

    action VARCHAR(100),

    module_name VARCHAR(100),

    entity_id VARCHAR(100),

    action_time TIMESTAMP
);