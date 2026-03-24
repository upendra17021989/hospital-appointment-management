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
