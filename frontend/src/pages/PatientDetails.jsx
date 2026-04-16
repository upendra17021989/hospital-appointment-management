import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useRole } from '../hooks/useRole';
import { LoadingSpinner, Badge, EmptyState } from '../components/Common';


// ── Helpers ───────────────────────────────────────────────────
const formatDate = (d) => {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
};

const calcAge = (dob) => {
  if (!dob) return null;
  const diff = Date.now() - new Date(dob).getTime();
  return Math.floor(diff / (1000 * 60 * 60 * 24 * 365.25));
};

// ── Info Row ──────────────────────────────────────────────────
const InfoRow = ({ label, value, highlight }) => (
  <div className="pd-info-row">
    <span className="pd-info-label">{label}</span>
    <span className={`pd-info-value ${highlight ? 'pd-highlight' : ''}`}>
      {value || '—'}
    </span>
  </div>
);

// ── Info Card ─────────────────────────────────────────────────
const InfoCard = ({ title, icon, children }) => (
  <div className="pd-card">
    <div className="pd-card-header">
      <span className="pd-card-icon">{icon}</span>
      <h3 className="pd-card-title">{title}</h3>
    </div>
    <div className="pd-card-body">{children}</div>
  </div>
);

// ── Stat Pill ─────────────────────────────────────────────────
const StatPill = ({ label, value, color }) => (
  <div className="pd-stat-pill" style={{ borderColor: color + '40', background: color + '10' }}>
    <div className="pd-stat-value" style={{ color }}>{value}</div>
    <div className="pd-stat-label">{label}</div>
  </div>
);

// ── Tab Button ────────────────────────────────────────────────
const Tab = ({ label, active, count, onClick }) => (
  <button
    className={`pd-tab ${active ? 'pd-tab-active' : ''}`}
    onClick={onClick}
  >
    {label}
    {count !== undefined && (
      <span className="pd-tab-count">{count}</span>
    )}
  </button>
);

// ── Appointment Card ──────────────────────────────────────────
const AppointmentCard = ({ appt, onPrescription }) => (
  <div className="pd-appt-card">
    <div className="pd-appt-top">
      <span className="token-badge">{appt.tokenNumber}</span>
      <Badge status={appt.status} />
    </div>
    <div className="pd-appt-body">
      <div className="pd-appt-row">
        <span className="pd-appt-label">Doctor</span>
        <span className="pd-appt-value">{appt.doctor?.fullName}</span>
      </div>
      <div className="pd-appt-row">
        <span className="pd-appt-label">Department</span>
        <span className="pd-appt-value">{appt.department?.name || '—'}</span>
      </div>
      <div className="pd-appt-row">
        <span className="pd-appt-label">Date & Time</span>
        <span className="pd-appt-value">
          {formatDate(appt.appointmentDate)} · {appt.appointmentTime}
        </span>
      </div>
      <div className="pd-appt-row">
        <span className="pd-appt-label">Reason</span>
        <span className="pd-appt-value">{appt.reasonForVisit}</span>
      </div>
      {appt.symptoms && (
        <div className="pd-appt-row">
          <span className="pd-appt-label">Symptoms</span>
          <span className="pd-appt-value">{appt.symptoms}</span>
        </div>
      )}
    </div>
    {appt.status === 'completed' && (
      <button
        className="btn btn-secondary btn-sm pd-presc-btn"
        onClick={() => onPrescription(appt)}
      >
        📋 View Prescription
      </button>
    )}
  </div>
);

