CREATE TABLE customer_visits (

    id UUID PRIMARY KEY,

    customer_id UUID NOT NULL,

    employee_id UUID NOT NULL,

    visit_date TIMESTAMP,

    visit_status VARCHAR(50),

    remarks TEXT,

    promise_amount DECIMAL(18,2),

    promise_date DATE,

    latitude DECIMAL(10,8),

    longitude DECIMAL(11,8),

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_visit_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_visit_employee
        FOREIGN KEY (employee_id)
        REFERENCES users(id)
);