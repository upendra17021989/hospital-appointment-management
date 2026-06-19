-- Add Doctor and Accountant as assignable hospital user roles

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('SUPER_ADMIN', 'HOSPITAL_ADMIN', 'STAFF', 'RECEPTIONIST', 'DOCTOR', 'ACCOUNTANT'));