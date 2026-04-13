import React, { useState, useEffect, useRef } from 'react';
import api from '../services/api';

// ── Common field wrapper ──────────────────────────────────────
const Field = ({ label, required, children, hint, col }) => (
  <div className={`pf-field ${col ? `pf-col-${col}` : ''}`}>
    {label && (
      <label className="pf-label">
        {label}{required && <span className="pf-required">*</span>}
      </label>
    )}
    {children}
    {hint && <span className="pf-hint">{hint}</span>}
  </div>
);

// ── Vital Sign Input ──────────────────────────────────────────
const VitalField = ({ label, unit, value, onChange, placeholder }) => (
  <div className="rx-vital-item">
    <label className="pf-label">{label}</label>
    <div className="rx-vital-wrap">
      <input value={value} onChange={onChange} placeholder={placeholder} className="rx-vital-input" />
      {unit && <span className="rx-vital-unit">{unit}</span>}
    </div>
  </div>
);

// ── Medicine Row ──────────────────────────────────────────────
const MedicineRow = ({ med, idx, onChange, onRemove }) => {
  const set = k => e => onChange(idx, k, e.target.value);
  const setB = k => e => onChange(idx, k, e.target.checked);

  return (
    <div className="rx-med-row">
      <div className="rx-med-num">{idx + 1}</div>
      <div className="rx-med-fields">
        <input className="rx-med-name" value={med.medicineName} onChange={set('medicineName')}
          placeholder="Medicine name (generic/brand)" />
        <div className="rx-med-details">
          <input value={med.dosage} onChange={set('dosage')} placeholder="Dosage (e.g. 500mg)" />
          <select value={med.frequency} onChange={set('frequency')}>
            <option value="">Frequency</option>
            <option value="Once daily">Once daily</option>
            <option value="Twice daily">Twice daily</option>
            <option value="Thrice daily">Thrice daily</option>
            <option value="Four times daily">Four times daily</option>
            <option value="Every 6 hours">Every 6 hours</option>
            <option value="Every 8 hours">Every 8 hours</option>
            <option value="At bedtime">At bedtime</option>
            <option value="As needed">As needed (SOS)</option>
            <option value="Weekly once">Weekly once</option>
          </select>
          <input value={med.duration} onChange={set('duration')} placeholder="Duration (e.g. 5 days)" />
          <select value={med.route} onChange={set('route')}>
            <option value="Oral">Oral</option>
            <option value="Injection">Injection</option>
            <option value="Topical">Topical</option>
            <option value="Inhaler">Inhaler</option>
            <option value="Drops">Drops</option>
            <option value="Sublingual">Sublingual</option>
            <option value="Suppository">Suppository</option>
            <option value="IV">IV</option>
          </select>
          <label className="rx-food-check">
            <input type="checkbox" checked={med.beforeFood} onChange={setB('beforeFood')} />
            <span>Before food</span>
          </label>
        </div>
        <input value={med.instructions} onChange={set('instructions')}
          placeholder="Special instructions (optional)" className="rx-med-instr" />
      </div>
      <button className="rx-med-remove" onClick={() => onRemove(idx)} title="Remove">✕</button>
    </div>
  );
};

// ── Lab Test Row ──────────────────────────────────────────────
const LabTestRow = ({ test, idx, onChange, onRemove }) => {
  const set = k => e => onChange(idx, k, e.target.value);
  const setB = k => e => onChange(idx, k, e.target.checked);

  return (
    <div className="rx-lab-row">
      <div className="rx-med-num">{idx + 1}</div>
      <div className="rx-lab-fields">
        <input value={test.testName} onChange={set('testName')} placeholder="Test name (e.g. CBC, Blood Sugar)" className="rx-lab-name" />
        <input value={test.instructions} onChange={set('instructions')} placeholder="Instructions (e.g. Fasting required)" />
        <label className="rx-food-check">
          <input type="checkbox" checked={test.isUrgent} onChange={setB('isUrgent')} />
          <span style={{ color: 'var(--primary)', fontWeight: 700 }}>🚨 Urgent</span>
        </label>
      </div>
      <button className="rx-med-remove" onClick={() => onRemove(idx)} title="Remove">✕</button>
    </div>
  );
};

