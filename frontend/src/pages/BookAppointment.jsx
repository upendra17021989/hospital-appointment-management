import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { LoadingSpinner } from '../components/Common';


const STEPS = [
  { num: 1, label: 'Department' },
  { num: 2, label: 'Doctor' },
  { num: 3, label: 'Date & Time' },
  { num: 4, label: 'Patient Info' },
  { num: 5, label: 'Confirm' },
];

const StepIndicator = ({ current }) => (
  <div className="steps">
    {STEPS.map(s => (
      <div key={s.num} className={`step ${current === s.num ? 'active' : current > s.num ? 'done' : ''}`}>
        <div className="step-num">{current > s.num ? '✓' : s.num}</div>
        <div className="step-label">{s.label}</div>
      </div>
    ))}
  </div>
);

// ── Step 1: Department ────────────────────────────────────────
const Step1Department = ({ departments, selected, onSelect, onNext }) => (
  <div className="card">
    <div className="card-title">Select Department</div>
    <div className="dept-select-grid">
      {departments.map(d => (
        <div
          key={d.id}
          className={`dept-card ${selected?.id === d.id ? 'selected' : ''}`}
          onClick={() => onSelect(d)}
        >
          <div className="dept-card-name">{d.name}</div>
          <div className="dept-card-floor">Floor {d.floorNumber}</div>
        </div>
      ))}
    </div>
    <div style={{ marginTop: 20, display: 'flex', justifyContent: 'flex-end' }}>
      <button className="btn btn-primary" disabled={!selected} onClick={onNext}>Next →</button>
    </div>
  </div>
);

