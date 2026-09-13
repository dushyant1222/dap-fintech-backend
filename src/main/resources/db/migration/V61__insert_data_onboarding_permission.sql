INSERT INTO permissions
(
    id,
    permission_key,
    module_name,
    description
)
VALUES
(gen_random_uuid(),
 'DATA_ONBOARDING',
 'Data Onboarding',
 'Onboard historical customer loans and spreadsheet bulk import')
ON CONFLICT (permission_key)
DO NOTHING;
