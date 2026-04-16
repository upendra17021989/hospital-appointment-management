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

