CREATE table role_permissions (
id UUID PRIMARY KEY,
role_id UUID,
permission_id UUID,
created_at timestamp,
updated_at timestamp,

constraint fk_role_permission_role
foreign key(role_id)
references roles(id),

constraint fk_role_permission_permission
foreign key(permission_id)
references permissions(id)
);