// ── Step 2: Doctor ────────────────────────────────────────────
const Step2Doctor = ({ doctors, selected, onSelect, onNext, onBack, deptName }) => (
  <div className="card">
    <div className="card-title">Select Doctor — {deptName}</div>
    <div className="doctor-grid">
      {doctors.map(d => (
        <div
          key={d.id}
          className={`doctor-card`}
          style={{ cursor: 'pointer', border: selected?.id === d.id ? '2px solid var(--primary)' : undefined }}
          onClick={() => onSelect(d)}
        >
          <div className="doctor-avatar">{d.firstName?.[0]}{d.lastName?.[0]}</div>
          <div className="doctor-name">Dr. {d.firstName} {d.lastName}</div>
          <div className="doctor-spec">{d.specialization}</div>
          <div className="doctor-dept">{d.department?.name}</div>
          <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 6 }}>
            {d.experienceYears} yrs exp · {d.qualification}
          </div>
          <div className="doctor-fee">₹{d.consultationFee?.toFixed(0)}</div>
          {d.languagesSpoken && (
            <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
              {d.languagesSpoken.map(l => (
                <span key={l} style={{ fontSize: 11, background: 'var(--bg)', padding: '2px 8px', borderRadius: 6, border: '1px solid var(--border)' }}>{l}</span>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
    {doctors.length === 0 && (
      <div className="empty-state">
        <div className="empty-state-icon">👨‍⚕️</div>
        <div className="empty-state-title">No doctors available</div>
      </div>
    )}
    <div style={{ marginTop: 20, display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
      <button className="btn btn-secondary" onClick={onBack}>← Back</button>
      <button className="btn btn-primary" disabled={!selected} onClick={onNext}>Next →</button>
    </div>
  </div>
);

// ── Step 3: Date & Time ───────────────────────────────────────
const Step3DateTime = ({ doctor, selectedDate, setSelectedDate, slots, slotsLoading, selectedSlot, setSelectedSlot, onNext, onBack }) => {
  const today = new Date().toISOString().split('T')[0];
  return (
    <div className="card">
      <div className="card-title">Select Date & Time</div>
      <div className="form-group" style={{ maxWidth: 280, marginBottom: 20 }}>
        <label>Appointment Date</label>
        <input type="date" min={today} value={selectedDate}
          onChange={e => { setSelectedDate(e.target.value); setSelectedSlot(null); }} />
      </div>

      {selectedDate && (
        <div>
          <label style={{ fontWeight: 600, fontSize: 13 }}>
            Available Slots — {doctor?.fullName}
          </label>
          {slotsLoading ? <LoadingSpinner /> : (
            <div className="slots-grid">
              {slots.map((s, i) => (
                <button
                  key={i}
                  className={`slot-btn ${selectedSlot?.time === s.time ? 'selected' : ''}`}
                  disabled={!s.available}
                  onClick={() => setSelectedSlot(s)}
                >
                  {s.displayTime}
                </button>
              ))}
            </div>
          )}
          {slots.length === 0 && !slotsLoading && (
            <div className="alert alert-info" style={{ marginTop: 12 }}>
              No slots available for this date. Please select another date.
            </div>
          )}
        </div>
      )}

      <div style={{ marginTop: 20, display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
        <button className="btn btn-secondary" onClick={onBack}>← Back</button>
        <button className="btn btn-primary" disabled={!selectedSlot} onClick={onNext}>Next →</button>
      </div>
    </div>
  );
};

// ── Phone Validation ──────────────────────────────────────────
// Accepts formats:
//   9876543210        (10 digits, starts with 6–9)
//   +919876543210     (with +91 country code)
//   919876543210      (with 91 country code, no +)
//   +91 98765 43210   (with spaces)
//   098765 43210      (with leading 0)
const INDIAN_PHONE_REGEX = /^(?:\+91|91|0)?[\s-]?[6-9]\d{9}$/;

const validatePhone = (value) => {
  // Strip all spaces and dashes for validation
  const stripped = value.replace(/[\s-]/g, '');
  if (!stripped) return 'Phone number is required.';
  if (!INDIAN_PHONE_REGEX.test(stripped)) {
    return 'Enter a valid Indian mobile number (e.g. 9876543210 or +91 98765 43210).';
  }
  return ''; // valid
};

// Normalise phone to +91XXXXXXXXXX before sending to API
const normalisePhone = (value) => {
  const stripped = value.replace(/[\s-]/g, '');
  if (stripped.startsWith('+91')) return stripped;
  if (stripped.startsWith('91') && stripped.length === 12) return '+' + stripped;
  if (stripped.startsWith('0')) return '+91' + stripped.slice(1);
  return '+91' + stripped;
};

// ── Step 4: Patient Info ──────────────────────────────────────
const Step4PatientInfo = ({
  patientData, setPatientData, visitData, setVisitData,
  isNewPatient, setIsNewPatient,
  searchQuery, setSearchQuery,
  searchedPatients, setSearchedPatients,
  selectedExistingPatient, setSelectedExistingPatient,
  searchLoading,
  onNext, onBack
}) => {
  const [touchedFields, setTouchedFields] = useState({
    firstName: false,
    lastName: false,
    phone: false,
    gender: false,
    reasonForVisit: false
  });

  const phoneError = touchedFields.phone ? validatePhone(patientData.phone) : '';

  const firstNameError = touchedFields.firstName && !patientData.firstName.trim();
  const lastNameError = touchedFields.lastName && !patientData.lastName.trim();
  const genderError = touchedFields.gender && !patientData.gender;
  const reasonError = touchedFields.reasonForVisit && !visitData.reasonForVisit.trim();

  const isValid = (isNewPatient ? 
    (patientData.firstName.trim() && patientData.lastName.trim() && !phoneError && !!patientData.gender && !reasonError) :
    (!!selectedExistingPatient?.id && !phoneError && !reasonError)
  );

  const touchField = (field) => () => setTouchedFields(t => ({ ...t, [field]: true }));

  const setAllTouched = () => setTouchedFields({
    firstName: true,
    lastName: true,
    phone: true,
    gender: true,
    reasonForVisit: true
  });

  const handlePhoneChange = (e) => {
    const raw = e.target.value.replace(/[^\d\s\-+]/g, '');
    setPatientData(p => ({ ...p, phone: raw }));
    if (!isNewPatient) setSearchQuery(raw);
  };

  const handlePatientSelect = (patient) => {
    setSelectedExistingPatient(patient);
    setPatientData({
      firstName: patient.firstName || '',
      lastName: patient.lastName || '',
      phone: patient.phone || '',
      email: patient.email || '',
      gender: patient.gender || '',
      dateOfBirth: patient.dateOfBirth || ''
    });
  };

  const handleToggleMode = () => {
    setIsNewPatient(!isNewPatient);
    if (!isNewPatient) {
      setSelectedExistingPatient(null);
      setPatientData({ firstName: '', lastName: '', phone: '', email: '', gender: '', dateOfBirth: '' });
    }
    setSearchQuery('');
    setSearchedPatients([]);
  };

  const handleNext = () => {
    setAllTouched();
    if (!isValid) return;
    setPatientData(p => ({ ...p, phone: normalisePhone(p.phone) }));
    onNext();
  };

  const fieldDisabled = !isNewPatient;

  return (
    <div className="card">
      <div className="card-title">Patient Information</div>
      
      {/* Toggle */}
      <div style={{ marginBottom: 20, display: 'flex', gap: 12 }}>
        <button 
          className={`btn ${isNewPatient ? 'btn-primary' : 'btn-secondary'}`}
          onClick={handleToggleMode}
        >
          👤 New Patient
        </button>
        <button 
          className={`btn ${!isNewPatient ? 'btn-primary' : 'btn-secondary'}`}
          onClick={handleToggleMode}
        >
          🔍 Existing Patient
        </button>
      </div>

      {!isNewPatient ? (
        // Existing Patient Mode
        <div>
          <div className="form-group">
            <label>Search by Phone</label>
            <input
              value={searchQuery}
              onChange={handlePhoneChange}
              onBlur={() => setPhoneTouched(true)}
              placeholder="Enter phone number (10 digits)"
              style={{
                borderColor: phoneError ? '#c0220a' : undefined,
                boxShadow: phoneError ? '0 0 0 3px rgba(192,34,10,0.1)' : undefined,
              }}
            />
            {phoneError && (
              <span style={{ fontSize: 12, color: '#c0220a', marginTop: 2 }}>
                ⚠ {phoneError}
              </span>
            )}
            <span style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
              Enter 10-digit phone to search existing patients
            </span>
          </div>

          {searchLoading ? (
            <LoadingSpinner />
          ) : searchedPatients.length > 0 ? (
            <div>
              <label style={{ fontWeight: 600, marginBottom: 12, display: 'block' }}>
                Select Patient ({searchedPatients.length} found)
              </label>
              <div className="doctor-grid" style={{ marginBottom: 20 }}>
                {searchedPatients.map(p => (
                  <div
                    key={p.id}
                    className="doctor-card"
                    style={{ 
                      cursor: 'pointer', 
                      border: selectedExistingPatient?.id === p.id ? '2px solid var(--primary)' : undefined 
                    }}
                    onClick={() => handlePatientSelect(p)}
                  >
                    <div className="doctor-avatar">{p.firstName?.[0]}{p.lastName?.[0]}</div>
                    <div className="doctor-name">{p.fullName || `${p.firstName} ${p.lastName}`}</div>
                    <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>
                      {p.phone}
                    </div>
                    {p.email && <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{p.email}</div>}
                  </div>
                ))}
              </div>
            </div>
          ) : searchQuery.length >= 10 && !searchLoading ? (
            <div className="empty-state">
              <div className="empty-state-icon">👤</div>
              <div className="empty-state-title">No patients found</div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>Try a different phone number</div>
            </div>
          ) : null}

          {selectedExistingPatient && (
            <div style={{ background: 'var(--bg-light)', padding: 16, borderRadius: 8, marginBottom: 20 }}>
              <div style={{ fontWeight: 600, marginBottom: 8 }}>Selected: {selectedExistingPatient.fullName}</div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>{selectedExistingPatient.phone}</div>
            </div>
          )}
        </div>
      ) : (
        // New Patient Mode - Original form
        <div className="form-grid">
          {/* First Name */}
          <div className="form-group">
            <label>First Name *</label>
            <input
              value={patientData.firstName}
              onChange={e => setPatientData(p => ({ ...p, firstName: e.target.value }))}
              onBlur={touchField('firstName')}
              style={{ borderColor: firstNameError ? '#c0220a' : undefined }}
              placeholder="First name"
            />
            {firstNameError && (
              <span style={{ fontSize: 12, color: '#c0220a', marginTop: 2 }}>
                ⚠ First name is required
              </span>
            )}
          </div>

          {/* Last Name */}
          <div className="form-group">
            <label>Last Name *</label>
            <input
              value={patientData.lastName}
              onChange={e => setPatientData(p => ({ ...p, lastName: e.target.value }))}
              onBlur={touchField('lastName')}
              style={{ borderColor: lastNameError ? '#c0220a' : undefined }}
              placeholder="Last name"
            />
            {lastNameError && (
              <span style={{ fontSize: 12, color: '#c0220a', marginTop: 2 }}>
                ⚠ Last name is required
              </span>
            )}
          </div>

          {/* Phone with validation */}
            <div className="form-group">
              <label>Phone *</label>
              <div style={{ position: 'relative' }}>
                <span style={{
                  position: 'absolute', left: 12, top: '50%',
                  transform: 'translateY(-50%)',
                  fontSize: 13, fontWeight: 700,
                  color: 'var(--text-muted)',
                  pointerEvents: 'none',
                  userSelect: 'none',
                }}>🇮🇳</span>
                <input
                  value={patientData.phone}
                  onChange={handlePhoneChange}
                  onBlur={touchField('phone')}
                  placeholder="98765 43210"
                  maxLength={15}
                  style={{
                    paddingLeft: 36,
                    borderColor: phoneError ? '#c0220a' : undefined,
                    boxShadow: phoneError ? '0 0 0 3px rgba(192,34,10,0.1)' : undefined,
                  }}
                />
              </div>
              {phoneError && (
                <span style={{ fontSize: 12, color: '#c0220a', marginTop: 2 }}>
                  ⚠ {phoneError}
                </span>
              )}
              {!phoneError && touchedFields.phone && patientData.phone && (
                <span style={{ fontSize: 12, color: 'var(--accent)', marginTop: 2 }}>
                  ✓ Valid Indian mobile number
                </span>
              )}
              <span style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                Accepted: 9876543210 · +91 98765 43210 · 091 98765 43210
              </span>
            </div>

          {/* Email */}
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              value={patientData.email}
              onChange={e => setPatientData(p => ({ ...p, email: e.target.value }))}
              placeholder="patient@email.com"
            />
          </div>

          {/* Gender */}
            <div className="form-group">
              <label>Gender *</label>
              <select
                value={patientData.gender}
                onChange={e => setPatientData(p => ({ ...p, gender: e.target.value }))}
                onBlur={touchField('gender')}
                style={{ borderColor: genderError ? '#c0220a' : undefined }}
              >
                <option value="">Select gender</option>
                <option value="male">Male</option>
                <option value="female">Female</option>
                <option value="other">Other</option>
              </select>
              {genderError && (
                <span style={{ fontSize: 12, color: '#c0220a', marginTop: 2 }}>
                  ⚠ Gender is required
                </span>
              )}
            </div>

          {/* Date of Birth */}
          <div className="form-group">
            <label>Date of Birth</label>
            <input
              type="date"
              value={patientData.dateOfBirth}
              onChange={e => setPatientData(p => ({ ...p, dateOfBirth: e.target.value }))}
            />
          </div>
        </div>
      )}

      {/* Common Visit Fields */}
      <div className="form-grid">
        {/* Reason for Visit */}
        <div className="form-group full">
          <label>Reason for Visit *</label>
          <textarea
            value={visitData.reasonForVisit}
            onChange={e => setVisitData(v => ({ ...v, reasonForVisit: e.target.value }))}
            onBlur={touchField('reasonForVisit')}
            style={{ borderColor: reasonError ? '#c0220a' : undefined }}
            placeholder="Describe the reason for this appointment..."
          />
          {reasonError && (
            <span style={{ fontSize: 12, color: '#c0220a', marginTop: 2 }}>
              ⚠ Reason for visit is required
            </span>
          )}
        </div>

        {/* Symptoms */}
        <div className="form-group full">
          <label>Symptoms (Optional)</label>
          <textarea
            value={visitData.symptoms}
            onChange={e => setVisitData(v => ({ ...v, symptoms: e.target.value }))}
            placeholder="List any symptoms..."
          />
        </div>

        {/* Appointment Type */}
        <div className="form-group">
          <label>Appointment Type</label>
          <select
            value={visitData.appointmentType}
            onChange={e => setVisitData(v => ({ ...v, appointmentType: e.target.value }))}
          >
            <option value="in_person">In Person</option>
            <option value="virtual">Virtual</option>
            <option value="follow_up">Follow Up</option>
          </select>
        </div>
      </div>

      <div style={{ marginTop: 20, display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
        <button className="btn btn-secondary" onClick={onBack}>← Back</button>
        <button
          className="btn btn-primary"
          disabled={!isValid}
          onClick={handleNext}
        >
          Review →
        </button>
      </div>
    </div>
  );
};

// ── Step 5: Confirm ───────────────────────────────────────────
const Step5Confirm = ({ dept, doctor, selectedDate, selectedSlot, patientData, visitData, loading, error, onConfirm, onBack }) => {
  const KV = ({ k, v }) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--border)', fontSize: 14 }}>
      <span style={{ color: 'var(--text-muted)' }}>{k}</span>
      <span style={{ fontWeight: 600 }}>{v}</span>
    </div>
  );
  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="grid-2" style={{ marginBottom: 20 }}>
        <div className="card" style={{ background: 'var(--bg)' }}>
          <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 12, textTransform: 'uppercase', letterSpacing: 1, color: 'var(--text-muted)' }}>Appointment Details</div>
          <KV k="Department" v={dept?.name} />
          <KV k="Doctor" v={doctor?.fullName} />
          <KV k="Specialization" v={doctor?.specialization} />
          <KV k="Date" v={selectedDate} />
          <KV k="Time" v={selectedSlot?.displayTime} />
          <KV k="Fee" v={`₹${doctor?.consultationFee?.toFixed(0)}`} />
          <KV k="Type" v={visitData.appointmentType?.replace('_', ' ')} />
        </div>
        <div className="card" style={{ background: 'var(--bg)' }}>
          <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 12, textTransform: 'uppercase', letterSpacing: 1, color: 'var(--text-muted)' }}>Patient Details</div>
          <KV k="Name" v={`${patientData.firstName} ${patientData.lastName}`} />
          <KV k="Phone" v={patientData.phone} />
          <KV k="Email" v={patientData.email || '—'} />
          <KV k="Gender" v={patientData.gender || '—'} />
          <div style={{ marginTop: 12 }}>
            <div style={{ fontSize: 12, color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1 }}>Reason</div>
            <div style={{ fontSize: 14, marginTop: 4 }}>{visitData.reasonForVisit}</div>
          </div>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
        <button className="btn btn-secondary" onClick={onBack}>← Back</button>
        <button className="btn btn-primary" disabled={loading} onClick={onConfirm}>
          {loading ? 'Booking...' : '✓ Confirm Appointment'}
        </button>
      </div>
    </div>
  );
};

// ── Success Screen ────────────────────────────────────────────
const SuccessScreen = ({ appointment, onReset }) => {
  const KV = ({ k, v }) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '7px 0', borderBottom: '1px solid var(--border)', fontSize: 14 }}>
      <span style={{ color: 'var(--text-muted)' }}>{k}</span>
      <span style={{ fontWeight: 600 }}>{v}</span>
    </div>
  );
  return (
    <div style={{ maxWidth: 540, margin: '0 auto', textAlign: 'center', padding: '48px 0' }}>
      <div style={{ fontSize: 64, marginBottom: 16 }}>✅</div>
      <h2 className="page-title" style={{ marginBottom: 8 }}>Appointment Confirmed!</h2>
      <p style={{ color: 'var(--text-muted)', marginBottom: 24 }}>Your appointment has been booked successfully.</p>
      <div className="card" style={{ textAlign: 'left', marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div className="card-title" style={{ margin: 0 }}>Booking Details</div>
          <span className="token-badge" style={{ fontSize: 14 }}>{appointment.tokenNumber}</span>
        </div>
        <KV k="Patient" v={appointment.patient?.fullName} />
        <KV k="Doctor" v={appointment.doctor?.fullName} />
        <KV k="Date" v={appointment.appointmentDate} />
        <KV k="Time" v={appointment.appointmentTime} />
        <KV k="Department" v={appointment.department?.name} />
        <KV k="Type" v={appointment.appointmentType} />
      </div>
      <button className="btn btn-primary" onClick={onReset}>Book Another</button>
    </div>
  );
};

// ── Main BookAppointment Page ─────────────────────────────────
const BookAppointment = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();

  const [step, setStep] = useState(1);
  const [departments, setDepartments] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [slots, setSlots] = useState([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [selectedDept, setSelectedDept] = useState(null);
  const [selectedDoctor, setSelectedDoctor] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [selectedDate, setSelectedDate] = useState('');
  const [patientData, setPatientData] = useState({ firstName: '', lastName: '', phone: '', email: '', gender: '', dateOfBirth: '' });
  const [visitData, setVisitData] = useState({ reasonForVisit: '', symptoms: '', appointmentType: 'in_person' });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(null);
  const [error, setError] = useState('');

  // New states for existing patient toggle
  const [isNewPatient, setIsNewPatient] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchedPatients, setSearchedPatients] = useState([]);
  const [selectedExistingPatient, setSelectedExistingPatient] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated && user?.hospital) {
      api.get('/departments/hospital').then(setDepartments).catch(() => {});
    }
  }, [isAuthenticated, user]);

  useEffect(() => {
    if (selectedDept) {
      api.get(`/doctors?departmentId=${selectedDept.id}`).then(setDoctors).catch(() => {});
    }
  }, [selectedDept]);

  useEffect(() => {
    if (selectedDoctor && selectedDate) {
      setSlotsLoading(true);
      api.get(`/doctors/${selectedDoctor.id}/slots?date=${selectedDate}`)
        .then(s => { setSlots(s); setSlotsLoading(false); })
        .catch(() => setSlotsLoading(false));
    }
  }, [selectedDoctor, selectedDate]);

  // Debounced patient search
  useEffect(() => {
    if (searchQuery.length < 10) {
      setSearchedPatients([]);
      return;
    }
    const timer = setTimeout(async () => {
      setSearchLoading(true);
      try {
        const patients = await api.get(`/patients/hospital/search?phone=${searchQuery}`);
        setSearchedPatients(patients);
      } catch {
        setSearchedPatients([]);
      } finally {
        setSearchLoading(false);
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  const handleConfirm = async () => {
    setLoading(true); setError('');
    try {
      let patientId;
      if (!isNewPatient && selectedExistingPatient?.id) {
        patientId = selectedExistingPatient.id;
      } else {
        // Always create new patient if no existing selected (user feedback: create from DB)
        const newPatient = await api.post('/patients', {
          ...patientData,
          dateOfBirth: patientData.dateOfBirth || null,
        });
        patientId = newPatient?.data?.id;
      }

      const appt = await api.post('/appointments', {
        patientId,
        doctorId: selectedDoctor.id,
        appointmentDate: selectedDate,
        appointmentTime: selectedSlot.time,
        ...visitData,
      });
      setSuccess(appt?.data);
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    } finally {
      setLoading(false);
    }
  };

  const location = useLocation();
  const prefillPatient = location.state?.prefillPatient;
  useEffect(() => {
    if (prefillPatient) {
      setPatientData({
        firstName: prefillPatient.firstName || '',
        lastName: prefillPatient.lastName || '',
        phone: prefillPatient.phone || '',
        email: prefillPatient.email || '',
        gender: prefillPatient.gender || '',
        dateOfBirth: prefillPatient.dateOfBirth || '',
      });
    }
  }, [prefillPatient]);


  const handleReset = () => {
    setSuccess(null); setStep(1); setSelectedDept(null);
    setSelectedDoctor(null); setSelectedSlot(null); setSelectedDate('');
    setPatientData(prefillPatient || { firstName: '', lastName: '', phone: '', email: '', gender: '', dateOfBirth: '' });
    setVisitData({ reasonForVisit: '', symptoms: '', appointmentType: 'in_person' });
    setIsNewPatient(true);
    setSelectedExistingPatient(null);
    setSearchQuery('');
    setSearchedPatients([]);
    setError('');
  };

  if (success) return <SuccessScreen appointment={success} onReset={handleReset} />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Book Appointment</h1>
          <p className="page-subtitle">Schedule a consultation with our specialists</p>
        </div>
      </div>

      <StepIndicator current={step} />

      {step === 1 && (
        <Step1Department
          departments={departments}
          selected={selectedDept}
          onSelect={setSelectedDept}
          onNext={() => setStep(2)}
        />
      )}
      {step === 2 && (
        <Step2Doctor
          doctors={doctors}
          selected={selectedDoctor}
          onSelect={setSelectedDoctor}
          onNext={() => setStep(3)}
          onBack={() => setStep(1)}
          deptName={selectedDept?.name}
        />
      )}
      {step === 3 && (
        <Step3DateTime
          doctor={selectedDoctor}
          selectedDate={selectedDate}
          setSelectedDate={setSelectedDate}
          slots={slots}
          slotsLoading={slotsLoading}
          selectedSlot={selectedSlot}
          setSelectedSlot={setSelectedSlot}
          onNext={() => setStep(4)}
          onBack={() => setStep(2)}
        />
      )}
      {step === 4 && (
        <Step4PatientInfo
          patientData={patientData}
          setPatientData={setPatientData}
          visitData={visitData}
          setVisitData={setVisitData}
          isNewPatient={isNewPatient}
          setIsNewPatient={setIsNewPatient}
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          searchedPatients={searchedPatients}
          setSearchedPatients={setSearchedPatients}
          selectedExistingPatient={selectedExistingPatient}
          setSelectedExistingPatient={setSelectedExistingPatient}
          searchLoading={searchLoading}
          setSearchLoading={setSearchLoading}
          onNext={() => setStep(5)}
          onBack={() => setStep(3)}
        />
      )}
      {step === 5 && (
        <Step5Confirm
          dept={selectedDept}
          doctor={selectedDoctor}
          selectedDate={selectedDate}
          selectedSlot={selectedSlot}
          patientData={patientData}
          visitData={visitData}
          loading={loading}
          error={error}
          onConfirm={handleConfirm}
          onBack={() => setStep(4)}
        />
      )}
    </div>
  );
};

export default BookAppointment;