// ── Print Prescription ────────────────────────────────────────
const printPrescription = (data) => {
  const { patient, doctor, diagnosis, medicines, labTests, prescriptionDate, vitalSigns, followUpDate, additionalNotes } = data;
  let vitals = {};
  try { vitals = JSON.parse(vitalSigns || '{}'); } catch {}

  const html = `
    <!DOCTYPE html><html><head><meta charset="UTF-8">
    <title>Prescription</title>
    <style>
      * { margin:0; padding:0; box-sizing:border-box; }
      body { font-family: Arial, sans-serif; font-size: 13px; color: #222; padding: 20px; }
      .header { border-bottom: 2px solid #c5522a; padding-bottom: 12px; margin-bottom: 12px; display:flex; justify-content:space-between; }
      .hospital-name { font-size: 20px; font-weight: 800; color: #c5522a; }
      .doctor-info { font-size: 12px; color: #555; margin-top: 4px; }
      .rx-symbol { font-size: 32px; color: #c5522a; font-weight: 900; }
      .patient-box { background: #f9f7f4; border: 1px solid #ddd; border-radius: 8px; padding: 10px 14px; margin-bottom: 12px; display:grid; grid-template-columns: 1fr 1fr 1fr; gap:6px; }
      .patient-box .item label { font-size:10px; text-transform:uppercase; color:#888; }
      .patient-box .item div { font-weight:700; }
      .vitals { display:flex; gap:12px; margin-bottom:12px; flex-wrap:wrap; }
      .vital { background:#fff8f0; border:1px solid #f0d8c0; border-radius:6px; padding:5px 10px; font-size:12px; }
      .vital span { font-weight:700; }
      h4 { font-size:13px; font-weight:800; text-transform:uppercase; letter-spacing:1px; color:#c5522a; border-bottom:1px solid #ddd; padding-bottom:5px; margin-bottom:8px; margin-top:14px; }
      .diag { background:#fef9e7; border-left:3px solid #c5522a; padding:8px 12px; border-radius:4px; font-weight:600; margin-bottom:10px; }
      table { width:100%; border-collapse:collapse; margin-bottom:10px; }
      th { background:#f5f0eb; font-size:11px; text-transform:uppercase; padding:6px 8px; text-align:left; color:#666; }
      td { padding:6px 8px; border-bottom:1px solid #f0e8e0; font-size:12px; }
      .urgent { background:#fff0f0; font-weight:700; color:#c0220a; }
      .footer { margin-top:30px; display:flex; justify-content:space-between; align-items:flex-end; border-top:1px dashed #ddd; padding-top:14px; }
      .sign-line { width:180px; border-top:1px solid #333; text-align:center; font-size:11px; color:#888; padding-top:4px; }
      .followup { background:#e6f4ee; border:1px solid #3d7a5e; border-radius:6px; padding:8px 12px; font-size:12px; }
      @media print { body { padding:0; } }
    </style></head><body>
    <div class="header">
      <div>
        <div class="hospital-name">✚ MediCare+</div>
        <div class="doctor-info">
          <strong>${doctor?.fullName || ''}</strong> · ${doctor?.specialization || ''}<br/>
          ${doctor?.qualification || ''}${doctor?.departmentName ? ' · ' + doctor.departmentName : ''}
        </div>
      </div>
      <div style="text-align:right">
        <div class="rx-symbol">℞</div>
        <div style="font-size:11px;color:#888">${prescriptionDate || new Date().toLocaleDateString('en-IN')}</div>
      </div>
    </div>

    <div class="patient-box">
      <div class="item"><label>Patient</label><div>${patient?.fullName || ''}</div></div>
      <div class="item"><label>Age / Gender</label><div>${patient?.gender || ''}</div></div>
      <div class="item"><label>Phone</label><div>${patient?.phone || ''}</div></div>
      ${patient?.bloodGroup ? `<div class="item"><label>Blood Group</label><div>${patient.bloodGroup}</div></div>` : ''}
    </div>

    ${Object.keys(vitals).some(k => vitals[k]) ? `
    <div class="vitals">
      ${vitals.bp       ? `<div class="vital">BP: <span>${vitals.bp}</span> mmHg</div>` : ''}
      ${vitals.pulse    ? `<div class="vital">Pulse: <span>${vitals.pulse}</span> bpm</div>` : ''}
      ${vitals.temp     ? `<div class="vital">Temp: <span>${vitals.temp}</span>°F</div>` : ''}
      ${vitals.weight   ? `<div class="vital">Weight: <span>${vitals.weight}</span> kg</div>` : ''}
      ${vitals.height   ? `<div class="vital">Height: <span>${vitals.height}</span> cm</div>` : ''}
      ${vitals.spo2     ? `<div class="vital">SpO₂: <span>${vitals.spo2}</span>%</div>` : ''}
    </div>` : ''}

    <h4>Diagnosis</h4>
    <div class="diag">${diagnosis || ''}</div>

    ${medicines?.length ? `
    <h4>Medications</h4>
    <table>
      <tr><th>#</th><th>Medicine</th><th>Dosage</th><th>Frequency</th><th>Duration</th><th>Route</th><th>Instructions</th></tr>
      ${medicines.map((m, i) => `
        <tr>
          <td>${i+1}</td>
          <td><strong>${m.medicineName}</strong></td>
          <td>${m.dosage || '—'}</td>
          <td>${m.frequency || '—'}</td>
          <td>${m.duration || '—'}</td>
          <td>${m.route || 'Oral'}</td>
          <td>${m.beforeFood ? 'Before food. ' : 'After food. '}${m.instructions || ''}</td>
        </tr>`).join('')}
    </table>` : ''}

    ${labTests?.length ? `
    <h4>Investigations</h4>
    <table>
      <tr><th>#</th><th>Test Name</th><th>Instructions</th><th>Priority</th></tr>
      ${labTests.map((t, i) => `
        <tr class="${t.isUrgent ? 'urgent' : ''}">
          <td>${i+1}</td>
          <td>${t.testName}</td>
          <td>${t.instructions || '—'}</td>
          <td>${t.isUrgent ? '🚨 URGENT' : 'Routine'}</td>
        </tr>`).join('')}
    </table>` : ''}

    ${additionalNotes ? `<h4>Notes</h4><p style="font-size:12px;line-height:1.6">${additionalNotes}</p>` : ''}

    ${followUpDate ? `<div class="followup" style="margin-top:12px">📅 Follow-up on: <strong>${followUpDate}</strong></div>` : ''}

    <div class="footer">
      <div style="font-size:11px;color:#888">This prescription is valid for 30 days from the date of issue.</div>
      <div class="sign-line">Doctor's Signature</div>
    </div>
    </body></html>`;

  const w = window.open('', '_blank');
  w.document.write(html);
  w.document.close();
  w.print();
};

