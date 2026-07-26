CREATE TABLE employee_permissions (

    id UUID PRIMARY KEY,

    employee_id UUID NOT NULL,

    permission_id UUID NOT NULL,

    allowed BOOLEAN NOT NULL,

    created_at TIMESTAMP,

    updated_at TIMESTAMP,

    created_by VARCHAR(255),

    updated_by VARCHAR(255),

    CONSTRAINT fk_employee_permission_employee
        FOREIGN KEY(employee_id)
        REFERENCES users(id),

    CONSTRAINT fk_employee_permission_permission
        FOREIGN KEY(permission_id)
        REFERENCES permissions(id),

    CONSTRAINT uk_employee_permission
        UNIQUE(employee_id, permission_id)

);