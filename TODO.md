# 🧾 TODO - Consultation Receipt Management Module

## Backend
- [ ] Implement `ConsultationReceiptService` full CRUD/search/view/update/void + dashboard + reporting aggregates
- [ ] Enforce void rule for PDF download/reprint
- [ ] Implement export endpoints (Excel/PDF/CSV) under `/consultation-receipts/reports/*`
- [x] Update task tracking file (this TODO)

- [ ] Expand `ConsultationReceiptController` to full REST API surface
- [ ] Add validation + RBAC checks per role

## Frontend
- [ ] Implement `frontend/src/pages/ConsultationReceipts.jsx` dashboard/list/table UI
- [ ] Create `frontend/src/pages/ConsultationReceiptForm.jsx`
- [ ] Create `frontend/src/pages/ConsultationReceiptViewer.jsx`
- [ ] Add Receipt History tab in `frontend/src/pages/PatientDetail.jsx`
- [ ] Extend `frontend/src/services/receiptApi.js` with REST calls + exports

## Verification
- [ ] Backend compile + run
- [ ] Frontend build
- [ ] Manual QA by role (Receptionist/Accountant/Admin)

