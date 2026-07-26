CREATE TABLE system_settings (

    id UUID PRIMARY KEY,

    setting_key VARCHAR(100)
    UNIQUE,

    setting_value VARCHAR(500)
);