import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';


// ── Indian Phone Validation ───────────────────────────────────
const PHONE_REGEX = /^(?:\+91|91|0)?[6-9]\d{9}$/;
const validatePhone = (v) => {
  if (!v) return '';
  if (!PHONE_REGEX.test(v.replace(/[\s-]/g, ''))) return 'Enter valid Indian mobile number';
  return '';
};

// ── Section wrapper ───────────────────────────────────────────
const Section = ({ title, icon, children }) => (
  <div className="pf-section">
    <div className="pf-section-header">
      <span className="pf-section-icon">{icon}</span>
      <h3 className="pf-section-title">{title}</h3>
    </div>
    <div className="pf-section-body">{children}</div>
  </div>
);

// ── Field ─────────────────────────────────────────────────────
const Field = ({ label, required, error, hint, children, half }) => (
  <div className={`pf-field ${half ? 'pf-field-half' : ''}`}>
    <label className="pf-label">
      {label}{required && <span className="pf-required">*</span>}
    </label>
    {children}
    {error && <span className="pf-error">⚠ {error}</span>}
    {hint && !error && <span className="pf-hint">{hint}</span>}
  </div>
);

// ── Empty State ───────────────────────────────────────────────
const SuccessScreen = ({ patient, onNew, onView }) => (
  <div className="pf-success">
    <div className="pf-success-icon">✅</div>
    <h2 className="pf-success-title">Patient Registered!</h2>
    <p className="pf-success-sub">
      {patient.firstName} {patient.lastName} has been successfully registered.
    </p>
    <div className="pf-success-card">
      {[
        ['Patient ID', patient.id?.slice(0, 8) + '...'],
        ['Name',       `${patient.firstName} ${patient.lastName}`],
        ['Phone',      patient.phone],
        ['Blood Group',patient.bloodGroup || '—'],
        ['Gender',     patient.gender || '—'],
      ].map(([k, v]) => (
        <div key={k} className="pf-success-row">
          <span className="pf-success-key">{k}</span>
          <span className="pf-success-val">{v}</span>
        </div>
      ))}
    </div>
    <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
      <button className="btn btn-primary" onClick={onNew}>Register Another Patient</button>
      <button className="btn btn-secondary" onClick={onView}>View All Patients</button>
    </div>
  </div>
);

// ── Step Indicator ────────────────────────────────────────────
const STEPS = ['Personal', 'Medical', 'Lifestyle', 'Emergency'];
const StepBar = ({ current }) => (
  <div className="pf-steps">
    {STEPS.map((s, i) => {
      const n = i + 1;
      return (
        <React.Fragment key={n}>
          <div className={`pf-step ${current === n ? 'active' : current > n ? 'done' : ''}`}>
            <div className="pf-step-circle">{current > n ? '✓' : n}</div>
            <div className="pf-step-label">{s}</div>
          </div>
          {i < STEPS.length - 1 && <div className={`pf-step-line ${current > n ? 'done' : ''}`} />}
        </React.Fragment>
      );
    })}
  </div>
);

