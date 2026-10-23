-- V3: Seed role definitions
INSERT INTO roles (name) VALUES
    ('ROLE_USER'),
    ('ROLE_ANALYST'),
    ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;
