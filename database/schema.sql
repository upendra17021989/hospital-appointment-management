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
