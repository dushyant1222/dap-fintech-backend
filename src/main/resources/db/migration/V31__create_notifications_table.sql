CREATE TABLE notifications (

    id UUID PRIMARY KEY,

    title VARCHAR(255),

    message VARCHAR(1000),

    is_read BOOLEAN,

    created_at TIMESTAMP
);