INSERT INTO permissions
(
    id,
    permission_key,
    module_name,
    description
)
VALUES

(gen_random_uuid(),
 'VIEW_DASHBOARD',
 'Dashboard',
 'View Dashboard'),

(gen_random_uuid(),
 'VIEW_CUSTOMERS',
 'Customer',
 'View Customers'),

(gen_random_uuid(),
 'CREATE_CUSTOMER',
 'Customer',
 'Create Customer'),

(gen_random_uuid(),
 'EDIT_CUSTOMER',
 'Customer',
 'Edit Customer'),

(gen_random_uuid(),
 'VIEW_LOANS',
 'Loan',
 'View Loans'),

(gen_random_uuid(),
 'CREATE_LOAN',
 'Loan',
 'Create Loan'),

(gen_random_uuid(),
 'COLLECT_EMI',
 'Collection',
 'Collect EMI'),

(gen_random_uuid(),
 'VIEW_COLLECTIONS',
 'Collection',
 'View Collections'),

(gen_random_uuid(),
 'VIEW_REPORTS',
 'Report',
 'View Reports')

ON CONFLICT (permission_key)
DO NOTHING;