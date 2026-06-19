-- Combined database SQL script
-- Generated from database/*.sql and database/migrations/*.sql
-- Original files are intentionally left unchanged.


-- ============================================================
-- Source: database\schema.sql
-- ============================================================

-- ============================================
-- HOSPITAL ENQUIRY & APPOINTMENT MANAGEMENT
-- Supabase Schema
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- DEPARTMENTS TABLE
-- ============================================
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    floor_number INTEGER,
    phone VARCHAR(20),
    email VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- DOCTORS TABLE
-- ============================================
CREATE TABLE doctors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    qualification VARCHAR(200),
    medical_registration_number VARCHAR(100),
    experience_years INTEGER DEFAULT 0,
    phone VARCHAR(20),
    email VARCHAR(100) UNIQUE,
    bio TEXT,
    profile_image_url TEXT,
    consultation_fee DECIMAL(10,2) DEFAULT 0.00,
    is_available BOOLEAN DEFAULT TRUE,
    languages_spoken TEXT[], -- array of languages
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- DOCTOR SCHEDULES TABLE
-- ============================================
CREATE TABLE doctor_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    doctor_id UUID REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6), -- 0=Sunday, 6=Saturday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER DEFAULT 30,
    max_appointments INTEGER DEFAULT 20,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- PATIENTS TABLE
-- ============================================
CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE,
    age INTEGER CHECK (age >= 0 AND age <= 150),
    gender VARCHAR(10) CHECK (gender IN ('male', 'female', 'other')),
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address TEXT,
    blood_group VARCHAR(5),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    medical_history TEXT,
    allergies TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- APPOINTMENTS TABLE
-- ============================================
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id UUID REFERENCES doctors(id) ON DELETE CASCADE,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    status VARCHAR(20) DEFAULT 'pending' CHECK (
        status IN ('pending', 'confirmed', 'cancelled', 'completed', 'no_show', 'rescheduled')
    ),
    appointment_type VARCHAR(50) DEFAULT 'in_person' CHECK (
        appointment_type IN ('in_person', 'virtual', 'follow_up', 'emergency')
    ),
    reason_for_visit TEXT NOT NULL,
    symptoms TEXT,
    notes TEXT,
    cancellation_reason TEXT,
    token_number VARCHAR(20),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- ENQUIRIES TABLE
-- ============================================
CREATE TABLE enquiries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    enquiry_type VARCHAR(50) DEFAULT 'general' CHECK (
        enquiry_type IN ('general', 'appointment', 'billing', 'medical', 'complaint', 'feedback')
    ),
    status VARCHAR(20) DEFAULT 'open' CHECK (
        status IN ('open', 'in_progress', 'resolved', 'closed')
    ),
    priority VARCHAR(10) DEFAULT 'normal' CHECK (
        priority IN ('low', 'normal', 'high', 'urgent')
    ),
    assigned_to VARCHAR(100),
    response TEXT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- APPOINTMENT LOGS (Audit Trail)
-- ============================================
CREATE TABLE appointment_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    appointment_id UUID REFERENCES appointments(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_by VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_date ON appointments(appointment_date);
CREATE INDEX idx_doctor_schedules_doctor ON doctor_schedules(doctor_id);
CREATE INDEX idx_enquiries_status ON enquiries(status);
CREATE INDEX idx_enquiries_type ON enquiries(enquiry_type);

-- ============================================
-- UPDATED_AT TRIGGER FUNCTION
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to all tables with updated_at
CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_doctors_updated_at BEFORE UPDATE ON doctors FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_patients_updated_at BEFORE UPDATE ON patients FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_appointments_updated_at BEFORE UPDATE ON appointments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_enquiries_updated_at BEFORE UPDATE ON enquiries FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- APPOINTMENT LOG TRIGGER
-- ============================================
CREATE OR REPLACE FUNCTION log_appointment_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO appointment_logs (appointment_id, action, old_status, new_status)
        VALUES (NEW.id, 'status_changed', OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER log_appointment_status_changes
AFTER UPDATE ON appointments
FOR EACH ROW EXECUTE FUNCTION log_appointment_changes();

-- ============================================
-- TOKEN NUMBER GENERATION
-- ============================================
CREATE OR REPLACE FUNCTION generate_token_number()
RETURNS TRIGGER AS $$
DECLARE
    date_str VARCHAR(8);
    count_today INTEGER;
BEGIN
    date_str := TO_CHAR(NEW.appointment_date, 'YYYYMMDD');
    SELECT COUNT(*) INTO count_today
    FROM appointments
    WHERE appointment_date = NEW.appointment_date
    AND doctor_id = NEW.doctor_id;
    NEW.token_number := 'TKN-' || date_str || '-' || LPAD((count_today + 1)::TEXT, 3, '0');
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER generate_appointment_token
BEFORE INSERT ON appointments
FOR EACH ROW EXECUTE FUNCTION generate_token_number();

-- ============================================
-- SUBSCRIPTION PLANS TABLE
-- ============================================
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    monthly_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    yearly_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    stripe_price_id_monthly VARCHAR(100),
    stripe_price_id_yearly VARCHAR(100),
    max_doctors INTEGER DEFAULT 3,
    max_users INTEGER DEFAULT 2,
    max_appointments_per_month INTEGER DEFAULT 100,
    allow_prescriptions BOOLEAN DEFAULT FALSE,
    allow_sms BOOLEAN DEFAULT FALSE,
    allow_whatsapp BOOLEAN DEFAULT FALSE,
    allow_custom_branding BOOLEAN DEFAULT FALSE,
    priority_support BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- HOSPITAL SUBSCRIPTIONS TABLE
-- ============================================
CREATE TABLE hospital_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(20) DEFAULT 'trial' CHECK (
        status IN ('trial', 'active', 'past_due', 'cancelled', 'expired')
    ),
    billing_cycle VARCHAR(10) DEFAULT 'monthly' CHECK (
        billing_cycle IN ('monthly', 'yearly')
    ),
    trial_ends_at TIMESTAMPTZ,
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    stripe_customer_id VARCHAR(100),
    stripe_subscription_id VARCHAR(100),
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- PAYMENTS TABLE
-- ============================================
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES hospital_subscriptions(id),
    stripe_payment_intent_id VARCHAR(100),
    stripe_invoice_id VARCHAR(100),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) DEFAULT 'pending' CHECK (
        status IN ('pending', 'succeeded', 'failed', 'refunded')
    ),
    description TEXT,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- SUBSCRIPTION-RELATED INDEXES
-- ============================================
CREATE INDEX idx_hospital_subscriptions_hospital ON hospital_subscriptions(hospital_id);
CREATE INDEX idx_hospital_subscriptions_status ON hospital_subscriptions(status);
CREATE INDEX idx_payments_hospital ON payments(hospital_id);
CREATE INDEX idx_payments_subscription ON payments(subscription_id);

-- ============================================
-- SUBSCRIPTION TRIGGERS
-- ============================================
CREATE TRIGGER update_hospital_subscriptions_updated_at BEFORE UPDATE ON hospital_subscriptions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- SEED SUBSCRIPTION PLANS
-- ============================================
-- IMPORTANT: After seeding, configure Stripe Price IDs for paid plans:
--   UPDATE subscription_plans SET stripe_price_id_monthly = 'price_xxx' WHERE slug = 'basic';
--   UPDATE subscription_plans SET stripe_price_id_yearly  = 'price_yyy' WHERE slug = 'basic';
-- Repeat for 'pro' and 'enterprise' plans.
-- The Free plan (price = 0) bypasses Stripe checkout automatically.
INSERT INTO subscription_plans (name, slug, description, monthly_price, yearly_price, max_doctors, max_users, max_appointments_per_month, allow_prescriptions, allow_sms, allow_whatsapp, allow_custom_branding, priority_support) VALUES
('Free', 'free', 'Basic features for small clinics just getting started', 0.00, 0.00, 3, 2, 100, FALSE, FALSE, FALSE, FALSE, FALSE),
('Basic', 'basic', 'Essential tools for growing practices', 29.00, 290.00, 10, 5, 500, TRUE, FALSE, FALSE, FALSE, FALSE),
('Pro', 'pro', 'Advanced features for established hospitals', 79.00, 790.00, 25, 15, 999999, TRUE, TRUE, TRUE, TRUE, TRUE),
('Enterprise', 'enterprise', 'Unlimited everything with dedicated support', 199.00, 1990.00, 999, 50, 999999, TRUE, TRUE, TRUE, TRUE, TRUE);

-- ============================================
-- ROW LEVEL SECURITY (RLS) - Supabase
-- ============================================
ALTER TABLE departments ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctor_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE enquiries ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointment_logs ENABLE ROW LEVEL SECURITY;

-- Public read access for departments and doctors
CREATE POLICY "Public can read departments" ON departments FOR SELECT USING (TRUE);
CREATE POLICY "Public can read doctors" ON doctors FOR SELECT USING (is_available = TRUE);
CREATE POLICY "Public can read schedules" ON doctor_schedules FOR SELECT USING (is_active = TRUE);

-- Anyone can create enquiries and patients
CREATE POLICY "Public can create enquiries" ON enquiries FOR INSERT WITH CHECK (TRUE);
CREATE POLICY "Public can create patients" ON patients FOR INSERT WITH CHECK (TRUE);
CREATE POLICY "Public can create appointments" ON appointments FOR INSERT WITH CHECK (TRUE);

-- Patients can read their own appointments
CREATE POLICY "Anyone can read appointments" ON appointments FOR SELECT USING (TRUE);
CREATE POLICY "Anyone can read enquiries" ON enquiries FOR SELECT USING (TRUE);
CREATE POLICY "Anyone can read patients" ON patients FOR SELECT USING (TRUE);

-- Allow updates
CREATE POLICY "Allow appointment updates" ON appointments FOR UPDATE USING (TRUE);
CREATE POLICY "Allow enquiry updates" ON enquiries FOR UPDATE USING (TRUE);
CREATE POLICY "Allow appointment logs" ON appointment_logs FOR ALL USING (TRUE);

-- ============================================
-- SEED DATA - Departments
-- ============================================
INSERT INTO departments (name, description, floor_number, phone, email) VALUES
('Cardiology', 'Heart and cardiovascular system care', 3, '+91-22-1234-5601', 'cardiology@hospital.com'),
('Orthopedics', 'Bone, joint, and muscle disorders', 2, '+91-22-1234-5602', 'ortho@hospital.com'),
('Neurology', 'Brain and nervous system disorders', 4, '+91-22-1234-5603', 'neuro@hospital.com'),
('Pediatrics', 'Medical care for infants, children and adolescents', 1, '+91-22-1234-5604', 'pediatrics@hospital.com'),
('Gynecology', 'Women health and reproductive system', 2, '+91-22-1234-5605', 'gynecology@hospital.com'),
('Dermatology', 'Skin, hair, and nail conditions', 1, '+91-22-1234-5606', 'dermatology@hospital.com'),
('Ophthalmology', 'Eye care and vision', 3, '+91-22-1234-5607', 'eye@hospital.com'),
('ENT', 'Ear, Nose, and Throat disorders', 1, '+91-22-1234-5608', 'ent@hospital.com'),
('General Medicine', 'Primary care and general health', 1, '+91-22-1234-5609', 'general@hospital.com'),
('Oncology', 'Cancer diagnosis and treatment', 5, '+91-22-1234-5610', 'oncology@hospital.com');

-- ============================================
-- SEED DATA - Doctors
-- ============================================
INSERT INTO doctors (department_id, first_name, last_name, specialization, qualification, experience_years, phone, email, consultation_fee, languages_spoken) VALUES
((SELECT id FROM departments WHERE name='Cardiology'), 'Rajesh', 'Sharma', 'Interventional Cardiologist', 'MBBS, MD, DM Cardiology', 15, '+91-98765-43201', 'dr.sharma@hospital.com', 1500.00, ARRAY['English', 'Hindi', 'Marathi']),
((SELECT id FROM departments WHERE name='Cardiology'), 'Priya', 'Menon', 'Cardiac Electrophysiologist', 'MBBS, MD, DM Cardiology', 10, '+91-98765-43202', 'dr.menon@hospital.com', 1200.00, ARRAY['English', 'Hindi', 'Malayalam']),
((SELECT id FROM departments WHERE name='Orthopedics'), 'Amit', 'Patel', 'Joint Replacement Surgeon', 'MBBS, MS Orthopedics', 18, '+91-98765-43203', 'dr.patel@hospital.com', 1000.00, ARRAY['English', 'Hindi', 'Gujarati']),
((SELECT id FROM departments WHERE name='Neurology'), 'Sunita', 'Gupta', 'Neurologist', 'MBBS, MD Neurology', 12, '+91-98765-43204', 'dr.gupta@hospital.com', 1300.00, ARRAY['English', 'Hindi']),
((SELECT id FROM departments WHERE name='Pediatrics'), 'Rahul', 'Joshi', 'Pediatrician', 'MBBS, MD Pediatrics', 8, '+91-98765-43205', 'dr.joshi@hospital.com', 800.00, ARRAY['English', 'Hindi', 'Marathi']),
((SELECT id FROM departments WHERE name='Gynecology'), 'Kavita', 'Nair', 'Obstetrician & Gynecologist', 'MBBS, MS Gynecology', 14, '+91-98765-43206', 'dr.nair@hospital.com', 1100.00, ARRAY['English', 'Hindi', 'Malayalam']),
((SELECT id FROM departments WHERE name='Dermatology'), 'Vikram', 'Singh', 'Dermatologist', 'MBBS, MD Dermatology', 9, '+91-98765-43207', 'dr.singh@hospital.com', 900.00, ARRAY['English', 'Hindi', 'Punjabi']),
((SELECT id FROM departments WHERE name='General Medicine'), 'Anita', 'Desai', 'General Physician', 'MBBS, MD Medicine', 11, '+91-98765-43208', 'dr.desai@hospital.com', 700.00, ARRAY['English', 'Hindi', 'Marathi']);

-- Doctor Schedules (Mon-Fri for all doctors)
INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes)
SELECT id, generate_series(1,5), '09:00', '17:00', 30
FROM doctors;

-- Weekend partial schedules
INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes)
SELECT id, 6, '09:00', '13:00', 30
FROM doctors
WHERE id IN (SELECT id FROM doctors LIMIT 4);

-- ============================================================
-- Source: database\auth_schema.sql
-- ============================================================

-- ============================================================
-- AUTH & MULTI-TENANT SCHEMA ADDITIONS
-- Run this AFTER the existing schema.sql
-- ============================================================

-- ============================================================
-- HOSPITALS TABLE (one per organisation)
-- ============================================================
CREATE TABLE hospitals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,         -- URL-safe unique identifier
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(200),
    logo_url TEXT,
    description TEXT,
    license_number VARCHAR(100),
    registration_number VARCHAR(100),
    clinical_establishment_registration_number VARCHAR(100),
    municipal_license_number VARCHAR(100),
    pharmacy_license_number VARCHAR(100),
    laboratory_license_number VARCHAR(100),
    gst_number VARCHAR(30),
    pan_number VARCHAR(20),
    owner_director_name VARCHAR(150),
    verification_status VARCHAR(30) DEFAULT 'PENDING' NOT NULL,
    verification_notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- USERS TABLE (hospital admins & staff)
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id UUID REFERENCES hospitals(id) ON DELETE CASCADE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF' CHECK (
        role IN ('SUPER_ADMIN', 'HOSPITAL_ADMIN', 'STAFF', 'RECEPTIONIST')
    ),
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- ADD hospital_id TO ALL EXISTING TABLES (multi-tenancy)
-- ============================================================
ALTER TABLE departments ADD COLUMN IF NOT EXISTS hospital_id UUID REFERENCES hospitals(id) ON DELETE CASCADE;
ALTER TABLE doctors     ADD COLUMN IF NOT EXISTS hospital_id UUID REFERENCES hospitals(id) ON DELETE CASCADE;
ALTER TABLE patients    ADD COLUMN IF NOT EXISTS hospital_id UUID REFERENCES hospitals(id) ON DELETE CASCADE;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS hospital_id UUID REFERENCES hospitals(id) ON DELETE CASCADE;
ALTER TABLE enquiries   ADD COLUMN IF NOT EXISTS hospital_id UUID REFERENCES hospitals(id) ON DELETE CASCADE;

-- ============================================================
-- INDEXES for tenant isolation (fast lookups by hospital)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_departments_hospital ON departments(hospital_id);
CREATE INDEX IF NOT EXISTS idx_doctors_hospital     ON doctors(hospital_id);
CREATE INDEX IF NOT EXISTS idx_patients_hospital    ON patients(hospital_id);
CREATE INDEX IF NOT EXISTS idx_appointments_hospital ON appointments(hospital_id);
CREATE INDEX IF NOT EXISTS idx_enquiries_hospital   ON enquiries(hospital_id);
CREATE INDEX IF NOT EXISTS idx_users_hospital       ON users(hospital_id);
CREATE INDEX IF NOT EXISTS idx_users_email          ON users(email);

-- ============================================================
-- UPDATED_AT TRIGGERS
-- ============================================================
CREATE TRIGGER update_hospitals_updated_at BEFORE UPDATE ON hospitals FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_users_updated_at     BEFORE UPDATE ON users     FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- ROW LEVEL SECURITY for new tables
-- ============================================================
ALTER TABLE hospitals ENABLE ROW LEVEL SECURITY;
ALTER TABLE users     ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public can read active hospitals" ON hospitals FOR SELECT USING (is_active = TRUE);
CREATE POLICY "Allow hospital insert"            ON hospitals FOR INSERT WITH CHECK (TRUE);
CREATE POLICY "Allow hospital update"            ON hospitals FOR UPDATE USING (TRUE);
CREATE POLICY "Allow user operations"            ON users FOR ALL USING (TRUE);

-- ============================================================
-- Source: database\prescription_schema.sql
-- ============================================================

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
    follow_up_after_days INTEGER,
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
-- COMMON MEDICINES (hospital catalog)
-- ============================================================
CREATE TABLE common_medicines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(hospital_id, name)
);

-- ============================================================
-- COMMON TESTS (hospital catalog)
-- ============================================================
CREATE TABLE common_tests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(hospital_id, name)
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_prescriptions_patient    ON prescriptions(patient_id);
CREATE INDEX idx_common_medicines_hospital ON common_medicines(hospital_id);
CREATE INDEX idx_common_tests_hospital    ON common_tests(hospital_id);
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
ALTER TABLE common_medicines ENABLE ROW LEVEL SECURITY;
ALTER TABLE common_tests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow prescriptions"        ON prescriptions FOR ALL USING (TRUE);
CREATE POLICY "Allow prescription_meds"    ON prescription_medicines FOR ALL USING (TRUE);
CREATE POLICY "Allow lab_tests"            ON lab_tests FOR ALL USING (TRUE);
CREATE POLICY "Allow patient_profiles"     ON patient_medical_profiles FOR ALL USING (TRUE);
CREATE POLICY "Allow common_medicines"     ON common_medicines FOR ALL USING (TRUE);
CREATE POLICY "Allow common_tests"         ON common_tests FOR ALL USING (TRUE);

-- ============================================================
-- Source: database\seed_dummy_data.sql
-- ============================================================

-- ============================================
-- DUMMY DATA SEED - Departments + Medicines + Labs
-- Run AFTER prescription_schema.sql (includes common_medicines, common_tests)
-- ============================================

-- ============================================
-- 1. DEPARTMENTS (10)
-- ============================================
INSERT INTO departments (name, description, floor_number, phone, email, hospital_id) VALUES
('Cardiology', 'Heart and cardiovascular system care', 3, '+91-22-1234-5601', 'cardiology@hospital.com', NULL),
('Orthopedics', 'Bone, joint, and muscle disorders', 2, '+91-22-1234-5602', 'ortho@hospital.com', NULL),
('Neurology', 'Brain and nervous system disorders', 4, '+91-22-1234-5603', 'neuro@hospital.com', NULL),
('Pediatrics', 'Medical care for infants, children and adolescents', 1, '+91-22-1234-5604', 'pediatrics@hospital.com', NULL),
('Gynecology', 'Women health and reproductive system', 2, '+91-22-1234-5605', 'gynecology@hospital.com', NULL),
('Dermatology', 'Skin, hair, and nail conditions', 1, '+91-22-1234-5606', 'dermatology@hospital.com', NULL),
('Ophthalmology', 'Eye care and vision', 3, '+91-22-1234-5607', 'eye@hospital.com', NULL),
('ENT', 'Ear, Nose, and Throat disorders', 1, '+91-22-1234-5608', 'ent@hospital.com', NULL),
('General Medicine', 'Primary care and general health', 1, '+91-22-1234-5609', 'general@hospital.com', NULL),
('Oncology', 'Cancer diagnosis and treatment', 5, '+91-22-1234-5610', 'oncology@hospital.com', NULL)
ON CONFLICT DO NOTHING;

-- ============================================
-- 2. COMMON MEDICINES (20 samples)
-- ============================================
INSERT INTO common_medicines (hospital_id, name) VALUES
('988942e7-4635-463c-9d28-df881f2acda0', 'Paracetamol 500mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Ibuprofen 400mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Amlodipine 5mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Metformin 500mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Atorvastatin 20mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Atenolol 50mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Losartan 50mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Levothyroxine 100mcg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Omeprazole 20mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Sertraline 50mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Amoxicillin 500mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Azithromycin 500mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Pantoprazole 40mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Vitamin D3 60K IU'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Calcium Carbonate 500mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Folic Acid 5mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Iron 100mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Cetirizine 10mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Hydrochlorothiazide 25mg'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Clopidogrel 75mg')
ON CONFLICT (hospital_id, name) DO NOTHING;

-- ============================================
-- 3. COMMON TESTS / LABS (15 samples)
-- ============================================
INSERT INTO common_tests (hospital_id, name) VALUES
('988942e7-4635-463c-9d28-df881f2acda0', 'Complete Blood Count (CBC)'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Lipid Profile'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Liver Function Test (LFT)'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Kidney Function Test (KFT)'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Thyroid Profile (T3, T4, TSH)'),
('988942e7-4635-463c-9d28-df881f2acda0', 'HbA1c (Diabetes)'),
('988942e7-4635-463c-9d28-df881f2acda0', 'ESR / CRP'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Urine Routine'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Blood Sugar Fasting / PP'),
('988942e7-4635-463c-9d28-df881f2acda0', 'ECG'),
('988942e7-4635-463c-9d28-df881f2acda0', 'X-Ray Chest PA'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Ultrasound Abdomen'),
('988942e7-4635-463c-9d28-df881f2acda0', 'Vitamin D (25 OH)'),
('988942e7-4635-463c-9d28-df881f2acda0', 'PSA (Prostate)'),
('988942e7-4635-463c-9d28-df881f2acda0', 'HIV / Hepatitis Panel')
ON CONFLICT (hospital_id, name) DO NOTHING;

-- ============================================
-- VERIFY ALL
-- ============================================
SELECT 'Departments: ' || COUNT(*) FROM departments
UNION ALL SELECT 'Medicines: ' || COUNT(*) FROM common_medicines
UNION ALL SELECT 'Tests/Labs: ' || COUNT(*) FROM common_tests;


-- ============================================================
-- Source: database\migrations\add_follow_up_after_days.sql
-- ============================================================

-- Add follow_up_after_days to prescriptions (0 = no follow-up required)
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS follow_up_after_days INTEGER;

-- ============================================================
-- Source: database\migrations\V20260611__create_consultation_payments.sql
-- ============================================================

-- Consultation payments for consultation fee receipt printing

CREATE TABLE IF NOT EXISTS consultation_payments (
    id UUID PRIMARY KEY,

    hospital_id UUID NOT NULL,
    appointment_id UUID NOT NULL,
    patient_id UUID NOT NULL,

    consultation_fee NUMERIC(10,2) NOT NULL,

    payment_mode VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(120),

    amount_paid NUMERIC(10,2) NOT NULL,
    paid_at TIMESTAMP NOT NULL,

    received_by_user_id UUID,
    received_by_name VARCHAR(150) NOT NULL,

    created_at TIMESTAMP,

    CONSTRAINT fk_consultation_payments_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id),

    CONSTRAINT fk_consultation_payments_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(id),

    CONSTRAINT fk_consultation_payments_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id),

    CONSTRAINT fk_consultation_payments_received_by
        FOREIGN KEY (received_by_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_consultation_payments_hospital_patient_paid_at
    ON consultation_payments (hospital_id, patient_id, paid_at DESC);


-- ============================================================
-- Source: database\migrations\V20260611__create_consultation_receipts.sql
-- ============================================================

-- Consultation receipts for consultation fee receipt printing

CREATE TABLE IF NOT EXISTS consultation_receipts (
    id UUID PRIMARY KEY,

    hospital_id UUID NOT NULL,
    consultation_payment_id UUID NOT NULL,

    receipt_number VARCHAR(60) NOT NULL,
    receipt_date_time TIMESTAMP NOT NULL,

    hospital_name VARCHAR(150) NOT NULL,
    hospital_address TEXT,
    hospital_phone VARCHAR(25),

    patient_name VARCHAR(200) NOT NULL,
    patient_identifier VARCHAR(120) NOT NULL,

    doctor_name VARCHAR(200) NOT NULL,
    department_name VARCHAR(200) NOT NULL,

    consultation_fee NUMERIC(10,2) NOT NULL,

    payment_mode VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(120),

    amount_paid NUMERIC(10,2) NOT NULL,

    received_by_name VARCHAR(150) NOT NULL,

    stamp_placeholder VARCHAR(200),

    created_at TIMESTAMP,

    CONSTRAINT uq_consultation_receipts_hospital_receipt_number
        UNIQUE (hospital_id, receipt_number),

    CONSTRAINT fk_consultation_receipts_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id),

    CONSTRAINT fk_consultation_receipts_consultation_payment
        FOREIGN KEY (consultation_payment_id) REFERENCES consultation_payments(id)
);

CREATE INDEX IF NOT EXISTS idx_consultation_receipts_hospital_receipt_number
    ON consultation_receipts (hospital_id, receipt_number);

CREATE INDEX IF NOT EXISTS idx_consultation_receipts_hospital_payment
    ON consultation_receipts (hospital_id, consultation_payment_id, receipt_date_time DESC);


-- ============================================================
-- Source: database\migrations\V20260616__create_consultation_receipt_line_items.sql
-- ============================================================

-- Creates snapshot line-items table for staff-entered consultation receipts

create table if not exists consultation_receipt_line_items (
    id uuid primary key,
    receipt_id uuid not null,
    sr_no integer not null,
    particulars varchar(250) not null,
    amount numeric(10,2) not null,

    constraint fk_consultation_receipt_line_items_receipt
        foreign key (receipt_id) references consultation_receipts(id)
        on delete cascade
);

create index if not exists idx_consultation_receipt_line_items_receipt_id
    on consultation_receipt_line_items(receipt_id);



-- ============================================================
-- Source: database\migrations\V20260617__create_consultation_payment_line_items.sql
-- ============================================================

-- Snapshot line items entered during consultation payment creation
-- for later receipt/PDF printing.

create table if not exists consultation_payment_line_items (
    id uuid primary key,
    payment_id uuid not null,
    sr_no integer not null,
    particulars varchar(250) not null,
    amount numeric(10,2) not null,

    constraint fk_consultation_payment_line_items_payment
        foreign key (payment_id) references consultation_payments(id)
        on delete cascade
);

create index if not exists idx_consultation_payment_line_items_payment_id
    on consultation_payment_line_items(payment_id);


-- ============================================================
-- Source: database\migrations\V20260618__extend_consultation_receipts_status_void.sql
-- ============================================================

-- Extends consultation receipts for voiding and status management

ALTER TABLE consultation_receipts
    ADD COLUMN IF NOT EXISTS receipt_status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL,
    ADD COLUMN IF NOT EXISTS voided_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS voided_by UUID;

-- Convenience index for reporting/filtering
CREATE INDEX IF NOT EXISTS idx_consultation_receipts_voided_at ON consultation_receipts(voided_at);
CREATE INDEX IF NOT EXISTS idx_consultation_receipts_receipt_status ON consultation_receipts(receipt_status);

-- Optional foreign key if users table exists (in your schema it does via auth_schema.sql)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_consultation_receipts_voided_by'
        ) THEN
            ALTER TABLE consultation_receipts
                ADD CONSTRAINT fk_consultation_receipts_voided_by
                FOREIGN KEY (voided_by) REFERENCES users(id);
        END IF;
    END IF;
END $$;