// ── Prescription Card ─────────────────────────────────────────
const PrescriptionCard = ({ presc }) => {
  const [expanded, setExpanded] = useState(false);
  let vitals = {};
  try { vitals = JSON.parse(presc.vitalSigns || '{}'); } catch {}

  return (
    <div className="pd-presc-card">
      <div className="pd-presc-header" onClick={() => setExpanded(e => !e)}>
        <div>
          <div className="pd-presc-date">📋 {formatDate(presc.prescriptionDate)}</div>
          <div className="pd-presc-doctor">{presc.doctor?.fullName} · {presc.doctor?.specialization}</div>
          <div className="pd-presc-diagnosis">{presc.diagnosis}</div>
        </div>
        <div className="pd-presc-expand">{expanded ? '▲' : '▼'}</div>
      </div>

      {expanded && (
        <div className="pd-presc-body">
          {/* Vitals */}
          {Object.values(vitals).some(Boolean) && (
            <div className="pd-presc-section">
              <div className="pd-presc-section-title">🩺 Vital Signs</div>
              <div className="pd-vitals-row">
                {vitals.bp     && <div className="pd-vital"><span>BP</span>{vitals.bp} mmHg</div>}
                {vitals.pulse  && <div className="pd-vital"><span>Pulse</span>{vitals.pulse} bpm</div>}
                {vitals.temp   && <div className="pd-vital"><span>Temp</span>{vitals.temp}°F</div>}
                {vitals.spo2   && <div className="pd-vital"><span>SpO₂</span>{vitals.spo2}%</div>}
                {vitals.weight && <div className="pd-vital"><span>Weight</span>{vitals.weight} kg</div>}
                {vitals.height && <div className="pd-vital"><span>Height</span>{vitals.height} cm</div>}
              </div>
            </div>
          )}

          {/* Medicines */}
          {presc.medicines?.length > 0 && (
            <div className="pd-presc-section">
              <div className="pd-presc-section-title">💊 Medicines ({presc.medicines.length})</div>
              <table className="pd-med-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Medicine</th>
                    <th>Dosage</th>
                    <th>Frequency</th>
                    <th>Duration</th>
                    <th>Route</th>
                    <th>When</th>
                  </tr>
                </thead>
                <tbody>
                  {presc.medicines.map((m, i) => (
                    <tr key={m.id}>
                      <td>{i + 1}</td>
                      <td><strong>{m.medicineName}</strong>{m.instructions && <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{m.instructions}</div>}</td>
                      <td>{m.dosage || '—'}</td>
                      <td>{m.frequency || '—'}</td>
                      <td>{m.duration || '—'}</td>
                      <td>{m.route || 'Oral'}</td>
                      <td>{m.beforeFood ? '🌅 Before food' : '🍽 After food'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Lab Tests */}
          {presc.labTests?.length > 0 && (
            <div className="pd-presc-section">
              <div className="pd-presc-section-title">🧪 Lab Tests ({presc.labTests.length})</div>
              <div className="pd-lab-list">
                {presc.labTests.map((t, i) => (
                  <div key={t.id} className={`pd-lab-item ${t.isUrgent ? 'pd-lab-urgent' : ''}`}>
                    <span>{i + 1}. {t.testName}</span>
                    {t.isUrgent && <span className="pd-urgent-tag">🚨 Urgent</span>}
                    {t.instructions && <span className="pd-lab-instr">{t.instructions}</span>}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Follow Up */}
          {(presc.followUpDate || presc.followUpInstructions) && (
            <div className="pd-presc-section">
              <div className="pd-presc-section-title">📅 Follow Up</div>
              {presc.followUpDate && (
                <div className="pd-followup-date">
                  Return on: <strong>{formatDate(presc.followUpDate)}</strong>
                </div>
              )}
              {presc.followUpInstructions && (
                <div className="pd-followup-instr">{presc.followUpInstructions}</div>
              )}
            </div>
          )}

          {/* Diet & Activity */}
          {(presc.dietInstructions || presc.activityRestrictions) && (
            <div className="pd-presc-section pd-two-col">
              {presc.dietInstructions && (
                <div>
                  <div className="pd-presc-section-title">🥗 Diet</div>
                  <p>{presc.dietInstructions}</p>
                </div>
              )}
              {presc.activityRestrictions && (
                <div>
                  <div className="pd-presc-section-title">🏃 Activity</div>
                  <p>{presc.activityRestrictions}</p>
                </div>
              )}
            </div>
          )}

          {presc.additionalNotes && (
            <div className="pd-presc-section">
              <div className="pd-presc-section-title">📝 Notes</div>
              <p>{presc.additionalNotes}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// ── Patient List (for searching) ──────────────────────────────
const PatientList = ({ onSelect, onRegister }) => {
  const [patients,  setPatients]  = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [search,    setSearch]    = useState('');
  const [filtered,  setFiltered]  = useState([]);
  const { isStaff, isReceptionist } = useRole();

  useEffect(() => {
    api.get('/patients/hospital')
      .then(data => { setPatients(data || []); setFiltered(data || []); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!search.trim()) { setFiltered(patients); return; }
    const q = search.toLowerCase();
    setFiltered(patients.filter(p =>
      p.fullName?.toLowerCase().includes(q) ||
      p.phone?.includes(search) ||
      p.email?.toLowerCase().includes(q) ||
      p.bloodGroup?.toLowerCase().includes(q)
    ));
  }, [search, patients]);

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Patients</h1>
          <p className="page-subtitle">{patients.length} registered patients</p>
        </div>
        <button className="btn btn-primary" onClick={onRegister}>
          + Register Patient
        </button>
      </div>

      {/* Search */}
      <div className="search-bar" style={{ marginBottom: 20 }}>
        <div className="search-input-wrap" style={{ flex: 1 }}>
          <span className="search-icon">🔍</span>
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by name, phone, email or blood group..."
          />
        </div>
        {search && (
          <button className="btn btn-secondary btn-sm" onClick={() => setSearch('')}>
            Clear
          </button>
        )}
      </div>

      {filtered.length === 0 ? (
        <EmptyState icon="👥" title="No patients found" subtitle="Register a new patient to get started" />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Patient</th>
                <th>Phone</th>
                <th>Gender</th>
                <th>Age</th>
                <th>Blood Group</th>
                <th>DOB</th>
                <th>Registered</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(p => (
                <tr key={p.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div className="pd-avatar">
                        {p.firstName?.[0]}{p.lastName?.[0]}
                      </div>
                      <div>
                        <div style={{ fontWeight: 700 }}>{p.fullName}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{p.email || '—'}</div>
                      </div>
                    </div>
                  </td>
                  <td>{p.phone}</td>
                  <td style={{ textTransform: 'capitalize' }}>{p.age || calcAge(p.dateOfBirth) || '—'} yrs</td>
                  <td>
                    {p.bloodGroup
                      ? <span className="pd-blood-badge">{p.bloodGroup}</span>
                      : '—'}
                  </td>
                  <td>{p.dateOfBirth ? formatDate(p.dateOfBirth) : '—'}</td>
                  <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>{formatDate(p.createdAt)}</td>
                  <td>
                    {isStaff() || isReceptionist() ? (
                      <button
                        className="btn btn-secondary btn-sm"
                        disabled
                        title="View patient details available for Hospital Admin and Super Admin only"
                      >
                        🔒 Admin Only
                      </button>
                    ) : (
                      <button
                        className="btn btn-primary btn-sm"
                        onClick={() => onSelect(p)}
                      >
                        👁 View
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

// ── Patient Detail View ───────────────────────────────────────
const PatientDetailView = ({ patient, onBack, navigate }) => {
  const [activeTab,     setActiveTab]     = useState('overview');

  const [appointments,  setAppointments]  = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading,       setLoading]       = useState(true);
  const [editMode,      setEditMode]      = useState(false);
  const [editForm,      setEditForm]      = useState({ ...patient });
  const [saving,        setSaving]        = useState(false);
  const [saveMsg,       setSaveMsg]       = useState('');

  const age = patient.age || calcAge(patient.dateOfBirth);

  useEffect(() => {
    Promise.all([
      api.get(`/appointments/hospital?patientId=${patient.id}`).catch(() => []),
      api.get(`/prescriptions/hospital/patient/${patient.id}`).catch(() => []),
    ]).then(([apts, prescs]) => {
      setAppointments(apts || []);
      setPrescriptions(prescs || []);
      setLoading(false);
    });
  }, [patient.id]);

  const handleSave = async () => {
    setSaving(true);
    try {
        await api.put(`/patients/hospital/${patient.id}`, {
        firstName:            editForm.firstName,
        lastName:             editForm.lastName,
        dateOfBirth:          editForm.dateOfBirth || null,
        age:                  parseInt(editForm.age) || null,
        gender:               editForm.gender,
        phone:                editForm.phone,
        email:                editForm.email || null,
        address:              editForm.address || null,
        bloodGroup:           editForm.bloodGroup || null,
        medicalHistory:       editForm.medicalHistory || null,
        allergies:            editForm.allergies || null,
        emergencyContactName: editForm.emergencyContactName || null,
        emergencyContactPhone:editForm.emergencyContactPhone || null,
      });
      setSaveMsg('Patient details updated successfully!');
      setEditMode(false);
      setTimeout(() => setSaveMsg(''), 3000);
    } catch (e) {
      setSaveMsg('Error: ' + e.message);
    } finally {
      setSaving(false);
    }
  };

  const completedAppts  = appointments.filter(a => a.status === 'completed').length;
  const upcomingAppts   = appointments.filter(a => ['pending', 'confirmed'].includes(a.status)).length;
  const cancelledAppts  = appointments.filter(a => a.status === 'cancelled').length;

  const setEF = k => e => setEditForm(f => ({ ...f, [k]: e.target.value }));

  return (
    <div>
      {/* Back button */}
      <button className="pd-back-btn" onClick={onBack}>
        ← Back to Patients
      </button>

      {saveMsg && (
        <div className={`alert ${saveMsg.startsWith('Error') ? 'alert-error' : 'alert-success'}`}>
          {saveMsg}
        </div>
      )}

      {/* Patient Header */}
      <div className="pd-hero">
        <div className="pd-hero-left">
          <div className="pd-hero-avatar">
            {patient.firstName?.[0]}{patient.lastName?.[0]}
          </div>
          <div className="pd-hero-info">
            <h1 className="pd-hero-name">{patient.fullName}</h1>
            <div className="pd-hero-meta">
              {age && <span>Age: {age} yrs</span>}
              {patient.gender && <span style={{ textTransform: 'capitalize' }}>{patient.gender}</span>}
              {patient.bloodGroup && (
                <span className="pd-blood-badge pd-blood-badge-lg">{patient.bloodGroup}</span>
              )}
            </div>
            <div className="pd-hero-contact">
              📞 {patient.phone}
              {patient.email && <span> · ✉️ {patient.email}</span>}
            </div>
          </div>
        </div>
        <div className="pd-hero-actions">
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setEditMode(e => !e)}
          >
            {editMode ? '✕ Cancel Edit' : '✏️ Edit'}
          </button>
          <button
            className="btn btn-primary btn-sm"
            onClick={() => navigate('/prescription-form', { state: { prefillPatient: patient } })}
          >
            💊 New Prescription
          </button>

          <button
            className="btn btn-accent btn-sm"
            onClick={() => navigate('/book-appointment', { state: { prefillPatient: patient } })}
          >
            📅 Book Appointment
          </button>

        </div>
      </div>

      {/* Stats row */}
      <div className="pd-stats-row">
        <StatPill label="Total Visits"     value={appointments.length} color="var(--primary)" />
        <StatPill label="Completed"        value={completedAppts}      color="var(--accent)"  />
        <StatPill label="Upcoming"         value={upcomingAppts}       color="var(--gold)"    />
        <StatPill label="Cancelled"        value={cancelledAppts}      color="#c0220a"        />
        <StatPill label="Prescriptions"    value={prescriptions.length} color="#6a4a8a"      />
      </div>

      {/* Tabs */}
      <div className="pd-tabs">
        {[
          { key: 'overview',      label: 'Overview' },
          { key: 'appointments',  label: 'Appointments',  count: appointments.length  },
          { key: 'prescriptions', label: 'Prescriptions', count: prescriptions.length },
          { key: 'edit',          label: 'Edit Details' },
        ].map(t => (
          <Tab
            key={t.key}
            label={t.label}
            count={t.count}
            active={activeTab === t.key}
            onClick={() => { setActiveTab(t.key); if (t.key !== 'edit') setEditMode(false); }}
          />
        ))}
      </div>

      {loading ? <LoadingSpinner /> : (
        <>
          {/* ── OVERVIEW TAB ── */}
          {activeTab === 'overview' && (
            <div className="pd-tab-content">
              <div className="pd-overview-grid">
                {/* Personal Info */}
                <InfoCard title="Personal Information" icon="👤">
                  <InfoRow label="Full Name"    value={patient.fullName} />
                  <InfoRow label="Date of Birth" value={patient.dateOfBirth ? `${formatDate(patient.dateOfBirth)} (Age ${age})` : null} />
                  <InfoRow label="Gender"       value={patient.gender} />
                  <InfoRow label="Blood Group"  value={patient.bloodGroup} highlight />
                  <InfoRow label="Phone"        value={patient.phone} />
                  <InfoRow label="Email"        value={patient.email} />
                  <InfoRow label="Address"      value={patient.address} />
                  <InfoRow label="Registered"   value={formatDate(patient.createdAt)} />
                </InfoCard>

                {/* Medical Info */}
                <InfoCard title="Medical Information" icon="🏥">
                  <InfoRow label="Allergies"        value={patient.allergies} highlight />
                  <InfoRow label="Medical History"  value={patient.medicalHistory} />
                </InfoCard>

                {/* Emergency Contact */}
                <InfoCard title="Emergency Contact" icon="🚨">
                  <InfoRow label="Name"     value={patient.emergencyContactName} />
                  <InfoRow label="Phone"    value={patient.emergencyContactPhone} />
                </InfoCard>

                {/* Recent Activity */}
                <InfoCard title="Recent Activity" icon="📊">
                  {appointments.length === 0 ? (
                    <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>No appointments yet</p>
                  ) : (
                    appointments.slice(0, 3).map(a => (
                      <div key={a.id} className="pd-recent-row">
                        <div>
                          <div style={{ fontWeight: 600, fontSize: 14 }}>{a.doctor?.fullName}</div>
                          <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{formatDate(a.appointmentDate)}</div>
                        </div>
                        <Badge status={a.status} />
                      </div>
                    ))
                  )}
                  {appointments.length > 3 && (
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => setActiveTab('appointments')}
                      style={{ marginTop: 10, width: '100%', justifyContent: 'center' }}
                    >
                      View all {appointments.length} appointments
                    </button>
                  )}
                </InfoCard>
              </div>
            </div>
          )}

          {/* ── APPOINTMENTS TAB ── */}
          {activeTab === 'appointments' && (
            <div className="pd-tab-content">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <h3 style={{ fontFamily: 'var(--font-display, serif)', fontSize: 18, fontWeight: 700 }}>
                  All Appointments ({appointments.length})
                </h3>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => navigate('/book-appointment', { state: { prefillPatient: patient } })}
                >
                  + Book Appointment
                </button>

              </div>
              {appointments.length === 0 ? (
                <EmptyState icon="📅" title="No appointments" subtitle="Book the first appointment for this patient" />
              ) : (
                <div className="pd-appt-grid">
                  {appointments.map(a => (
                    <AppointmentCard
                      key={a.id}
                      appt={a}
                      onPrescription={appt => {
                        navigate('/prescription-form', {
                          state: {
                            appointmentId: appt.id,
                            prefillPatient: patient,
                          }
                        });
                      }}
                    />

                  ))}
                </div>
              )}
            </div>
          )}

          {/* ── PRESCRIPTIONS TAB ── */}
          {activeTab === 'prescriptions' && (
            <div className="pd-tab-content">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <h3 style={{ fontFamily: 'var(--font-display, serif)', fontSize: 18, fontWeight: 700 }}>
                  Prescription History ({prescriptions.length})
                </h3>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => navigate('/prescription-form', { state: { prefillPatient: patient } })}
                >
                  + New Prescription
                </button>

              </div>
              {prescriptions.length === 0 ? (
                <EmptyState icon="💊" title="No prescriptions" subtitle="Create the first prescription for this patient" />
              ) : (
                <div className="pd-presc-list">
                  {prescriptions.map(p => (
                    <PrescriptionCard key={p.id} presc={p} />
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ── EDIT TAB ── */}
          {activeTab === 'edit' && (
            <div className="pd-tab-content">
              <div className="pd-edit-card card">
                <div className="card-title">✏️ Edit Patient Details</div>
                <div className="form-grid">
                  <div className="form-group">
                    <label>First Name *</label>
                    <input value={editForm.firstName} onChange={setEF('firstName')} />
                  </div>
                  <div className="form-group">
                    <label>Last Name *</label>
                    <input value={editForm.lastName} onChange={setEF('lastName')} />
                  </div>
                  <div className="form-group">
                    <label>Phone </label>
                    <input value={editForm.phone} onChange={setEF('phone')} />
                  </div>
                  <div className="form-group">
                    <label>Email</label>
                    <input type="email" value={editForm.email || ''} onChange={setEF('email')} />
                  </div>
                  <div className="form-group">
                    <label>Date of Birth</label>
                    <input type="date" value={editForm.dateOfBirth || ''} onChange={setEF('dateOfBirth')} />
                  </div>
                  <div className="form-group">
                    <label>Age</label>
                    <input type="number" value={editForm.age || ''} onChange={setEF('age')} min="0" max="150" placeholder="e.g. 30" />
                  </div>
                  <div className="form-group">
                    <label>Gender</label>
                    <select value={editForm.gender || ''} onChange={setEF('gender')}>
                      <option value="">Select</option>
                      <option value="male">Male</option>
                      <option value="female">Female</option>
                      <option value="other">Other</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Blood Group</label>
                    <select value={editForm.bloodGroup || ''} onChange={setEF('bloodGroup')}>
                      <option value="">Select</option>
                      {['A+','A-','B+','B-','AB+','AB-','O+','O-'].map(bg => (
                        <option key={bg} value={bg}>{bg}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Address</label>
                    <input value={editForm.address || ''} onChange={setEF('address')} placeholder="Full address" />
                  </div>
                  <div className="form-group full">
                    <label>Known Allergies</label>
                    <textarea value={editForm.allergies || ''} onChange={setEF('allergies')} placeholder="List any known allergies..." />
                  </div>
                  <div className="form-group full">
                    <label>Medical History</label>
                    <textarea value={editForm.medicalHistory || ''} onChange={setEF('medicalHistory')} placeholder="Past conditions, surgeries, medications..." />
                  </div>
                  <div className="form-group">
                    <label>Emergency Contact Name</label>
                    <input value={editForm.emergencyContactName || ''} onChange={setEF('emergencyContactName')} placeholder="Contact person name" />
                  </div>
                  <div className="form-group">
                    <label>Emergency Contact Phone</label>
                    <input value={editForm.emergencyContactPhone || ''} onChange={setEF('emergencyContactPhone')} placeholder="+91 98765 43210" />
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 20 }}>
                  <button className="btn btn-secondary" onClick={() => setEditForm({ ...patient })}>Reset</button>
                  <button className="btn btn-primary" disabled={saving} onClick={handleSave}>
                    {saving ? 'Saving...' : '✓ Save Changes'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

// ── Main export — handles list + detail view ──────────────────
const PatientDetails = () => {
  const navigate = useNavigate();
  const [selectedPatient, setSelectedPatient] = useState(null);


  if (selectedPatient) {
    return (
      <PatientDetailView
        patient={selectedPatient}
        onBack={() => setSelectedPatient(null)}
        navigate={navigate}
      />
    );
  }

  return (
    <PatientList
      onSelect={setSelectedPatient}
      onRegister={() => navigate('/patient-form')}
    />
  );
};


export default PatientDetails;
