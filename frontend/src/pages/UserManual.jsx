import React from 'react';
import { Link } from 'react-router-dom';
import { PageHeader } from '../components/Common';

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

const roles = [
  ['Receptionist / Staff', 'Register patients, book appointments, manage appointment flow, create certificates, and handle consultation receipts where permitted.'],
  ['Doctor', 'View patient history, write prescriptions, create medical certificates, and follow appointment clinical workflow.'],
  ['Accountant', 'Work mainly with consultation receipts, payment records, printing, and collection reports.'],
  ['Hospital Admin', 'Manage hospital setup, doctors, users, billing, reports, settings, and day-to-day operations.'],
  ['Super Admin', 'Review hospital verification, legal review, and platform-level administration pages.'],
];

const workflows = [
  {
    title: 'Front Desk Appointment Flow',
    steps: [
      'Open Book Appointment and select the department.',
      'Select the doctor and available appointment date.',
      'Choose an open time slot.',
      'Search existing patient or register a new patient from the booking form.',
      'Enter reason for visit, symptoms, appointment type, and confirm.',
      'From All Appointments, confirm the appointment when the patient arrives.',
      'Collect consultation payment and print the receipt when required.',
    ],
  },
  {
    title: 'Doctor Consultation Flow',
    steps: [
      'Open the appointment or patient record.',
      'Review patient details, appointment history, prescription history, and receipt history.',
      'Create a prescription with vitals, diagnosis, medicines, lab tests, instructions, and follow-up.',
      'Print the prescription after saving.',
      'Create a medical certificate only when clinically appropriate.',
      'Complete the appointment once the consultation is finished.',
    ],
  },
  {
    title: 'Billing and Receipt Flow',
    steps: [
      'Open Consultation Receipts or collect payment from an appointment.',
      'Select appointment, amount, payment mode, and reference number if available.',
      'Save the receipt and print or download the PDF.',
      'Use receipt filters to find older receipts by number, patient, doctor, payment mode, date range, or status.',
      'Void a receipt only for genuine mistakes, because the record remains in history.',
      'Use collection reports for daily or date-range reconciliation.',
    ],
  },
  {
    title: 'Admin Setup Flow',
    steps: [
      'Complete hospital profile and receipt branding from Hospital Settings.',
      'Create departments before adding doctors.',
      'Add doctors with specialization, qualification, and department details.',
      'Create staff users and assign the correct role.',
      'Check subscription plan and billing status.',
      'Upload verification or legal documents where required.',
    ],
  },
];

