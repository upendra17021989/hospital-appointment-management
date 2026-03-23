# 🏥 MediCare+ Hospital Management System

A full-stack Hospital Enquiry & Appointment Management System built with **React**, **Java Spring Boot**, and **Supabase (PostgreSQL)**.

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    React Frontend                        │
│              (Vite + React 18, Port 5173)               │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP REST (JSON)
┌──────────────────────▼──────────────────────────────────┐
│              Java Spring Boot Backend                    │
│           (Spring Boot 3.2, Port 8080)                  │
│                                                          │
│  Controllers → Services → Repositories → JPA Entities  │
└──────────────────────┬──────────────────────────────────┘
                       │ JDBC (PostgreSQL driver)
┌──────────────────────▼──────────────────────────────────┐
│                  Supabase Database                       │
│              (PostgreSQL 15, Cloud)                      │
│                                                          │
│  departments | doctors | doctor_schedules               │
│  patients | appointments | enquiries | logs             │
└─────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Step 1 — Set Up Supabase

1. Go to [supabase.com](https://supabase.com) → Create a new project
2. Navigate to **SQL Editor** → **New Query**
3. Paste and run the entire contents of `database/schema.sql`
4. Note your project details from **Settings → Database**:
   - **Host**: `db.YOUR_PROJECT_REF.supabase.co`
   - **Password**: Your database password
   - **Port**: `5432`

### Step 2 — Configure & Run Backend

```bash
cd backend
```

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=YOUR_SUPABASE_DB_PASSWORD
```

Run the application:
```bash
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

Backend will start at **http://localhost:8080**  
Swagger UI: **http://localhost:8080/api/swagger-ui.html**

### Step 3 — Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend will start at **http://localhost:5173**

---

## 📁 Project Structure

```
hospital-system/
│
├── database/
│   └── schema.sql                    # Full Supabase schema + seed data
│
├── backend/
│   ├── pom.xml                       # Maven dependencies
│   └── src/main/
│       ├── resources/
│       │   └── application.properties
│       └── java/com/hospital/
│           ├── HospitalManagementApplication.java
│           ├── model/
│           │   ├── Department.java
│           │   ├── Doctor.java
│           │   ├── DoctorSchedule.java
│           │   ├── Patient.java
│           │   ├── Appointment.java
│           │   ├── AppointmentLog.java
│           │   └── Enquiry.java
│           ├── dto/
│           │   └── Dtos.java         # All request/response DTOs
│           ├── repository/
│           │   └── Repositories.java # All JPA repositories
│           ├── service/
│           │   ├── AppointmentService.java
│           │   ├── PatientService.java
│           │   ├── EnquiryService.java
│           │   └── DoctorDepartmentService.java
│           ├── controller/
│           │   └── Controllers.java  # All REST controllers
│           └── config/
│               ├── CorsConfig.java
│               ├── OpenApiConfig.java
│               └── GlobalExceptionHandler.java
│
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.jsx
        └── App.jsx                   # Complete SPA
```

---

## 🗄️ Database Schema

### Tables

| Table | Description |
|-------|-------------|
| `departments` | Hospital departments (Cardiology, Ortho, etc.) |
| `doctors` | Doctor profiles with specialization & fees |
| `doctor_schedules` | Weekly availability per doctor |
| `patients` | Patient registration data |
| `appointments` | Appointment bookings with token numbers |
| `enquiries` | Patient/public enquiries |
| `appointment_logs` | Audit trail for status changes |

### Auto-Generated Features
- **Token Numbers**: Auto-generated on appointment creation (`TKN-20260319-001`)
- **Audit Logs**: Status changes automatically logged
- **Timestamps**: `created_at` / `updated_at` auto-managed

### Seed Data Included
- 10 Departments (Cardiology, Orthopedics, Neurology, Pediatrics, etc.)
- 8 Doctors with full profiles and weekly schedules

---

## 🔌 REST API Reference

Base URL: `http://localhost:8080/api`

### Departments
```
GET  /departments          → List all active departments
GET  /departments/{id}     → Get department by ID
```

### Doctors
```
GET  /doctors                         → All available doctors
GET  /doctors?departmentId={id}       → Filter by department
GET  /doctors?search={query}          → Search by name/specialization
GET  /doctors/{id}                    → Doctor profile
GET  /doctors/{id}/slots?date={date}  → Available time slots
```

### Patients
```
POST /patients              → Register new patient
GET  /patients/{id}         → Get patient by ID
GET  /patients/search?phone={phone}  → Search by phone
PUT  /patients/{id}         → Update patient info
```

### Appointments
```
POST   /appointments               → Book appointment
GET    /appointments               → All appointments
GET    /appointments?date={date}   → Filter by date
GET    /appointments?doctorId={id} → Filter by doctor
GET    /appointments?patientId={id}→ Filter by patient
GET    /appointments/{id}          → Appointment details
PATCH  /appointments/{id}/status   → Update status
```

#### Appointment Status Flow
```
pending → confirmed → completed
       ↘ cancelled
       ↘ no_show
       ↘ rescheduled
```

### Enquiries
```
POST  /enquiries              → Submit enquiry
GET   /enquiries              → Active enquiries
GET   /enquiries?status={s}   → Filter by status
GET   /enquiries/{id}         → Enquiry details
PATCH /enquiries/{id}/status  → Update status
```

### Dashboard
```
GET /dashboard/stats          → Summary statistics
```

---

## 🎨 Frontend Pages

| Page | Route (SPA) | Description |
|------|-------------|-------------|
| Dashboard | `dashboard` | Stats + today's appointments |
| Book Appointment | `book` | 5-step booking wizard |
| All Appointments | `appointments` | Full list with filters & actions |
| Enquiries | `enquiries` | Submit & manage enquiries |
| Doctors | `doctors` | Search/filter doctor directory |
| Departments | `departments` | Department cards with info |

### Booking Wizard Steps
1. **Select Department** — Visual department cards
2. **Select Doctor** — Doctor cards with fee, experience, languages
3. **Date & Time** — Calendar + available time slots (30-min slots)
4. **Patient Information** — Registration form
5. **Review & Confirm** — Summary + token generation

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Language |
| Spring Boot | 3.2.3 | Framework |
| Spring Data JPA | 3.2.3 | ORM |
| Hibernate | 6.x | JPA Implementation |
| PostgreSQL Driver | Latest | DB Connection |
| Lombok | Latest | Boilerplate reduction |
| Springdoc OpenAPI | 2.3.0 | Swagger UI |
| HikariCP | Built-in | Connection pooling |

### Frontend
| Technology | Version | Purpose |
|-----------|---------|---------|
| React | 18.3 | UI Framework |
| Vite | 5.4 | Build tool |
| Vanilla CSS | — | Styling (no UI library) |
| Google Fonts | — | Playfair Display + DM Sans |

### Database
| Technology | Purpose |
|-----------|---------|
| Supabase | Managed PostgreSQL hosting |
| PostgreSQL 15 | Database |
| Row Level Security | Access control |

---

## ⚙️ Configuration Reference

### application.properties
```properties
# Database (Supabase)
spring.datasource.url=jdbc:postgresql://db.YOUR_REF.supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

# JPA
spring.jpa.hibernate.ddl-auto=validate   # Use 'create' first time if not running schema.sql
spring.jpa.show-sql=false

# Server
server.port=8080
server.servlet.context-path=/api

# CORS (add your frontend URL)
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

---

## 🔒 Security Notes

1. **Row Level Security (RLS)** is enabled on all Supabase tables
2. The current setup allows public read/write — add authentication for production
3. For production, use **Spring Security + JWT** for the backend
4. Store database passwords in environment variables, never in code:
   ```bash
   export DB_PASSWORD=your_password
   ```
   ```properties
   spring.datasource.password=${DB_PASSWORD}
   ```

---

## 🧪 Testing the API

Using curl:
```bash
# Get all departments
curl http://localhost:8080/api/departments

# Get doctors in Cardiology
curl "http://localhost:8080/api/doctors?departmentId=DEPT_UUID"

# Register a patient
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Raj","lastName":"Sharma","phone":"+919876543210","email":"raj@example.com","gender":"male"}'

# Book appointment
curl -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{"patientId":"PATIENT_UUID","doctorId":"DOCTOR_UUID","appointmentDate":"2026-03-25","appointmentTime":"10:00","reasonForVisit":"Chest pain consultation"}'

# Get available slots
curl "http://localhost:8080/api/doctors/DOCTOR_UUID/slots?date=2026-03-25"

# Dashboard stats
curl http://localhost:8080/api/dashboard/stats
```

---

## 🚀 Production Deployment

### Backend — Deploy to Railway / Render / AWS EC2
1. Build: `mvn clean package -DskipTests`
2. JAR is at `target/hospital-management-1.0.0.jar`
3. Run: `java -jar hospital-management-1.0.0.jar`
4. Set environment variables for DB credentials

### Frontend — Deploy to Vercel / Netlify
1. Build: `npm run build`
2. Deploy the `dist/` folder
3. Set `VITE_API_BASE=https://your-backend-url.com/api`

---

## 📞 Support

For technical issues, check Swagger UI at `/api/swagger-ui.html` or review server logs.
