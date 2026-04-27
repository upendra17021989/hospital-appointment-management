import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { LoadingSpinner, Badge, EmptyState, Modal } from '../components/Common';

const DAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const LANGUAGES = ['English', 'Hindi', 'Marathi', 'Gujarati', 'Malayalam', 'Tamil', 'Telugu', 'Punjabi', 'Bengali', 'Kannada'];

const EMPTY_FORM = {
  firstName: '', lastName: '', specialization: '', qualification: '',
  experienceYears: '', phone: '', email: '', bio: '',
  consultationFee: '', isAvailable: true, departmentId: '',
  languagesSpoken: [],
};

const EMPTY_SCHEDULE = {
  dayOfWeek: '1', startTime: '09:00', endTime: '17:00',
  slotDurationMinutes: '30', maxAppointments: '20', isActive: true,
};

// ── Doctor Form Modal ─────────────────────────────────────────
const DoctorFormModal = ({ doctor, departments, onSave, onClose }) => {
  const isEdit = !!doctor;
  const [form, setForm] = useState(isEdit ? {
    firstName:        doctor.firstName || '',
    lastName:         doctor.lastName || '',
    specialization:   doctor.specialization || '',
    qualification:    doctor.qualification || '',
    experienceYears:  doctor.experienceYears || '',
    phone:            doctor.phone || '',
    email:            doctor.email || '',
    bio:              doctor.bio || '',
    consultationFee:  doctor.consultationFee || '',
    isAvailable:      doctor.isAvailable ?? true,
    departmentId:     doctor.department?.id || '',
    languagesSpoken:  doctor.languagesSpoken || [],
  } : { ...EMPTY_FORM });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const set = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.value }));
  const setCheck = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.checked }));

  const toggleLang = (lang) => {
    setForm(f => ({
      ...f,
      languagesSpoken: f.languagesSpoken.includes(lang)
        ? f.languagesSpoken.filter(l => l !== lang)
        : [...f.languagesSpoken, lang],
    }));
  };

  const handleSave = async () => {
    if (!form.firstName || !form.lastName || !form.specialization || !form.departmentId) {
      setError('First name, last name, specialization and department are required.');
      return;
    }
    setSaving(true); setError('');
    try {
      const payload = {
        ...form,
        experienceYears: Number(form.experienceYears) || 0,
        consultationFee: Number(form.consultationFee) || 0,
      };
      await onSave(payload);
      onClose();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal title={isEdit ? 'Edit Doctor' : 'Add New Doctor'} onClose={onClose}>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="form-grid">
        <div className="form-group">
          <label>First Name *</label>
          <input value={form.firstName} onChange={set('firstName')} placeholder="e.g. Rajesh" />
        </div>
        <div className="form-group">
          <label>Last Name *</label>
          <input value={form.lastName} onChange={set('lastName')} placeholder="e.g. Sharma" />
        </div>
        <div className="form-group">
          <label>Specialization *</label>
          <input value={form.specialization} onChange={set('specialization')} placeholder="e.g. Cardiologist" />
        </div>
        <div className="form-group">
          <label>Department *</label>
          <select value={form.departmentId} onChange={set('departmentId')}>
            <option value="">Select department</option>
            {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </div>
        <div className="form-group">
          <label>Qualification</label>
          <input value={form.qualification} onChange={set('qualification')} placeholder="e.g. MBBS, MD Cardiology" />
        </div>
        <div className="form-group">
          <label>Experience (Years)</label>
          <input type="number" min="0" value={form.experienceYears} onChange={set('experienceYears')} placeholder="e.g. 10" />
        </div>
        <div className="form-group">
          <label>Phone</label>
          <input value={form.phone} onChange={set('phone')} placeholder="+91 98765 43210" />
        </div>
        <div className="form-group">
          <label>Email</label>
          <input type="email" value={form.email} onChange={set('email')} placeholder="dr.name@hospital.com" />
        </div>
        <div className="form-group">
          <label>Consultation Fee (₹)</label>
          <input type="number" min="0" value={form.consultationFee} onChange={set('consultationFee')} placeholder="e.g. 1000" />
        </div>
        <div className="form-group" style={{ justifyContent: 'center' }}>
          <label>Availability</label>
          <label className="dm-toggle">
            <input type="checkbox" checked={form.isAvailable} onChange={setCheck('isAvailable')} />
            <span className="dm-toggle__slider" />
            <span className="dm-toggle__label">{form.isAvailable ? 'Available' : 'Unavailable'}</span>
          </label>
        </div>
        <div className="form-group full">
          <label>Bio</label>
          <textarea value={form.bio} onChange={set('bio')} placeholder="Brief description of the doctor's expertise..." />
        </div>
        <div className="form-group full">
          <label>Languages Spoken</label>
          <div className="dm-lang-grid">
            {LANGUAGES.map(lang => (
              <label key={lang} className={`dm-lang-pill ${form.languagesSpoken.includes(lang) ? 'selected' : ''}`}>
                <input type="checkbox" checked={form.languagesSpoken.includes(lang)} onChange={() => toggleLang(lang)} style={{ display: 'none' }} />
                {lang}
              </label>
            ))}
          </div>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 10, marginTop: 24, justifyContent: 'flex-end' }}>
        <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={saving} onClick={handleSave}>
          {saving ? 'Saving...' : isEdit ? '✓ Update Doctor' : '✓ Add Doctor'}
        </button>
      </div>
    </Modal>
  );
};

