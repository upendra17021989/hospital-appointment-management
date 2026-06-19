-- Dynamic medical certificates issued by a hospital

CREATE TABLE IF NOT EXISTS medical_certificates (
    id UUID PRIMARY KEY,

    hospital_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    appointment_id UUID,

    certificate_number VARCHAR(60) NOT NULL,
    certificate_type VARCHAR(60) NOT NULL,
    certificate_status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL,

    issue_date DATE NOT NULL,
    valid_from DATE,
    valid_until DATE,

    diagnosis_or_reason TEXT,
    remarks TEXT,
    dynamic_fields TEXT,

    patient_name VARCHAR(200) NOT NULL,
    doctor_name VARCHAR(200) NOT NULL,
    department_name VARCHAR(200),
    hospital_name VARCHAR(150) NOT NULL,
    hospital_address TEXT,
    hospital_phone VARCHAR(25),
    issued_by_name VARCHAR(150),

    voided_at TIMESTAMP,
    voided_by UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT uq_medical_certificates_hospital_number
        UNIQUE (hospital_id, certificate_number),

    CONSTRAINT fk_medical_certificates_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id),

    CONSTRAINT fk_medical_certificates_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id),

    CONSTRAINT fk_medical_certificates_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id),

    CONSTRAINT fk_medical_certificates_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

CREATE INDEX IF NOT EXISTS idx_medical_certificates_hospital_number
    ON medical_certificates (hospital_id, certificate_number);

CREATE INDEX IF NOT EXISTS idx_medical_certificates_hospital_created
    ON medical_certificates (hospital_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_medical_certificates_hospital_patient
    ON medical_certificates (hospital_id, patient_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_medical_certificates_hospital_type
    ON medical_certificates (hospital_id, certificate_type);

CREATE INDEX IF NOT EXISTS idx_medical_certificates_status
    ON medical_certificates (certificate_status);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_medical_certificates_voided_by'
        ) THEN
            ALTER TABLE medical_certificates
                ADD CONSTRAINT fk_medical_certificates_voided_by
                FOREIGN KEY (voided_by) REFERENCES users(id);
        END IF;
    END IF;
END $$;
