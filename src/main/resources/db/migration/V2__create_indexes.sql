-- V2: Create performance indexes

CREATE INDEX IF NOT EXISTS idx_txn_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_txn_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_txn_risk_level ON transactions(risk_level);
CREATE INDEX IF NOT EXISTS idx_txn_timestamp ON transactions(transaction_timestamp);
CREATE INDEX IF NOT EXISTS idx_txn_device_id ON transactions(device_id);
CREATE INDEX IF NOT EXISTS idx_txn_user_timestamp ON transactions(user_id, transaction_timestamp);

CREATE INDEX IF NOT EXISTS idx_device_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_device_fingerprint ON devices(device_fingerprint);

CREATE INDEX IF NOT EXISTS idx_risk_event_user_id ON risk_events(user_id);
CREATE INDEX IF NOT EXISTS idx_risk_event_transaction_id ON risk_events(transaction_id);
CREATE INDEX IF NOT EXISTS idx_risk_event_created_at ON risk_events(created_at);

CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_device_user_fingerprint ON devices(user_id, device_fingerprint);