// ── Main Component ────────────────────────────────────────────
const PatientForm = ({ prefillData, onSaved }) => {
  const navigate = useNavigate();
  const [step, setStep]       = useState(1);

  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');
  const [success, setSuccess] = useState(null);
  const [departments, setDepartments] = useState([]);

  // ── Form State ────────────────────────────────────────────
  const [personal, setPersonal] = useState({
    firstName: prefillData?.firstName || '',
    lastName:  prefillData?.lastName  || '',
    dateOfBirth: '', gender: '', phone: prefillData?.phone || '',
    email: prefillData?.email || '', address: '', pincode: '', city: '',
    aadharNumber: '', abhaId: '',
  });

  const [medical, setMedical] = useState({
    bloodGroup: '', height: '', weight: '',
    knownAllergies: '', chronicConditions: '', currentMedications: '',
    pastSurgeries: '', familyHistory: '',
    vaccinationHistory: '',
  });

  const [lifestyle, setLifestyle] = useState({
    smokingStatus: '', alcoholConsumption: '', occupation: '',
    insuranceProvider: '', insurancePolicyNumber: '',
    dietaryPreferences: '',
  });

  const [emergency, setEmergency] = useState({
    emergencyContactName: '', emergencyContactPhone: '',
    emergencyContactRelation: '',
  });

  const [touched, setTouched] = useState({});

  useEffect(() => {
    api.get('/departments').then(setDepartments).catch(() => {});
  }, []);

  const setP = k => e => setPersonal(p => ({ ...p, [k]: e.target.value }));
  const setM = k => e => setMedical(m  => ({ ...m, [k]: e.target.value }));
  const setL = k => e => setLifestyle(l => ({ ...l, [k]: e.target.value }));
  const setE = k => e => setEmergency(em => ({ ...em, [k]: e.target.value }));
  const touch = k => () => setTouched(t => ({ ...t, [k]: true }));

  const phoneErr = touched.phone ? validatePhone(personal.phone) : '';

  const validateStep = (s) => {
    if (s === 1) {
      if (!personal.firstName.trim()) return 'First name is required';
      if (!personal.lastName.trim())  return 'Last name is required';
      // Phone now optional; validatePhone returns '' if empty
      if (!personal.gender)            return 'Gender is required';
    }
    return '';
  };

  const goNext = () => {
    const err = validateStep(step);
    if (err) { setError(err); return; }
    setError('');
    setStep(s => s + 1);
  };

  const goBack = () => { setError(''); setStep(s => s - 1); };

  const handleSubmit = async () => {
    setLoading(true); setError('');
    try {
      const payload = {
        firstName: personal.firstName,
        lastName:  personal.lastName,
        dateOfBirth: personal.dateOfBirth || null,
        gender:    personal.gender,
        phone:     personal.phone,
        email:     personal.email || null,
        address:   [personal.address, personal.city, personal.pincode].filter(Boolean).join(', ') || null,
        bloodGroup: medical.bloodGroup || null,
        medicalHistory: [
          medical.chronicConditions && `Conditions: ${medical.chronicConditions}`,
          medical.pastSurgeries     && `Surgeries: ${medical.pastSurgeries}`,
          medical.familyHistory     && `Family: ${medical.familyHistory}`,
          medical.vaccinationHistory && `Vaccinations: ${medical.vaccinationHistory}`,
        ].filter(Boolean).join('\n') || null,
        allergies: medical.knownAllergies || null,
        emergencyContactName:  emergency.emergencyContactName  || null,
        emergencyContactPhone: emergency.emergencyContactPhone || null,
      };

      const saved = await api.post('/patients', payload);
      setSuccess(saved);
      if (onSaved) onSaved(saved);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <SuccessScreen
        patient={success}
        onNew={() => { setSuccess(null); setStep(1); setPersonal({ firstName: '', lastName: '', dateOfBirth: '', gender: '', phone: '', email: '', address: '', pincode: '', city: '', aadharNumber: '', abhaId: '' }); setMedical({ bloodGroup: '', height: '', weight: '', knownAllergies: '', chronicConditions: '', currentMedications: '', pastSurgeries: '', familyHistory: '', vaccinationHistory: '' }); }}
        onView={() => navigate('/patients')}

      />
    );
  }

  return (
    <div className="pf-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Patient Registration</h1>
          <p className="page-subtitle">Complete patient data entry form</p>
        </div>
      </div>

      <StepBar current={step} />

      {error && <div className="alert alert-error">{error}</div>}

      {/* ── STEP 1: Personal Information ── */}
      {step === 1 && (
        <div className="pf-card">
          <Section title="Personal Information" icon="👤">
            <div className="pf-grid">
              <Field label="First Name" required half error={touched.firstName && !personal.firstName ? 'Required' : ''}>
                <input value={personal.firstName} onChange={setP('firstName')} onBlur={touch('firstName')} placeholder="Enter first name" />
              </Field>
              <Field label="Last Name" required half error={touched.lastName && !personal.lastName ? 'Required' : ''}>
                <input value={personal.lastName} onChange={setP('lastName')} onBlur={touch('lastName')} placeholder="Enter last name" />
              </Field>
              <Field label="Date of Birth" half>
                <input type="date" value={personal.dateOfBirth} onChange={setP('dateOfBirth')} max={new Date().toISOString().split('T')[0]} />
              </Field>
              <Field label="Gender" required half error={touched.gender && !personal.gender ? 'Required' : ''}>
                <select value={personal.gender} onChange={setP('gender')} onBlur={touch('gender')}>
                  <option value="">Select gender</option>
                  <option value="male">Male</option>
                  <option value="female">Female</option>
                  <option value="other">Other</option>
                </select>
              </Field>
              <Field label="Mobile Number" hint="10-digit Indian mobile number (optional)" error={phoneErr}>
                <div className="pf-phone-wrap">
                  <span className="pf-phone-prefix">🇮🇳 +91</span>
                  <input
                    value={personal.phone}
                    onChange={e => setPersonal(p => ({ ...p, phone: e.target.value.replace(/[^\d\s-]/g, '') }))}
                    onBlur={touch('phone')}
                    placeholder="98765 43210"
                    maxLength={15}
                    style={{ paddingLeft: 70 }}
                  />
                </div>
              </Field>
              <Field label="Email Address" half>
                <input type="email" value={personal.email} onChange={setP('email')} placeholder="patient@email.com" />
              </Field>
              <Field label="Aadhar Number" half hint="12-digit Aadhar (optional)">
                <input value={personal.aadharNumber} onChange={setP('aadharNumber')} placeholder="XXXX XXXX XXXX" maxLength={14} />
              </Field>
              <Field label="ABHA ID" half hint="Ayushman Bharat Health Account">
                <input value={personal.abhaId} onChange={setP('abhaId')} placeholder="14-digit ABHA ID" />
              </Field>
            </div>
          </Section>

          <Section title="Address" icon="📍">
            <div className="pf-grid">
              <Field label="Street Address">
                <textarea value={personal.address} onChange={setP('address')} placeholder="House no, Street, Area..." rows={2} />
              </Field>
              <Field label="City" half>
                <input value={personal.city} onChange={setP('city')} placeholder="e.g. Mumbai" />
              </Field>
              <Field label="Pincode" half>
                <input value={personal.pincode} onChange={setP('pincode')} placeholder="6-digit pincode" maxLength={6} />
              </Field>
            </div>
          </Section>

          <div className="pf-nav">
            <button className="btn btn-primary" onClick={goNext}>Next: Medical History →</button>
          </div>
        </div>
      )}

      {/* ── STEP 2: Medical Information ── */}
      {step === 2 && (
        <div className="pf-card">
          <Section title="Vital Statistics" icon="📊">
            <div className="pf-grid">
              <Field label="Blood Group" half>
                <select value={medical.bloodGroup} onChange={setM('bloodGroup')}>
                  <option value="">Select blood group</option>
                  {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map(bg => (
                    <option key={bg} value={bg}>{bg}</option>
                  ))}
                </select>
              </Field>
              <Field label="Height (cm)" half hint="e.g. 170">
                <input type="number" value={medical.height} onChange={setM('height')} placeholder="170" min="50" max="250" />
              </Field>
              <Field label="Weight (kg)" half hint="e.g. 70">
                <input type="number" value={medical.weight} onChange={setM('weight')} placeholder="70" min="1" max="500" />
              </Field>
            </div>
          </Section>

          <Section title="Medical History" icon="🏥">
            <div className="pf-grid">
              <Field label="Known Allergies" hint="Drug, food, or environmental allergies">
                <textarea value={medical.knownAllergies} onChange={setM('knownAllergies')} placeholder="e.g. Penicillin, Peanuts, Dust..." rows={2} />
              </Field>
              <Field label="Chronic Conditions" hint="Ongoing medical conditions">
                <textarea value={medical.chronicConditions} onChange={setM('chronicConditions')} placeholder="e.g. Diabetes Type 2, Hypertension, Asthma..." rows={2} />
              </Field>
              <Field label="Current Medications" hint="Medicines currently being taken">
                <textarea value={medical.currentMedications} onChange={setM('currentMedications')} placeholder="e.g. Metformin 500mg, Amlodipine 5mg..." rows={2} />
              </Field>
              <Field label="Past Surgeries / Hospitalizations">
                <textarea value={medical.pastSurgeries} onChange={setM('pastSurgeries')} placeholder="e.g. Appendectomy 2018, Knee surgery 2020..." rows={2} />
              </Field>
              <Field label="Family Medical History" hint="Significant conditions in immediate family">
                <textarea value={medical.familyHistory} onChange={setM('familyHistory')} placeholder="e.g. Father: Heart disease, Mother: Diabetes..." rows={2} />
              </Field>
              <Field label="Vaccination History">
                <textarea value={medical.vaccinationHistory} onChange={setM('vaccinationHistory')} placeholder="e.g. COVID-19 (2x), Flu shot (2023)..." rows={2} />
              </Field>
            </div>
          </Section>

          <div className="pf-nav">
            <button className="btn btn-secondary" onClick={goBack}>← Back</button>
            <button className="btn btn-primary" onClick={goNext}>Next: Lifestyle →</button>
          </div>
        </div>
      )}

      {/* ── STEP 3: Lifestyle & Insurance ── */}
      {step === 3 && (
        <div className="pf-card">
          <Section title="Lifestyle" icon="🌿">
            <div className="pf-grid">
              <Field label="Smoking Status" half>
                <select value={lifestyle.smokingStatus} onChange={setL('smokingStatus')}>
                  <option value="">Select</option>
                  <option value="never">Never Smoked</option>
                  <option value="former">Former Smoker</option>
                  <option value="current">Current Smoker</option>
                  <option value="unknown">Unknown</option>
                </select>
              </Field>
              <Field label="Alcohol Consumption" half>
                <select value={lifestyle.alcoholConsumption} onChange={setL('alcoholConsumption')}>
                  <option value="">Select</option>
                  <option value="never">Never</option>
                  <option value="occasional">Occasional</option>
                  <option value="moderate">Moderate</option>
                  <option value="heavy">Heavy</option>
                  <option value="unknown">Unknown</option>
                </select>
              </Field>
              <Field label="Occupation" half>
                <input value={lifestyle.occupation} onChange={setL('occupation')} placeholder="e.g. Teacher, Engineer, Retired..." />
              </Field>
              <Field label="Dietary Preferences" half>
                <select value={lifestyle.dietaryPreferences} onChange={setL('dietaryPreferences')}>
                  <option value="">Select</option>
                  <option value="vegetarian">Vegetarian</option>
                  <option value="vegan">Vegan</option>
                  <option value="non-vegetarian">Non-Vegetarian</option>
                  <option value="jain">Jain</option>
                </select>
              </Field>
            </div>
          </Section>

          <Section title="Insurance Details" icon="🛡️">
            <div className="pf-grid">
              <Field label="Insurance Provider" half>
                <input value={lifestyle.insuranceProvider} onChange={setL('insuranceProvider')} placeholder="e.g. Star Health, HDFC ERGO..." />
              </Field>
              <Field label="Policy Number" half>
                <input value={lifestyle.insurancePolicyNumber} onChange={setL('insurancePolicyNumber')} placeholder="Policy / Member ID" />
              </Field>
            </div>
          </Section>

          <div className="pf-nav">
            <button className="btn btn-secondary" onClick={goBack}>← Back</button>
            <button className="btn btn-primary" onClick={goNext}>Next: Emergency Contact →</button>
          </div>
        </div>
      )}

      {/* ── STEP 4: Emergency Contact & Submit ── */}
      {step === 4 && (
        <div className="pf-card">
          <Section title="Emergency Contact" icon="🚨">
            <div className="pf-grid">
              <Field label="Contact Name" half>
                <input value={emergency.emergencyContactName} onChange={setE('emergencyContactName')} placeholder="Full name" />
              </Field>
              <Field label="Relationship" half>
                <select value={emergency.emergencyContactRelation} onChange={setE('emergencyContactRelation')}>
                  <option value="">Select relation</option>
                  {['Spouse', 'Parent', 'Child', 'Sibling', 'Friend', 'Relative', 'Other'].map(r => (
                    <option key={r} value={r.toLowerCase()}>{r}</option>
                  ))}
                </select>
              </Field>
              <Field label="Contact Phone" half hint="Indian mobile number">
                <input value={emergency.emergencyContactPhone} onChange={setE('emergencyContactPhone')} placeholder="98765 43210" keyboardType="phone-pad" />
              </Field>
            </div>
          </Section>

          {/* Review Summary */}
          <Section title="Review Summary" icon="📋">
            <div className="pf-review-grid">
              <div className="pf-review-block">
                <div className="pf-review-block-title">Personal</div>
                {[
                  ['Name',   `${personal.firstName} ${personal.lastName}`],
                  ['Phone',  personal.phone],
                  ['Gender', personal.gender || '—'],
                  ['DOB',    personal.dateOfBirth || '—'],
                ].map(([k, v]) => (
                  <div key={k} className="pf-review-row">
                    <span className="pf-review-key">{k}</span>
                    <span className="pf-review-val">{v}</span>
                  </div>
                ))}
              </div>
              <div className="pf-review-block">
                <div className="pf-review-block-title">Medical</div>
                {[
                  ['Blood Group', medical.bloodGroup || '—'],
                  ['Height',      medical.height ? `${medical.height} cm` : '—'],
                  ['Weight',      medical.weight ? `${medical.weight} kg` : '—'],
                  ['Allergies',   medical.knownAllergies || 'None'],
                ].map(([k, v]) => (
                  <div key={k} className="pf-review-row">
                    <span className="pf-review-key">{k}</span>
                    <span className="pf-review-val">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          </Section>

          <div className="pf-nav">
            <button className="btn btn-secondary" onClick={goBack}>← Back</button>
            <button className="btn btn-primary" disabled={loading} onClick={handleSubmit}>
              {loading ? 'Registering...' : '✓ Register Patient'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default PatientForm;
