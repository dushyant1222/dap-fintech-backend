CREATE TABLE sync_logs (

    id UUID PRIMARY KEY,

    entity_type VARCHAR(100),

    entity_id VARCHAR(100),

    sync_status VARCHAR(50),

    sync_time TIMESTAMP
);