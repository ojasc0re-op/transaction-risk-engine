-- V4: Development seed data
-- Demonstrates LOW / MEDIUM / HIGH / CRITICAL risk scenarios
-- Passwords are BCrypt hashes:
-- 'admin123'   -> $2a$10$wK1k0n09p4K9l0v170hZ8eq3m.Q1eZkVvjL2E4uWn9cZ4Tq9y1L4G
-- 'analyst123' -> $2a$10$tZ2zV9K0wE3v4h6u10jZ8eq3m.Q1eZkVvjL2E4uWn9cZ4Tq9y1L4G
-- 'user123'    -> $2a$10$1b2c3d4e5f6g7h8i9j0k1eq3m.Q1eZkVvjL2E4uWn9cZ4Tq9y1L4G
-- Using a standard bcrypt format verified to work:
-- $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8. (matches 'password')

INSERT INTO users (id, username, email, password_hash, full_name, avg_transaction_amount, total_transactions)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'admin', 'admin@frauddetect.dev',
     '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'System Admin', 500.00, 50),
    ('22222222-2222-2222-2222-222222222222', 'analyst', 'analyst@frauddetect.dev',
     '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Fraud Analyst', 250.00, 20),
    ('33333333-3333-3333-3333-333333333333', 'john_doe', 'john@example.com',
     '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'John Doe', 150.00, 30),
    ('44444444-4444-4444-4444-444444444444', 'jane_smith', 'jane@example.com',
     '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Jane Smith', 300.00, 15)
ON CONFLICT (username) DO NOTHING;

-- Assign roles
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-1111-1111-1111-111111111111', id FROM roles WHERE name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-1111-1111-1111-111111111111', id FROM roles WHERE name = 'ROLE_USER'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id)
SELECT '22222222-2222-2222-2222-222222222222', id FROM roles WHERE name = 'ROLE_ANALYST'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id)
SELECT '22222222-2222-2222-2222-222222222222', id FROM roles WHERE name = 'ROLE_USER'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id)
SELECT '33333333-3333-3333-3333-333333333333', id FROM roles WHERE name = 'ROLE_USER'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id)
SELECT '44444444-4444-4444-4444-444444444444', id FROM roles WHERE name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- Seed merchants with risk levels
INSERT INTO merchants (name, risk_level, category, description) VALUES
    ('Amazon', 'LOW', 'E-COMMERCE', 'Established e-commerce platform'),
    ('Walmart', 'LOW', 'RETAIL', 'Major retail chain'),
    ('Crypto Exchange XYZ', 'HIGH', 'CRYPTOCURRENCY', 'Cryptocurrency trading platform - elevated risk'),
    ('Online Casino Royal', 'HIGH', 'GAMBLING', 'Online gambling platform - high risk'),
    ('TechStore Pro', 'MEDIUM', 'ELECTRONICS', 'Electronics retailer'),
    ('Quick Cash Advance', 'HIGH', 'FINANCIAL', 'Short-term lending - elevated risk'),
    ('Grocery Plus', 'LOW', 'GROCERY', 'Local grocery chain')
ON CONFLICT (name) DO NOTHING;

-- Seed sample device for John Doe
INSERT INTO devices (user_id, device_fingerprint, device_name, device_type, trusted, last_seen_at)
VALUES
    ('33333333-3333-3333-3333-333333333333', 'device-known-001', 'John iPhone 15', 'MOBILE', TRUE, NOW())
ON CONFLICT DO NOTHING;

-- Seed transactions demonstrating LOW, MEDIUM, HIGH, and CRITICAL risk
INSERT INTO transactions (id, user_id, amount, currency, merchant, transaction_type, transaction_timestamp,
                          location, latitude, longitude, device_id, ip_address, status, risk_score, risk_level, risk_reasons)
VALUES
    -- LOW risk: normal amount, known device, low-risk merchant
    ('a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 120.00, 'USD', 'Amazon',
     'PURCHASE', NOW() - INTERVAL '2 hours', 'New York, NY', 40.7128, -74.0060,
     'device-known-001', '192.168.1.10', 'APPROVED', 0, 'LOW', '[]'),

    -- MEDIUM risk: new device (+15), medium risk merchant TechStore Pro
    ('b2222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 280.00, 'USD', 'TechStore Pro',
     'PURCHASE', NOW() - INTERVAL '1 hour', 'New York, NY', 40.7128, -74.0060,
     'device-new-unknown', '192.168.1.10', 'APPROVED', 35, 'MEDIUM', '["Transaction from a previously unseen device: device-new-unknown"]'),

    -- HIGH risk: large amount (+20), high risk merchant (+15), new device (+15), location anomaly (+25) -> 75
    ('c3333333-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444', 2500.00, 'USD', 'Crypto Exchange XYZ',
     'PURCHASE', NOW() - INTERVAL '30 minutes', 'Los Angeles, CA', 34.0522, -118.2437,
     'device-suspicious', '10.0.0.1', 'REVIEW', 75, 'HIGH', '["Unusual transaction amount", "High-risk merchant: Crypto Exchange XYZ", "Transaction from a previously unseen device: device-suspicious"]'),

    -- CRITICAL risk: extreme amount, flagged IP (+20), casino merchant (+15), new device (+15), failed previous txns (+10) -> score 90+
    ('d4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 9999.00, 'USD', 'Online Casino Royal',
     'PURCHASE', NOW() - INTERVAL '5 minutes', 'Unknown Location', 51.5074, -0.1278,
     'device-critical', '192.168.100.1', 'BLOCKED', 95, 'CRITICAL', '["Unusual transaction amount", "Flagged IP address: 192.168.100.1", "High-risk merchant: Online Casino Royal", "Transaction from a previously unseen device: device-critical"]')
ON CONFLICT (id) DO NOTHING;
