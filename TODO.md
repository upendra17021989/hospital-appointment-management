# React Router Implementation TODO

## [x] 1. Install Dependencies
- [x] `cd frontend && npm install react-router-dom`

## [x] 2. Core Files
- [x] Update App.jsx: Add BrowserRouter, Routes/Route config, ProtectedRoute wrapper
- [x] Update Sidebar.jsx: Replace onClick with Link, use useLocation for active
- [x] Create/Update ProtectedRoute.jsx: Combine auth + RoleGuard

## [x] 3. Pages - Remove onNavigate prop, add useNavigate()
- [x] PatientDetails.jsx (high priority: multiple nav calls)
- [x] PrescriptionForm.jsx
- [x] PatientForm.jsx
- [x] Dashboard.jsx
- [x] BookAppointment.jsx
- [x] Others (Appointments, Enquiries, etc.)

## [x] 4. Auth & Testing
- [x] Update AuthContext.jsx: logout → navigate('/login')
- [x] Test all routes, nav, roles, auth redirects
- [x] `npm run dev` and verify

## [ ] 5. Final
- [ ] attempt_completion

