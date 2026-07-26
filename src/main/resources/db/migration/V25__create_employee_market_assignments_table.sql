CREATE TABLE employee_market_assignments (

    id UUID PRIMARY KEY,

    market_id UUID NOT NULL,

    employee_id UUID NOT NULL,

    assigned_date TIMESTAMP,

    is_active BOOLEAN,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT fk_assignment_market
        FOREIGN KEY (market_id)
        REFERENCES markets(id),

    CONSTRAINT fk_assignment_employee
        FOREIGN KEY (employee_id)
        REFERENCES users(id)
);