# Common Medicines & Tests (Hospital-specific)

## Confirmed: Hospital-scoped (doctors see only their hospital's catalog)

## Steps
1. ✅ Create TODO.md
2. ✅ Update database/prescription_schema.sql → add `common_medicines`, `common_tests` tables
3. ✅ Create models: CommonMedicine.java, CommonTest.java
4. ✅ Create repos: CommonMedicineRepo.java, CommonTestRepo.java
5. ✅ Edit PrescriptionController.java (endpoints + upsert)
   * Repos injected
   * GET/POST /common-medicines/hospital, /common-tests/hospital
   * Upsert before prescription save
6. ✅ Edit PrescriptionForm.jsx 
   * useState commonMedicines/commonTests
   * useEffect fetch from /prescriptions/common-*/hospital
   * Dynamic quick-add buttons (slice 12 + more indicator)
   * Removed hardcoded arrays
7. ☐ Update TODO.md
8. ☐ Test/DB migrate/backend restart

## Details
- Tables: hospital_id, name (UNIQUE per hospital)
- POST /prescriptions: upsert new med/test names to common tables
- GET /common-medicines/hospital, /common-tests/hospital → list for quick-add
- Frontend: replace hardcoded arrays
