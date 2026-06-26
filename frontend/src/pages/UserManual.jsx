import React from 'react';
import { Link } from 'react-router-dom';
import { PageHeader } from '../components/Common';

const sections = [
  {
    title: 'Getting Started',
    items: [
      ['Sign in', 'Open the login page, enter your registered email and password, then continue to the dashboard.'],
      ['Dashboard', 'Use the dashboard as the first overview of hospital activity and subscription notices.'],
      ['Navigation', 'Use the sidebar to move between appointments, patients, clinical tools, billing, reports, settings, and support.'],
    ],
  },
  {
    title: 'Appointments',
    items: [
      ['Book Appointment', 'Select department, doctor, date, time slot, patient details, visit reason, and confirm the appointment.'],
      ['Existing Patient Search', 'Search by patient name, phone, or address when booking for a returning patient.'],
      ['All Appointments', 'Filter appointments by date and status, then confirm, complete, cancel, or collect consultation payment where allowed.'],
      ['Receipt Flow', 'After collecting payment, print or download the consultation receipt PDF. Existing receipts can be reprinted from the receipt screen.'],
    ],
  },
  {
    title: 'Patients',
    items: [
      ['Register Patient', 'Create a patient profile with contact and medical details so it can be reused in appointment booking.'],
      ['Patient Records', 'Search patient records, open details, review appointment history, prescription history, receipt history, and related records.'],
      ['Patient Detail', 'Doctors and admins can open patient details to start a new prescription or inspect clinical history.'],
    ],
  },
  {
    title: 'Clinical',
    items: [
      ['Prescriptions', 'Doctors can write prescriptions, capture vitals, diagnosis, medicines, lab tests, follow-up instructions, and print the prescription.'],
      ['Medical Certificates', 'Create sick leave, fitness, fit-to-fly, vaccination, recovery, caretaker leave, and other supported certificates.'],
      ['Medicolegal Note', 'Printed prescriptions and certificates include the note that they are not for medicolegal purpose.'],
    ],
  },
  {
    title: 'Consultation Receipts',
    items: [
      ['Create Receipt', 'Create consultation fee receipts by selecting an appointment, entering amount, payment mode, and payment reference if available.'],
      ['Search Receipts', 'Filter by receipt number, patient, doctor, payment mode, status, or date range.'],
      ['Print and PDF', 'Use Print for direct printing or PDF to download a copy. Voided receipts cannot be printed as active receipts.'],
      ['Void Receipt', 'Admins or permitted billing users can void incorrect receipts while preserving the audit trail.'],
    ],
  },
  {
    title: 'Reports and Billing',
    items: [
      ['Reports', 'Generate patient visit reports for a selected date range, including all patients, department-wise, doctor-wise, OPD, and IPD views.'],
      ['Downloads', 'Download available report formats such as CSV, Excel, or PDF depending on the active report tab.'],
      ['Plans and Billing', 'Hospital admins can review subscription plans and billing history from the Billing & Plans section.'],
    ],
  },
  {
    title: 'Administration',
    items: [
      ['Manage Doctors', 'Hospital admins can add and maintain doctor profiles, departments, specializations, and availability-related details.'],
      ['Manage Users', 'Create and manage staff accounts with role-based access.'],
      ['Hospital Settings', 'Maintain hospital profile, receipt header details, logo, QR code, and document settings.'],
      ['Verification and Legal', 'Super admins can review hospital verification and legal records where enabled.'],
    ],
  },
  {
    title: 'Troubleshooting',
    items: [
      ['PDF download fails', 'Check that you are logged in, the backend is reachable, and the receipt or certificate has not been voided.'],
      ['No appointment slots', 'Try another date or confirm the selected doctor has availability configured.'],
      ['Patient search is empty', 'Enter at least two characters and confirm the patient belongs to the current hospital workspace.'],
      ['Access denied', 'Ask a hospital admin to confirm your user role has permission for that page.'],
    ],
  },
];

const quickLinks = [
  ['Dashboard', '/dashboard'],
  ['Book Appointment', '/book-appointment'],
  ['Appointments', '/appointments'],
  ['Patients', '/patients'],
  ['Prescriptions', '/prescription-form'],
  ['Medical Certificates', '/medical-certificates'],
  ['Consultation Receipts', '/consultation-receipts'],
  ['Reports', '/reports'],
  ['Hospital Settings', '/hospital-settings'],
];

const UserManual = () => {
  return (
    <div className="page-container">
      <PageHeader
        title="User Manual"
        subtitle="Step-by-step reference for using MediCare+ day to day"
      />

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-title">Quick Links</div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {quickLinks.map(([label, href]) => (
            <Link key={href} className="btn btn-secondary btn-sm" to={href}>
              {label}
            </Link>
          ))}
        </div>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
        gap: 16,
      }}>
        {sections.map((section) => (
          <section key={section.title} className="card" style={{ margin: 0 }}>
            <div className="card-title">{section.title}</div>
            <div style={{ display: 'grid', gap: 12 }}>
              {section.items.map(([heading, body]) => (
                <div key={heading}>
                  <div style={{ fontWeight: 800, color: 'var(--text)', marginBottom: 3 }}>
                    {heading}
                  </div>
                  <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.55 }}>
                    {body}
                  </div>
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
};

export default UserManual;
