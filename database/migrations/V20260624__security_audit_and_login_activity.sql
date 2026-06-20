CREATE TABLE IF NOT EXISTS login_activities (
    id UUID PRIMARY KEY,
    user_id UUID,
    hospital_id UUID,
    email VARCHAR(150) NOT NULL,
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(200),
    ip_address VARCHAR(80),
    user_agent TEXT,
    logged_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_login_activities_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_login_activities_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);

CREATE TABLE IF NOT EXISTS security_audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    hospital_id UUID,
    method VARCHAR(20) NOT NULL,
    path VARCHAR(500) NOT NULL,
    action VARCHAR(80) NOT NULL,
    http_status INTEGER,
    ip_address VARCHAR(80),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_security_audit_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_security_audit_logs_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);

CREATE INDEX IF NOT EXISTS idx_login_activities_email_logged_at
    ON login_activities (email, logged_at DESC);

CREATE INDEX IF NOT EXISTS idx_login_activities_hospital_logged_at
    ON login_activities (hospital_id, logged_at DESC);

CREATE INDEX IF NOT EXISTS idx_security_audit_logs_hospital_created_at
    ON security_audit_logs (hospital_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_security_audit_logs_user_created_at
    ON security_audit_logs (user_id, created_at DESC);
