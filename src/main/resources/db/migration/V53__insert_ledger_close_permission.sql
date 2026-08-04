INSERT INTO permissions
(
    id,
    permission_key,
    module_name,
    description
)
VALUES
(gen_random_uuid(),
 'CLOSE_LEDGER',
 'Accounting',
 'Close Ledger')
ON CONFLICT (permission_key)
DO NOTHING;
