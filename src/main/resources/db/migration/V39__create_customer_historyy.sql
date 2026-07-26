CREATE TABLE customer_history (

    id UUID PRIMARY KEY,

    customer_id UUID NOT NULL,

    action VARCHAR(50) NOT NULL,

    title VARCHAR(150) NOT NULL,

    description TEXT,

    old_value TEXT,

    new_value TEXT,

    performed_by UUID,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_history_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_customer_history_user
        FOREIGN KEY (performed_by)
        REFERENCES users(id)
);

CREATE INDEX idx_customer_history_customer_id
    ON customer_history(customer_id);

CREATE INDEX idx_customer_history_action
    ON customer_history(action);