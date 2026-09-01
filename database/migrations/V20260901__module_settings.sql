CREATE TABLE IF NOT EXISTS module_settings (
    id UUID PRIMARY KEY,
    scope_key VARCHAR(50) NOT NULL,
    hospital_id UUID NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    module_key VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_module_setting_scope_module UNIQUE (scope_key, module_key)
);

CREATE INDEX IF NOT EXISTS idx_module_settings_hospital_id ON module_settings(hospital_id);
