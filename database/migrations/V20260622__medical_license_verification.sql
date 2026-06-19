-- Medical license verification fields for hospitals and doctors

ALTER TABLE hospitals
    ADD COLUMN IF NOT EXISTS clinical_establishment_registration_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS municipal_license_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pharmacy_license_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS laboratory_license_number VARCHAR(100);

ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS medical_registration_number VARCHAR(100);