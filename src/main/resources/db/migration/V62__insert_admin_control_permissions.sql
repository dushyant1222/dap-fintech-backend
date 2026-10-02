INSERT INTO permissions (id, permission_key, module_name, description)
VALUES
(gen_random_uuid(), 'VIEW_EMPLOYEES', 'Employee', 'View and manage employees'),
(gen_random_uuid(), 'VIEW_MARKETS', 'Market', 'View and manage markets'),
(gen_random_uuid(), 'VIEW_ENQUIRIES', 'Enquiry', 'View and manage enquiries'),
(gen_random_uuid(), 'VIEW_PERSONAL_LEDGER', 'Accounting', 'View personal ledger'),
(gen_random_uuid(), 'VIEW_CAPITAL', 'Accounting', 'View capital and cash pivot accounting'),
(gen_random_uuid(), 'VIEW_BUSINESS_GROWTH', 'Business Growth', 'View business growth model')
ON CONFLICT (permission_key) DO NOTHING;
