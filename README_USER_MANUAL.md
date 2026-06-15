# MediCare+ User Manual

> Covers both **hospital staff/admin workflows** (roles-based pages) and **patient workflows** (booking appointments through the UI).

## 1. Overview
MediCare+ is a hospital management system that supports:
- Appointment booking (department → doctor → date/time → patient info → confirmation)
- Appointment lifecycle management (pending/confirmed/completed/cancelled)
- Patient data management
- Enquiry tracking
- Billing: payment collection and receipt PDF generation
- Reports: patient visit statistics and downloads

## 2. How to access the application
- **Frontend (React)** runs as a single-page application (SPA).
- **Backend** is a Spring Boot API.

Open the frontend in a browser (the app redirects you to the appropriate route).

## 3. Authentication
### 3.1 Hospital Registration (Signup)
Route: **`/signup`**

Signup is a **3-step wizard**:
1. **Hospital Info**
   - Hospital Name*
   - City*
   - Hospital Phone*
   - (Optional) Address, State, Hospital Email, Website, License Number
2. **Admin Account**
   - Admin first name*, last name*
   - Admin email*
   - Password* (minimum 8 chars) and Confirm password
3. **Review & Confirm**
   - Shows hospital and admin details
   - On confirmation, creates the hospital workspace and admin account

### 3.2 Hospital Login
Route: **`/login`**
- Enter Email and Password
- On success, you are redirected to **`/dashboard`**.

### 3.3 Roles & protected pages (what staff can see)
The app uses role-based guards (`RoleGuard`) and protects routes. The following routes are explicitly guarded in the SPA:
- **Hospital Admin / Super Admin**: 
  - `/doctor-management`
  - `/user-management`
  - `/patient-form`
  - `/patients`
  - `/patients/:id`
  - `/prescription-form`
  - `/subscription-plans`
  - `/billing-history`
  - `/payment-receipt`
  - `/reports`
- **Staff / Receptionist (as allowed)**:
  - `/patient-form`
  - `/patients`
  
> Appointment booking pages (`/book-appointment`, `/appointments`, `/enquiries`, `/doctors`, `/departments`) are also protected, but with different role allowances (based on the route config).

## 4. Patient Workflow (Booking an appointment)
Route: **`/book-appointment`**

### Booking Wizard Steps (5 steps)
The UI guides you through:
1. **Select Department**
   - Click a department card
   - Continue with **Next →**

2. **Select Doctor**
   - Doctor cards show name, specialization, experience, qualification, fee, and languages (when available)
   - Continue with **Next →**

3. **Select Date & Time**
   - Pick an **Appointment Date**
   - The system fetches available time slots for the selected doctor and date
   - Choose one slot (disabled slots cannot be selected)
   - If no slots exist, the UI shows an info message

4. **Patient Information**
   - Toggle between:
     - **👤 New Patient**
     - **🔍 Existing Patient**

   **Phone validation (New Patient mode)**
   - Accepts Indian mobile formats such as:
     - `9876543210`
     - `+919876543210`
     - `91 98765 43210`
     - `098765 43210`
   - The UI normalizes phone to `+91XXXXXXXXXX` before sending to the backend.

   **Existing Patient mode**
   - Search for patient (requires **2+ characters**)
   - Click a patient result to populate patient fields

   **Visit fields**
   - **Reason for Visit*** (required)
   - **Symptoms** (optional)
   - **Appointment Type**: in person / virtual / follow up / emergency (IPD)

   Continue with **Review →**

5. **Confirm**
   - Review department, doctor, fee, date, time, and patient details
   - Click **✓ Confirm Appointment**

### After confirmation
- A success screen shows the **token number** and appointment details.
- You can click **Book Another** to restart.

## 5. Staff/Admin Workflows

### 5.1 Dashboard (overview)
Route: **`/dashboard`**
- Summary dashboard for the hospital.
- (Exact cards/widgets depend on implementation.)

### 5.2 Departments
Route: **`/departments`**
- View department cards.

### 5.3 Doctors
Route: **`/doctors`**
- View/search doctors (based on the page implementation).

### 5.4 Appointments management
Route: **`/appointments`**