// ── Schedule Modal ────────────────────────────────────────────
const ScheduleModal = ({ doctor, onClose }) => {
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({ ...EMPTY_SCHEDULE });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    api.get(`/doctors/${doctor.id}/schedules`)
      .then(data => { setSchedules(data || []); setLoading(false); })
      .catch(() => setLoading(false));
  }, [doctor.id]);

  const set = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.value }));

  const handleAdd = async () => {
    setSaving(true); setError('');
    try {
      const payload = {
        doctorId: doctor.id,
        dayOfWeek: Number(form.dayOfWeek),
        startTime: form.startTime,
        endTime: form.endTime,
        slotDurationMinutes: Number(form.slotDurationMinutes),
        maxAppointments: Number(form.maxAppointments),
        isActive: true,
      };
      const saved = await api.post('/doctors/schedules', payload);
      setSchedules(s => [...s, saved]);
      setForm({ ...EMPTY_SCHEDULE });
      setSuccess('Schedule added successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this schedule?')) return;
    try {
      await api.delete(`/doctors/schedules/${id}`);
      setSchedules(s => s.filter(sc => sc.id !== id));
    } catch (e) {
      setError(e.message);
    }
  };

  const handleToggle = async (schedule) => {
    try {
      const updated = await api.patch(`/doctors/schedules/${schedule.id}/toggle`, {});
      setSchedules(s => s.map(sc => sc.id === schedule.id ? updated : sc));
    } catch (e) {
      setError(e.message);
    }
  };

  const groupedByDay = DAYS.map((day, i) => ({
    day,
    dayIndex: i,
    schedules: schedules.filter(s => s.dayOfWeek === i),
  }));

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal dm-schedule-modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <div className="modal-title">Manage Schedules</div>
            <div style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 3 }}>
              Dr. {doctor.firstName} {doctor.lastName} — {doctor.department?.name}
            </div>
          </div>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        {/* Add Schedule Form */}
        <div className="dm-schedule-form card" style={{ marginBottom: 24 }}>
          <div className="card-title" style={{ fontSize: 14 }}>➕ Add New Schedule</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
            <div className="form-group">
              <label>Day</label>
              <select value={form.dayOfWeek} onChange={set('dayOfWeek')}>
                {DAYS.map((d, i) => <option key={i} value={i}>{d}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Start Time</label>
              <input type="time" value={form.startTime} onChange={set('startTime')} />
            </div>
            <div className="form-group">
              <label>End Time</label>
              <input type="time" value={form.endTime} onChange={set('endTime')} />
            </div>
            <div className="form-group">
              <label>Slot Duration (min)</label>
              <select value={form.slotDurationMinutes} onChange={set('slotDurationMinutes')}>
                {[15, 20, 30, 45, 60].map(m => <option key={m} value={m}>{m} min</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Max Appointments</label>
              <input type="number" min="1" value={form.maxAppointments} onChange={set('maxAppointments')} />
            </div>
            <div className="form-group" style={{ justifyContent: 'flex-end' }}>
              <label>&nbsp;</label>
              <button className="btn btn-primary" disabled={saving} onClick={handleAdd}>
                {saving ? 'Adding...' : 'Add Slot'}
              </button>
            </div>
          </div>
        </div>

        {/* Weekly Schedule View */}
        <div className="dm-weekly">
          {loading ? <LoadingSpinner /> : groupedByDay.map(({ day, schedules: daySched }) => (
            <div key={day} className={`dm-day-row ${daySched.length > 0 ? 'has-slots' : ''}`}>
              <div className="dm-day-label">{day.slice(0, 3).toUpperCase()}</div>
              <div className="dm-day-slots">
                {daySched.length === 0 ? (
                  <span className="dm-no-slot">No schedule</span>
                ) : daySched.map(sc => (
                  <div key={sc.id} className={`dm-slot-chip ${sc.isActive ? 'active' : 'inactive'}`}>
                    <span>{sc.startTime} – {sc.endTime}</span>
                    <span className="dm-slot-meta">{sc.slotDurationMinutes}min · {sc.maxAppointments} slots</span>
                    <div className="dm-slot-actions">
                      <button onClick={() => handleToggle(sc)} title={sc.isActive ? 'Deactivate' : 'Activate'}
                        className={`dm-chip-btn ${sc.isActive ? 'deactivate' : 'activate'}`}>
                        {sc.isActive ? '⏸' : '▶'}
                      </button>
                      <button onClick={() => handleDelete(sc.id)} className="dm-chip-btn delete" title="Delete">✕</button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

// ── Delete Confirm Modal ──────────────────────────────────────
const DeleteModal = ({ doctor, onConfirm, onClose, deleting }) => (
  <Modal title="Delete Doctor" onClose={onClose}>
    <div style={{ textAlign: 'center', padding: '8px 0 24px' }}>
      <div style={{ fontSize: 52, marginBottom: 16 }}>⚠️</div>
      <p style={{ fontSize: 15, marginBottom: 8 }}>
        Are you sure you want to delete <strong>Dr. {doctor.firstName} {doctor.lastName}</strong>?
      </p>
      <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
        This will permanently remove the doctor and all their schedules. This action cannot be undone.
      </p>
    </div>
    <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
      <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
      <button className="btn btn-danger" disabled={deleting} onClick={onConfirm}>
        {deleting ? 'Deleting...' : 'Yes, Delete'}
      </button>
    </div>
  </Modal>
);

// ── Main DoctorManagement Page ────────────────────────────────
const DoctorManagement = () => {
  const [doctors, setDoctors] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [deptFilter, setDeptFilter] = useState('');
  const [showAddEdit, setShowAddEdit] = useState(false);
  const [editDoctor, setEditDoctor] = useState(null);
  const [scheduleDoctor, setScheduleDoctor] = useState(null);
  const [deleteDoctor, setDeleteDoctor] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState('');

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3500); };

  const fetchDoctors = () => {
    setLoading(true);
    Promise.all([
      api.get('/doctors/hospital/list').catch(() => []),
      api.get('/departments/hospital').catch(() => []),
    ]).then(([d, dept]) => {
      setDoctors(d || []);
      setDepartments(dept || []);
      setLoading(false);
    });
  };

  useEffect(() => { fetchDoctors(); }, []);

  const handleSave = async (payload) => {
    if (editDoctor) {
      await api.put(`/doctors/${editDoctor.id}`, payload);
      showToast(`Dr. ${payload.firstName} ${payload.lastName} updated successfully.`);
    } else {
      await api.post('/doctors', payload);
      showToast(`Dr. ${payload.firstName} ${payload.lastName} added successfully.`);
    }
    fetchDoctors();
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await api.delete(`/doctors/${deleteDoctor.id}`);
      showToast(`Dr. ${deleteDoctor.firstName} ${deleteDoctor.lastName} deleted.`);
      setDeleteDoctor(null);
      fetchDoctors();
    } catch (e) {
      alert(e.message);
    } finally {
      setDeleting(false);
    }
  };

  const filtered = doctors.filter(d => {
    const q = search.toLowerCase();
    const matchSearch = !search
      || d.fullName?.toLowerCase().includes(q)
      || d.specialization?.toLowerCase().includes(q)
      || d.email?.toLowerCase().includes(q);
    const matchDept = !deptFilter || d.department?.id === deptFilter;
    return matchSearch && matchDept;
  });

  return (
    <div>
      {/* Toast */}
      {toast && (
        <div className="dm-toast">✓ {toast}</div>
      )}

      {/* Page Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Doctor Management</h1>
          <p className="page-subtitle">{doctors.length} doctors registered</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setEditDoctor(null); setShowAddEdit(true); }}>
          + Add New Doctor
        </button>
      </div>

      {/* Filters */}
      <div className="dm-filters">
        <div className="search-input-wrap" style={{ flex: 1 }}>
          <span className="search-icon">🔍</span>
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by name, specialization or email..."
          />
        </div>
        <select
          value={deptFilter}
          onChange={e => setDeptFilter(e.target.value)}
          className="dm-dept-select"
        >
          <option value="">All Departments</option>
          {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
        </select>
        <div className="dm-count-badge">{filtered.length} result{filtered.length !== 1 ? 's' : ''}</div>
      </div>

      {/* Doctors Table */}
      {loading ? <LoadingSpinner /> : filtered.length === 0 ? (
        <EmptyState icon="👨‍⚕️" title="No doctors found" subtitle="Try adjusting your search or add a new doctor." />
      ) : (
        <div className="table-wrap">
          {/* Desktop Table */}
          <table className="doctors-table-desktop">
            <thead>
              <tr>
                <th>Doctor</th>
                <th>Specialization</th>
                <th>Department</th>
                <th>Experience</th>
                <th>Fee</th>
                <th>Contact</th>
                <th>Languages</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(d => (
                <tr key={d.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div className="dm-avatar">{d.firstName?.[0]}{d.lastName?.[0]}</div>
                      <div>
                        <div style={{ fontWeight: 700 }}>Dr. {d.firstName} {d.lastName}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{d.email || '—'}</div>
                      </div>
                    </div>
                  </td>
                  <td>{d.specialization}</td>
                  <td>
                    <span className="dm-dept-tag">{d.department?.name || '—'}</span>
                  </td>
                  <td>{d.experienceYears} yrs</td>
                  <td style={{ fontWeight: 700, color: 'var(--primary)' }}>₹{d.consultationFee?.toFixed(0)}</td>
                  <td style={{ fontSize: 13 }}>{d.phone || '—'}</td>
                  <td>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                      {d.languagesSpoken?.slice(0, 2).map(l => (
                        <span key={l} className="dm-lang-badge">{l}</span>
                      ))}
                      {d.languagesSpoken?.length > 2 && (
                        <span className="dm-lang-badge">+{d.languagesSpoken.length - 2}</span>
                      )}
                    </div>
                  </td>
                  <td>
                    <span className={`badge ${d.isAvailable ? 'badge-confirmed' : 'badge-cancelled'}`}>
                      {d.isAvailable ? 'Available' : 'Unavailable'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button
                        className="btn btn-secondary btn-sm"
                        title="Edit Doctor"
                        onClick={() => { setEditDoctor(d); setShowAddEdit(true); }}
                      >✏️ Edit</button>
                      <button
                        className="btn btn-secondary btn-sm"
                        title="Manage Schedule"
                        onClick={() => setScheduleDoctor(d)}
                      >📅 Schedule</button>
                      <button
                        className="btn btn-danger btn-sm"
                        title="Delete Doctor"
                        onClick={() => setDeleteDoctor(d)}
                      >🗑</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Mobile Cards */}
          <div className="doctors-mobile">
            {filtered.map(d => (
              <div className="doctor-list-card" key={d.id}>
                <div className="doctor-list-card-header">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div className="dm-avatar">{d.firstName?.[0]}{d.lastName?.[0]}</div>
                    <div>
                      <div style={{ fontWeight: 700 }}>Dr. {d.firstName} {d.lastName}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{d.email || '—'}</div>
                    </div>
                  </div>
                  <span className={`badge ${d.isAvailable ? 'badge-confirmed' : 'badge-cancelled'}`}>
                    {d.isAvailable ? 'Available' : 'Unavailable'}
                  </span>
                </div>
                <div className="doctor-list-card-body">
                  <div className="doctor-list-card-row">
                    <span className="doctor-list-card-label">Specialization</span>
                    <span className="doctor-list-card-value">{d.specialization}</span>
                  </div>
                  <div className="doctor-list-card-row">
                    <span className="doctor-list-card-label">Department</span>
                    <span className="doctor-list-card-value">{d.department?.name || '—'}</span>
                  </div>
                  <div className="doctor-list-card-row">
                    <span className="doctor-list-card-label">Experience</span>
                    <span className="doctor-list-card-value">{d.experienceYears} yrs</span>
                  </div>
                  <div className="doctor-list-card-row">
                    <span className="doctor-list-card-label">Fee</span>
                    <span className="doctor-list-card-value" style={{ fontWeight: 700, color: 'var(--primary)' }}>₹{d.consultationFee?.toFixed(0)}</span>
                  </div>
                  <div className="doctor-list-card-row">
                    <span className="doctor-list-card-label">Phone</span>
                    <span className="doctor-list-card-value">{d.phone || '—'}</span>
                  </div>
                  <div className="doctor-list-card-row">
                    <span className="doctor-list-card-label">Languages</span>
                    <span className="doctor-list-card-value">
                      {d.languagesSpoken?.length > 0 ? d.languagesSpoken.join(', ') : '—'}
                    </span>
                  </div>
                </div>
                <div className="doctor-list-card-actions">
                  <button
                    className="btn btn-secondary btn-sm"
                    title="Edit Doctor"
                    onClick={() => { setEditDoctor(d); setShowAddEdit(true); }}
                  >✏️ Edit</button>
                  <button
                    className="btn btn-secondary btn-sm"
                    title="Manage Schedule"
                    onClick={() => setScheduleDoctor(d)}
                  >📅 Schedule</button>
                  <button
                    className="btn btn-danger btn-sm"
                    title="Delete Doctor"
                    onClick={() => setDeleteDoctor(d)}
                  >🗑</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Summary Cards */}
      <div className="dm-summary-grid">
        <div className="dm-summary-card">
          <div className="dm-summary-value">{doctors.filter(d => d.isAvailable).length}</div>
          <div className="dm-summary-label">Available</div>
        </div>
        <div className="dm-summary-card">
          <div className="dm-summary-value">{doctors.filter(d => !d.isAvailable).length}</div>
          <div className="dm-summary-label">Unavailable</div>
        </div>
        <div className="dm-summary-card">
          <div className="dm-summary-value">{departments.length}</div>
          <div className="dm-summary-label">Departments</div>
        </div>
        <div className="dm-summary-card">
          <div className="dm-summary-value">
            {doctors.length > 0
              ? `₹${Math.round(doctors.reduce((s, d) => s + (d.consultationFee || 0), 0) / doctors.length)}`
              : '—'}
          </div>
          <div className="dm-summary-label">Avg. Fee</div>
        </div>
      </div>

      {/* Modals */}
      {showAddEdit && (
        <DoctorFormModal
          doctor={editDoctor}
          departments={departments}
          onSave={handleSave}
          onClose={() => { setShowAddEdit(false); setEditDoctor(null); }}
        />
      )}
      {scheduleDoctor && (
        <ScheduleModal
          doctor={scheduleDoctor}
          onClose={() => setScheduleDoctor(null)}
        />
      )}
      {deleteDoctor && (
        <DeleteModal
          doctor={deleteDoctor}
          onConfirm={handleDelete}
          onClose={() => setDeleteDoctor(null)}
          deleting={deleting}
        />
      )}
    </div>
  );
};

export default DoctorManagement;
