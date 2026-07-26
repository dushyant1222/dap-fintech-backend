INSERT INTO roles (
    id,
    role_name,
    role_description,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'EMPLOYEE',
    'Field Collection Employee',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE role_name = 'EMPLOYEE'
);

INSERT INTO roles (
    id,
    role_name,
    role_description,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'ADMIN',
    'System Administrator',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE role_name = 'ADMIN'
);