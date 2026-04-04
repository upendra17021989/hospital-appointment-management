-- ============================================================
-- PATIENT DETAILS & PRESCRIPTION TABLES
-- Run after auth_schema.sql
-- ============================================================

-- ============================================================
-- PATIENT MEDICAL PROFILE (extended patient data)
-- ============================================================
CREATE TABLE patient_medical_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID UNIQUE REFERENCES patients(id) ON DELETE CASCADE,
    hospital_id UUID REFERENCES hospitals(id) ON DELETE CASCADE,

    -- Vital Statistics
    blood_group VARCHAR(5),
    height_cm DECIMAL(5,1),
    weight_kg DECIMAL(5,1),

    -- Medical History
    known_allergies TEXT,
    chronic_conditions TEXT,        -- Diabetes, Hypertension, Asthma etc.
    current_medications TEXT,
    past_surgeries TEXT,
    family_history TEXT,

    -- Lifestyle
    smoking_status VARCHAR(20) CHECK (smoking_status IN ('never','former','current','unknown')),
    alcohol_consumption VARCHAR(20) CHECK (alcohol_consumption IN ('never','occasional','moderate','heavy','unknown')),
    occupation VARCHAR(100),

    -- Insurance
    insurance_provider VARCHAR(100),
    insurance_policy_number VARCHAR(100),

    -- Emergency
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relation VARCHAR(50),

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- PRESCRIPTIONS
-- ============================================================
CREATE TABLE prescriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    hospital_id UUID REFERENCES hospitals(id) ON DELETE CASCADE,

    prescription_date DATE NOT NULL DEFAULT CURRENT_DATE,
    diagnosis TEXT NOT NULL,
    chief_complaint TEXT,
    examination_notes TEXT,
    vital_signs TEXT,               -- JSON: {bp, pulse, temp, weight, height, spo2}

    follow_up_date DATE,
    follow_up_instructions TEXT,
    diet_instructions TEXT,
    activity_restrictions TEXT,
    additional_notes TEXT,

    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- PRESCRIPTION MEDICINES
-- ============================================================
CREATE TABLE prescription_medicines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prescription_id UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    medicine_name VARCHAR(200) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(100),
    route VARCHAR(50) DEFAULT 'Oral',
    before_food BOOLEAN DEFAULT FALSE,
    instructions VARCHAR(200),
    sort_order INTEGER DEFAULT 0
);

-- ============================================================
-- LAB TESTS
-- ============================================================
CREATE TABLE lab_tests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prescription_id UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    test_name VARCHAR(200) NOT NULL,
    instructions VARCHAR(200),
    is_urgent BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_prescriptions_patient    ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_doctor     ON prescriptions(doctor_id);
CREATE INDEX idx_prescriptions_appointment ON prescriptions(appointment_id);
CREATE INDEX idx_prescriptions_hospital   ON prescriptions(hospital_id);
CREATE INDEX idx_presc_medicines_presc    ON prescription_medicines(prescription_id);
CREATE INDEX idx_lab_tests_presc          ON lab_tests(prescription_id);
CREATE INDEX idx_patient_profile_patient  ON patient_medical_profiles(patient_id);

-- ============================================================
-- TRIGGERS
-- ============================================================
CREATE TRIGGER update_prescriptions_updated_at
  BEFORE UPDATE ON prescriptions
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_patient_profiles_updated_at
  BEFORE UPDATE ON patient_medical_profiles
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- RLS
-- ============================================================
ALTER TABLE prescriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE prescription_medicines ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_medical_profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow prescriptions"        ON prescriptions FOR ALL USING (TRUE);
CREATE POLICY "Allow prescription_meds"    ON prescription_medicines FOR ALL USING (TRUE);
CREATE POLICY "Allow lab_tests"            ON lab_tests FOR ALL USING (TRUE);
CREATE POLICY "Allow patient_profiles"     ON patient_medical_profiles FOR ALL USING (TRUE);
