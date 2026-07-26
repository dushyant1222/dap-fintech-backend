CREATE TABLE enquiry_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address_type VARCHAR(20) NOT NULL,
    address_line TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE TABLE enquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    father_name VARCHAR(150) NOT NULL,
    mother_name VARCHAR(150),
    mobile_number VARCHAR(15) NOT NULL,
    alternate_mobile VARCHAR(15),
    email VARCHAR(100),
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    occupation VARCHAR(100),
    qualification VARCHAR(100),
    business_type VARCHAR(100),
    business_name VARCHAR(150),
    annual_income NUMERIC(15, 2),
    reference_source VARCHAR(100),
    gps_latitude DOUBLE PRECISION NOT NULL,
    gps_longitude DOUBLE PRECISION NOT NULL,
    remarks TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    employee_id UUID NOT NULL,
    market_id UUID NOT NULL,
    current_address_id UUID NOT NULL,
    permanent_address_id UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_enquiry_employee FOREIGN KEY (employee_id) REFERENCES users(id),
    CONSTRAINT fk_enquiry_market FOREIGN KEY (market_id) REFERENCES markets(id),
    CONSTRAINT fk_enq_curr_addr FOREIGN KEY (current_address_id) REFERENCES enquiry_addresses(id),
    CONSTRAINT fk_enq_perm_addr FOREIGN KEY (permanent_address_id) REFERENCES enquiry_addresses(id)
);

CREATE TABLE enquiry_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enquiry_id UUID NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    file_url TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_enquiry_media FOREIGN KEY (enquiry_id) REFERENCES enquiries(id) ON DELETE CASCADE
);

CREATE TABLE enquiry_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enquiry_id UUID NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    remarks TEXT,
    action_by UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enquiry_history FOREIGN KEY (enquiry_id) REFERENCES enquiries(id) ON DELETE CASCADE,
    CONSTRAINT fk_enquiry_history_user FOREIGN KEY (action_by) REFERENCES users(id)
);

CREATE INDEX idx_enquiry_mobile ON enquiries(mobile_number);
CREATE INDEX idx_enquiry_status ON enquiries(status);
CREATE INDEX idx_enquiry_employee ON enquiries(employee_id);