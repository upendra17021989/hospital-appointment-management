# Add Age Field to Patient Registration and Related Pages

## Overview
Add `age` field (Integer) alongside `dateOfBirth` in:
- Database schema
- Backend (model, DTOs, service)
- Frontend: PatientForm.jsx, BookAppointment.jsx (new patient), PatientDetails.jsx (list/edit/display)

Age is direct input (validated 0-150), kept alongside DOB for quick reference (no auto-calc in DB/backend).

## Steps

### 1. [x] Update database/schema.sql
- Add `age INTEGER CHECK (age >= 0 AND age <= 150)` to patients table (after date_of_birth)

### 2. [x] Update backend/src/main/java/com/hospital/model/Patient.java
- Add `@Column(name = "age") private Integer age;`
- Add getter/setter if needed (Lombok handles)

### 3. [x] Update backend/src/main/java/com/hospital/dto/Dtos.java
- PatientRequest: Add `private Integer age;`
- PatientResponse: Add `private Integer age;`
- Update builders/mappers

### 4. [x] Update backend/src/main/java/com/hospital/service/PatientService.java
- registerPatient/updatePatient: Set `patient.setAge(request.getAge());`
- mapToResponse: `.age(p.getAge())`

### 5. [x] Update frontend/src/pages/PatientForm.jsx
- Add age input (number, min=0 max=150) near DOB
- Include age in payload to /patients

### 6. [x] Update frontend/src/pages/BookAppointment.jsx
- Step4PatientInfo (new patient mode): Add age input near DOB
- Include in new patient POST

### 7. [x] Update frontend/src/pages/PatientDetails.jsx
- List table: Add Age column after Gender
- Detail view: Use patient.age || calcAge(patient.dateOfBirth)
- Edit tab: Add age input, handle in PUT

### 8. [ ] Test & Followup
- Restart backend: `mvn spring-boot:run` (in backend/)
- Test PatientForm registration (check DB)
- Test BookAppointment new patient
- Test PatientDetails list/edit/display
- Verify Supabase: Run ALTER TABLE if needed (manual)

## Progress Tracking
- Mark [x] when complete
- Update after each file