const sections = [
  {
    title: 'Dashboard',
    items: [
      ['Purpose', 'Use the dashboard for a fast overview of hospital activity, subscription alerts, and operational shortcuts.'],
      ['Good Practice', 'Start the day by checking appointment count, pending actions, and subscription or billing notices.'],
    ],
  },
  {
    title: 'Appointments',
    items: [
      ['Book Appointment', 'The booking wizard follows department, doctor, date and time, patient details, and final confirmation.'],
      ['Patient Modes', 'Use Existing Patient for repeat visits. Use New Patient only when the patient is not already registered.'],
      ['Statuses', 'Pending means booked but not confirmed. Confirmed means accepted for visit. Completed means consultation is finished. Cancelled means inactive.'],
      ['Payments', 'Collect consultation payment after appointment confirmation, then print or download the receipt.'],
    ],
  },
  {
    title: 'Patient Records',
    items: [
      ['Register Patient', 'Capture clean patient demographics and contact information so records can be reused across visits.'],
      ['Search', 'Search by patient name, phone, or related identifying details. Enter at least two characters when using live search fields.'],
      ['Patient Detail', 'Use the detail page as the patient timeline for appointments, prescriptions, receipts, and clinical context.'],
      ['Data Quality', 'Avoid duplicate patients. Search by phone number before creating a new record.'],
    ],
  },
  {
    title: 'Prescriptions',
    items: [
      ['Required Fields', 'Patient, doctor, diagnosis, and at least one medicine are required before saving.'],
      ['Vitals', 'Record BP, pulse, temperature, SpO2, respiratory rate, height, and weight when available.'],
      ['Medicines', 'Use quick-add medicines or enter medicine name, dosage, frequency, duration, route, food timing, and instructions manually.'],
      ['Investigations', 'Add lab tests with urgency and instructions where needed.'],
      ['Print Note', 'Printed prescriptions include: This Prescription / Certificate is not for medicolegal purpose.'],
    ],
  },
  {
    title: 'Medical Certificates',
    items: [
      ['Certificate Types', 'Supported types include sick leave, fitness, fit-to-fly, driving license Form 1A, vaccination, recovery, caretaker leave, and CARA adoption fitness.'],
      ['Validity', 'Enter issue date, valid from, valid until, diagnosis or reason, remarks, and certificate-specific fields carefully.'],
      ['Print and PDF', 'Saved certificates can be printed or downloaded as PDFs unless voided.'],
      ['Medicolegal Boundary', 'Certificates are generated for routine clinical use and include the non-medicolegal note.'],
    ],
  },
  {
    title: 'Consultation Receipts',
    items: [
      ['Create', 'Create a receipt against an appointment with amount, payment mode, and reference if applicable.'],
      ['Search', 'Use filters for receipt number, patient, doctor, payment mode, status, and date range.'],
      ['Reprint', 'Use Print or PDF for active receipts. Older receipts can be found and downloaded again.'],
      ['Void', 'Void only incorrect receipts. Voiding preserves the record for audit history.'],
    ],
  },
  {
    title: 'Reports',
    items: [
      ['Date Range', 'Select start and end dates before generating reports.'],
      ['Views', 'Review all patients, department-wise, doctor-wise, OPD, and IPD report views.'],
      ['Exports', 'Download supported CSV, Excel, or PDF reports depending on the selected view.'],
      ['Reconciliation', 'Use reports with receipt filters to cross-check collections and visit volume.'],
    ],
  },
  {
    title: 'Hospital Administration',
    items: [
      ['Doctors', 'Add and maintain doctors, department mapping, specialization, qualification, and visible profile details.'],
      ['Users', 'Assign the least powerful role that still allows the user to do their work.'],
      ['Settings', 'Maintain hospital address, phone, receipt logo, QR code, and documents used in generated PDFs.'],
      ['Legal and Verification', 'Admins and super admins should keep required hospital documents updated for review.'],
    ],
  },
  {
    title: 'Troubleshooting',
    items: [
      ['Access Denied', 'Your role may not have permission. Ask an admin to review your account role.'],
      ['No Slots', 'Try another date or verify doctor availability and department selection.'],
      ['PDF Fails', 'Check login session, backend connection, receipt status, and whether browser popups are blocked.'],
      ['Payment Mistake', 'Do not edit history manually. Void the incorrect receipt and create the correct one.'],
      ['Duplicate Patient', 'Search by phone before registering. If a duplicate already exists, use the most complete record going forward.'],
    ],
  },
];

const operatingTips = [
  'Keep patient phone numbers consistent because search and follow-up workflows depend on them.',
  'Confirm appointments before collecting consultation payment.',
  'Use void actions for audit-safe corrections instead of deleting financial records.',
  'Review reports at the end of each day for visit count and collection matching.',
  'Keep hospital logo, address, phone, and QR details updated so PDFs look correct.',
  'Log out from shared systems when your shift ends.',
];

const renderItems = (items) => (
  <div style={{ display: 'grid', gap: 12 }}>
    {items.map(([heading, body]) => (
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
);

const UserManual = () => {
  return (
    <div className="page-container">
      <PageHeader
        title="User Manual"
        subtitle="Operational guide for reception, doctors, billing, and hospital admins"
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

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-title">Role Guide</div>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: 14,
        }}>
          {roles.map(([role, detail]) => (
            <div key={role} style={{ borderLeft: '3px solid var(--primary)', paddingLeft: 12 }}>
              <div style={{ fontWeight: 800, marginBottom: 4 }}>{role}</div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.5 }}>{detail}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
        gap: 16,
        marginBottom: 20,
      }}>
        {workflows.map((workflow) => (
          <section key={workflow.title} className="card" style={{ margin: 0 }}>
            <div className="card-title">{workflow.title}</div>
            <ol style={{ margin: 0, paddingLeft: 20, color: 'var(--text-muted)', lineHeight: 1.55, fontSize: 13 }}>
              {workflow.steps.map((step) => (
                <li key={step} style={{ marginBottom: 7 }}>{step}</li>
              ))}
            </ol>
          </section>
        ))}
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
        gap: 16,
        marginBottom: 20,
      }}>
        {sections.map((section) => (
          <section key={section.title} className="card" style={{ margin: 0 }}>
            <div className="card-title">{section.title}</div>
            {renderItems(section.items)}
          </section>
        ))}
      </div>

      <div className="card">
        <div className="card-title">Daily Operating Tips</div>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: 10,
        }}>
          {operatingTips.map((tip) => (
            <div key={tip} style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.5 }}>
              {tip}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default UserManual;