// ── Main Component ────────────────────────────────────────────
const PrescriptionForm = ({ appointmentId, prefillPatient, prefillDoctor, onSaved, onNavigate }) => {
  const [patients,    setPatients]    = useState([]);
  const [doctors,     setDoctors]     = useState([]);
  const [appointments,setAppointments]= useState([]);
  const [loading,     setLoading]     = useState(false);
  const [error,       setError]       = useState('');
  const [success,     setSuccess]     = useState(null);
  const [patientSearch,setPatientSearch] = useState('');
  const [showPatientDrop, setShowPatientDrop] = useState(false);

  const [form, setForm] = useState({
    patientId:      prefillPatient?.id     || '',
    doctorId:       prefillDoctor?.id      || '',
    appointmentId:  appointmentId          || '',
    diagnosis:      '',
    chiefComplaint: '',
    examinationNotes: '',
    followUpDate:   '',
    followUpInstructions: '',
    dietInstructions: '',
    activityRestrictions: '',
    additionalNotes: '',
  });

  const [vitals, setVitals] = useState({ bp: '', pulse: '', temp: '', weight: '', height: '', spo2: '', rr: '' });
  const [medicines, setMedicines] = useState([{ medicineName: '', dosage: '', frequency: '', duration: '', route: 'Oral', beforeFood: false, instructions: '', sortOrder: 0 }]);
  const [labTests,  setLabTests]  = useState([]);

  const [selectedPatient, setSelectedPatient] = useState(prefillPatient || null);
  const [selectedDoctor,  setSelectedDoctor]  = useState(prefillDoctor  || null);
  const [commonMedicines, setCommonMedicines] = useState([]);
  const [commonTests,     setCommonTests]     = useState([]);

  useEffect(() => {
    api.get('/doctors/hospital/list').then(setDoctors).catch(() => {});
    api.get('/appointments/hospital').then(setAppointments).catch(() => {});
    
    // Fetch common lists
    api.get('/prescriptions/common-medicines/hospital').then(setCommonMedicines).catch(() => {});
    api.get('/prescriptions/common-tests/hospital').then(setCommonTests).catch(() => {});
  }, []);

  useEffect(() => {
    if (patientSearch.length >= 2) {
      api.get(`/patients/hospital/search?phone=${patientSearch}`).then(setPatients).catch(() => {});
    }
  }, [patientSearch]);

  const setF = k => e => setForm(f => ({ ...f, [k]: e.target.value }));
  const setV = k => e => setVitals(v => ({ ...v, [k]: e.target.value }));

  const addMedicine = () => setMedicines(m => [...m, { medicineName: '', dosage: '', frequency: '', duration: '', route: 'Oral', beforeFood: false, instructions: '', sortOrder: m.length }]);
  const removeMedicine = (i) => setMedicines(m => m.filter((_, idx) => idx !== i));
  const updateMedicine = (i, k, v) => setMedicines(m => m.map((med, idx) => idx === i ? { ...med, [k]: v } : med));

  const addLabTest = () => setLabTests(t => [...t, { testName: '', instructions: '', isUrgent: false, sortOrder: t.length }]);
  const removeLabTest = (i) => setLabTests(t => t.filter((_, idx) => idx !== i));
  const updateLabTest = (i, k, v) => setLabTests(t => t.map((test, idx) => idx === i ? { ...test, [k]: v } : test));

  // COMMON lists now dynamic from API

  const handleSubmit = async () => {
    if (!form.patientId || !form.doctorId || !form.diagnosis.trim()) {
      setError('Patient, Doctor, and Diagnosis are required.');
      return;
    }
    const validMeds = medicines.filter(m => m.medicineName.trim());
    if (validMeds.length === 0) {
      setError('Add at least one medicine.');
      return;
    }
    setLoading(true); setError('');
    try {
      const payload = {
        ...form,
        appointmentId: form.appointmentId || null,
        vitalSigns: JSON.stringify(vitals),
        medicines:  validMeds.map((m, i) => ({ ...m, sortOrder: i })),
        labTests:   labTests.filter(t => t.testName.trim()).map((t, i) => ({ ...t, sortOrder: i })),
      };
      const saved = await api.post('/prescriptions', payload);
      setSuccess(saved?.data);
      if (onSaved) onSaved(saved?.data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    let vitalsObj = {};
    try { vitalsObj = JSON.parse(success?.vitalSigns || '{}'); } catch {}
    printPrescription({ ...success, vitalSigns: success?.vitalSigns });
  };

  if (success) {
    return (
      <div className="pf-success">
        <div className="pf-success-icon">📋</div>
        <h2 className="pf-success-title">Prescription Saved!</h2>
        <p className="pf-success-sub">Prescription for {success.patient?.fullName} has been recorded.</p>
        <div className="pf-success-card">
          {[
            ['Patient',  success.patient?.fullName],
            ['Doctor',   success.doctor?.fullName],
            ['Date',     success.prescriptionDate],
            ['Diagnosis',success.diagnosis],
            ['Medicines',`${success.medicines?.length || 0} prescribed`],
            ['Lab Tests',`${success.labTests?.length  || 0} ordered`],
          ].map(([k, v]) => (
            <div key={k} className="pf-success-row">
              <span className="pf-success-key">{k}</span>
              <span className="pf-success-val">{v}</span>
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>

          <button className="btn btn-primary" onClick={handlePrint}>🖨️ Print Prescription</button>
          {success.patient?.email && (
            <button className="btn btn-success" onClick={async () => {
              try {
                await api.post(`/prescriptions/${success.id}/send?mode=email`);
                alert('✅ Prescription sent via email successfully!');
              } catch (e) {
                alert('❌ Email send failed: ' + (e.response?.data?.message || e.message));
              }
            }}>📧 Send Email</button>
          )}
          {success.patient?.phone && (
            <>
              <button className="btn btn-success" style={{ backgroundColor: '#25D366' }} onClick={async () => {
                try {
                  await api.post(`/prescriptions/${success.id}/send?mode=whatsapp`);
                  alert('✅ Prescription sent via WhatsApp successfully!');
                } catch (e) {
                  alert('❌ WhatsApp send failed: ' + (e.response?.data?.message || e.message));
                }
              }}>📱 Send WhatsApp</button>
              <button className="btn btn-success" style={{ backgroundColor: '#007bff' }} onClick={async () => {
                try {
                  await api.post(`/prescriptions/${success.id}/send?mode=sms`);
                  alert('✅ Prescription sent via SMS successfully!');
                } catch (e) {
                  alert('❌ SMS send failed: ' + (e.response?.data?.message || e.message));
                }
              }}>📱 Send SMS</button>
            </>
          )}
          <button className="btn btn-secondary" onClick={() => { setSuccess(null); setForm({ patientId: '', doctorId: '', appointmentId: '', diagnosis: '', chiefComplaint: '', examinationNotes: '', followUpDate: '', followUpInstructions: '', dietInstructions: '', activityRestrictions: '', additionalNotes: '' }); setMedicines([{ medicineName: '', dosage: '', frequency: '', duration: '', route: 'Oral', beforeFood: false, instructions: '' }]); setLabTests([]); setSelectedPatient(null); setSelectedDoctor(null); }}>New Prescription</button>
          <button className="btn btn-secondary" onClick={() => onNavigate && onNavigate('appointments')}>View Appointments</button>
        </div>
      </div>
    );
  }


  return (
    <div className="pf-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Write Prescription</h1>
          <p className="page-subtitle">Create a new prescription for a patient</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-secondary btn-sm" onClick={() => onNavigate && onNavigate('patients')}>
            + Register Patient
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {/* ── Patient & Doctor Selection ── */}
      <div className="rx-header-card card">
        <div className="form-grid">
          {/* Patient Search */}
          <div className="form-group">
            <label>Patient *</label>
            {selectedPatient ? (
              <div className="rx-selected-patient">
                <div>
                  <div style={{ fontWeight: 700 }}>{selectedPatient.fullName}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{selectedPatient.phone} · {selectedPatient.bloodGroup || 'Blood group unknown'}</div>
                </div>
                <button className="rx-clear-btn" onClick={() => { setSelectedPatient(null); setForm(f => ({ ...f, patientId: '' })); }}>✕ Change</button>
              </div>
            ) : (
              <div style={{ position: 'relative' }}>
                <input
                  value={patientSearch}
                  onChange={e => { setPatientSearch(e.target.value); setShowPatientDrop(true); }}
                  placeholder="Search by phone number..."
                />
                {showPatientDrop && patients.length > 0 && (
                  <div className="rx-dropdown">
                    {patients.map(p => (
                      <div key={p.id} className="rx-dropdown-item" onClick={() => { setSelectedPatient(p); setForm(f => ({ ...f, patientId: p.id })); setPatientSearch(''); setShowPatientDrop(false); }}>
                        <div style={{ fontWeight: 600 }}>{p.fullName}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{p.phone}</div>
                      </div>
                    ))}
                  </div>
                )}
                {patientSearch.length < 2 && (
                  <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>Enter phone number to search patient</div>
                )}
              </div>
            )}
          </div>

          {/* Doctor Selection */}
          <div className="form-group">
            <label>Doctor *</label>
            {selectedDoctor ? (
              <div className="rx-selected-patient">
                <div>
                  <div style={{ fontWeight: 700 }}>{selectedDoctor.fullName}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{selectedDoctor.specialization}</div>
                </div>
                <button className="rx-clear-btn" onClick={() => { setSelectedDoctor(null); setForm(f => ({ ...f, doctorId: '' })); }}>✕ Change</button>
              </div>
            ) : (
              <select value={form.doctorId} onChange={e => { setF('doctorId')(e); const d = doctors.find(d => d.id === e.target.value); setSelectedDoctor(d || null); }}>
                <option value="">Select doctor</option>
                {doctors.map(d => <option key={d.id} value={d.id}>Dr. {d.firstName} {d.lastName} — {d.specialization}</option>)}
              </select>
            )}
          </div>

          {/* Appointment Link */}
          <div className="form-group">
            <label>Link to Appointment (optional)</label>
            <select value={form.appointmentId} onChange={setF('appointmentId')}>
              <option value="">Select appointment</option>
              {appointments.filter(a => !form.patientId || a.patient?.id === form.patientId).slice(0, 20).map(a => (
                <option key={a.id} value={a.id}>
                  {a.tokenNumber} — {a.patient?.fullName} — {a.appointmentDate}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Prescription Date</label>
            <input type="date" value={form.prescriptionDate || new Date().toISOString().split('T')[0]} onChange={setF('prescriptionDate')} />
          </div>
        </div>
      </div>

      {/* ── Vitals ── */}
      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-title">🩺 Vital Signs</div>
        <div className="rx-vitals-grid">
          <VitalField label="Blood Pressure" unit="mmHg" value={vitals.bp}     onChange={setV('bp')}     placeholder="120/80" />
          <VitalField label="Pulse Rate"     unit="bpm"  value={vitals.pulse}  onChange={setV('pulse')}  placeholder="72" />
          <VitalField label="Temperature"    unit="°F"   value={vitals.temp}   onChange={setV('temp')}   placeholder="98.6" />
          <VitalField label="SpO₂"           unit="%"    value={vitals.spo2}   onChange={setV('spo2')}   placeholder="98" />
          <VitalField label="Weight"         unit="kg"   value={vitals.weight} onChange={setV('weight')} placeholder="70" />
          <VitalField label="Height"         unit="cm"   value={vitals.height} onChange={setV('height')} placeholder="170" />
          <VitalField label="Resp. Rate"     unit="/min" value={vitals.rr}     onChange={setV('rr')}     placeholder="16" />
        </div>
      </div>

      {/* ── Clinical Notes ── */}
      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-title">📝 Clinical Notes</div>
        <div className="form-grid">
          <div className="form-group">
            <label>Chief Complaint</label>
            <textarea value={form.chiefComplaint} onChange={setF('chiefComplaint')} placeholder="Patient's main complaint..." rows={2} />
          </div>
          <div className="form-group">
            <label>Examination Findings</label>
            <textarea value={form.examinationNotes} onChange={setF('examinationNotes')} placeholder="Physical examination findings..." rows={2} />
          </div>
          <div className="form-group full">
            <label>Diagnosis <span className="pf-required">*</span></label>
            <textarea value={form.diagnosis} onChange={setF('diagnosis')} placeholder="Primary and secondary diagnosis..." rows={2} />
          </div>
        </div>
      </div>

      {/* ── Medicines ── */}
      <div className="card" style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div className="card-title" style={{ margin: 0 }}>💊 Medicines</div>
          <button className="btn btn-primary btn-sm" onClick={addMedicine}>+ Add Medicine</button>
        </div>

        {/* Quick add common medicines */}
        <div className="rx-quick-add">
          <span style={{ fontSize: 12, color: 'var(--text-muted)', fontWeight: 600 }}>Quick add ({commonMedicines?.length}):</span>
          {commonMedicines?.slice(0, 12).map(m => (
            <button key={m} className="rx-quick-btn" onClick={() => setMedicines(ms => [...ms, { medicineName: m, dosage: '', frequency: '', duration: '', route: 'Oral', beforeFood: false, instructions: '', sortOrder: ms.length }])}>
              {m}
            </button>
          ))}
          {commonMedicines?.length > 12 && (
            <button className="rx-quick-btn" style={{ fontSize: 11 }} onClick={() => {}}>
              +{commonMedicines?.length - 12} more...
            </button>
          )}
        </div>

        <div className="rx-med-header">
          <span style={{ width: 24 }}>#</span>
          <span style={{ flex: 1 }}>Medicine Name · Dosage · Frequency · Duration · Route</span>
        </div>

        {medicines.map((med, i) => (
          <MedicineRow key={i} med={med} idx={i} onChange={updateMedicine} onRemove={removeMedicine} />
        ))}

        {medicines.length === 0 && (
          <div style={{ textAlign: 'center', padding: 24, color: 'var(--text-muted)' }}>
            No medicines added. Click "+ Add Medicine" to add.
          </div>
        )}
      </div>

      {/* ── Lab Tests ── */}
      <div className="card" style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div className="card-title" style={{ margin: 0 }}>🧪 Investigations / Lab Tests</div>
          <button className="btn btn-secondary btn-sm" onClick={addLabTest}>+ Add Test</button>
        </div>

        <div className="rx-quick-add">
          <span style={{ fontSize: 12, color: 'var(--text-muted)', fontWeight: 600 }}>Quick add ({commonTests?.length}):</span>
          {commonTests?.slice(0, 12).map(t => (
            <button key={t} className="rx-quick-btn" onClick={() => setLabTests(ts => [...ts, { testName: t, instructions: '', isUrgent: false, sortOrder: ts.length }])}>
              {t}
            </button>
          ))}
          {commonTests?.length > 12 && (
            <button className="rx-quick-btn" style={{ fontSize: 11 }} onClick={() => {}}>
              +{commonTests?.length - 12} more...
            </button>
          )}
        </div>

        {labTests.map((test, i) => (
          <LabTestRow key={i} test={test} idx={i} onChange={updateLabTest} onRemove={removeLabTest} />
        ))}

        {labTests.length === 0 && (
          <div style={{ textAlign: 'center', padding: 20, color: 'var(--text-muted)', fontSize: 13 }}>
            No investigations ordered.
          </div>
        )}
      </div>

      {/* ── Follow Up & Instructions ── */}
      <div className="card" style={{ marginBottom: 24 }}>
        <div className="card-title">📅 Follow Up & Instructions</div>
        <div className="form-grid">
          <div className="form-group">
            <label>Follow-up Date</label>
            <input type="date" value={form.followUpDate} onChange={setF('followUpDate')} min={new Date().toISOString().split('T')[0]} />
          </div>
          <div className="form-group">
            <label>Follow-up Instructions</label>
            <input value={form.followUpInstructions} onChange={setF('followUpInstructions')} placeholder="e.g. Return if fever persists..." />
          </div>
          <div className="form-group">
            <label>Diet Instructions</label>
            <textarea value={form.dietInstructions} onChange={setF('dietInstructions')} placeholder="Dietary advice..." rows={2} />
          </div>
          <div className="form-group">
            <label>Activity Restrictions</label>
            <textarea value={form.activityRestrictions} onChange={setF('activityRestrictions')} placeholder="Rest / avoid strenuous activity..." rows={2} />
          </div>
          <div className="form-group full">
            <label>Additional Notes</label>
            <textarea value={form.additionalNotes} onChange={setF('additionalNotes')} placeholder="Any other notes or instructions..." rows={2} />
          </div>
        </div>
      </div>

      {/* ── Actions ── */}
      <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
        <button className="btn btn-secondary" onClick={() => onNavigate && onNavigate('appointments')}>Cancel</button>
        <button className="btn btn-primary" disabled={loading} onClick={handleSubmit}>
          {loading ? 'Saving...' : '✓ Save Prescription'}
        </button>
      </div>
    </div>
  );
};

export default PrescriptionForm;