#### Filtering
- **Date filter** (optional): uses a date input
- **Status tabs**: All / Pending / Confirmed / Completed / Cancelled

#### Sorting & Pagination
- Table supports sorting by columns (Token, Patient, Doctor, Department, Date, Time, Type, Status)
- Uses pagination controls (page number + page size selector)

#### Appointment status actions (per row)
- **Pending** → **Confirm**
- **Confirmed** → **Complete**
- **Pending / Confirmed** → **Cancel**

#### Collect consultation payment & print receipt
When an appointment is **confirmed**, staff can open a payment modal:
- **Payment Mode**: CASH / UPI / CARD / NET_BANKING / OTHER
- **Amount Paid**
- **Payment Reference** (optional)

Then choose one:
- **Print Receipt (PDF)**
- **Save & Print Receipt (PDF)** and optionally mark appointment **completed**

### 5.5 Billing Receipt Viewer
Route: **`/payment-receipt`**

This page displays a generated **Hospital Consultation Receipt** with:
- Receipt No
- Paid date/time
- Patient / Doctor / Department
- Amount paid
- Payment mode
- “Received By”

Actions:
1. **Print (Browser)**
   - Uses `window.print()` for printing.
2. **Save & Print Receipt (PDF)**
   - Downloads a PDF generated by backend using the payment id.
3. **Reprint Receipt (PDF)**
   - Downloads a PDF using the receipt number.

### 5.6 Billing History
Route: **`/billing-history`**
- View previously created billing/payment records (table/list view).

### 5.7 Reports (patient visit statistics)
Route: **`/reports`**

#### Generate reports
1. Choose **Start Date** and **End Date**
2. Click **Generate**

#### Tabs / Views
The UI supports these report perspectives:
- **All Patients**
- **Department Wise**
- **Doctor Wise**
- **Total OPD**
- **Total IPD**

#### Downloads
Depending on the active tab:
- **CSV**: supported for All Patients (and the UI provides a CSV download action)
- **PDF**: supported for all tab types (All / Department / Doctor / OPD / IPD)
- **Excel**: supported for **All Patients tab only**

The download actions call backend endpoints and save the returned file.

#### Drilldown behavior
- Clicking a department/doctor in the report can load drilldown data via additional API calls.
- Date range remains the same.

### 5.8 Enquiries
Route: **`/enquiries`**
- Submit and manage enquiries.
- (Exact fields and status transitions depend on the enquiry page implementation.)

### 5.9 Patient management
Routes:
- **`/patient-form`**
- **`/patients`**
- **`/patients/:id`**

Typical workflow:
1. Create or update patient profiles
2. Use patient records in the booking wizard (Existing Patient mode)

## 6. Common Troubleshooting
### Missing token / “Missing auth token”
- If the app fails to download PDFs/CSVs/Excels from Reports, log out and log back in.

### No available appointment slots
- Change the date in **Step 3**.
- Verify the selected doctor and department have availability.

### Phone number errors (New Patient mode)
- Use a valid Indian mobile format accepted by the UI:
  - `9876543210` or `+91 98765 43210` etc.

### PDF download fails
- Confirm you have completed payment steps (for receipt download actions that require payment id/receipt number).
- Ensure backend endpoints are reachable.

## 7. Admin Setup Notes (high level)
- Use **Signup** to create a hospital workspace and admin account.
- Ensure backend environment variables (database credentials, JWT secret, etc.) are configured.

---

## Appendix: Routes quick reference
- `/login` — Hospital login
- `/signup` — Hospital registration
- `/dashboard` — Dashboard
- `/book-appointment` — Patient/staff booking wizard
- `/appointments` — Appointment list + status updates + payment/receipt printing
- `/enquiries` — Enquiries
- `/doctors` — Doctor directory
- `/departments` — Department directory
- `/patient-form` — Patient form
- `/patients` — Patients list
- `/patients/:id` — Patient detail
- `/subscription-plans` — Subscription plans management
- `/billing-history` — Billing history
- `/payment-receipt` — Receipt viewer / PDF download
- `/reports` — Reports with CSV/PDF/Excel downloads
- `/doctor-management` — Doctor management (admin/super admin)
- `/user-management` — Staff accounts management (admin/super admin)

