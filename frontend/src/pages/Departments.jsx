import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { EmptyState, LoadingSpinner, Modal } from '../components/Common';
import { useAuth } from '../context/AuthContext';

const DEPT_ICONS = ['🏥', '🩺', '🫀', '🧠', '🫁', '👶', '🦴', '👁️', '🦷', '🔬'];

const DepartmentCard = ({ department, icon, onEdit, onDelete }) => (
  <div className="card" style={{ border: '1px solid var(--border)' }}>
    <div style={{ fontSize: 34, marginBottom: 10 }}>{icon}</div>
    <div className="card-title">{department.name}</div>
    <p style={{ color: 'var(--text-muted)', fontSize: 13, marginBottom: 14 }}>
      {department.description || 'No description available.'}
    </p>

    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
      <div style={{ background: 'var(--bg)', padding: '8px 10px', borderRadius: 8 }}>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.8 }}>
          Floor
        </div>
        <div style={{ fontWeight: 700 }}>{department.floorNumber ?? 'N/A'}</div>
      </div>
      <div style={{ background: 'var(--bg)', padding: '8px 10px', borderRadius: 8 }}>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.8 }}>
          Doctors
        </div>
        <div style={{ fontWeight: 700 }}>{department.doctorCount ?? 0}</div>
      </div>
    </div>
    <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
      <button className="btn btn-secondary btn-sm" onClick={() => onEdit(department)}>Edit</button>
      <button className="btn btn-danger btn-sm" onClick={() => onDelete(department)}>Delete</button>
    </div>
  </div>
);

const Departments = () => {
  const { user } = useAuth();
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [selectedDepartment, setSelectedDepartment] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    name: '',
    description: '',
    floorNumber: '',
    phone: '',
    email: '',
  });

  useEffect(() => {
    api.get('/departments/hospital')
      .then((data) => setDepartments(data || []))
      .catch(() => setDepartments([]))
      .finally(() => setLoading(false));
  }, []);

  const setField = (key) => (e) => {
    setForm((prev) => ({ ...prev, [key]: e.target.value }));
  };

  const resetForm = () => {
    setForm({ name: '', description: '', floorNumber: '', phone: '', email: '' });
    setError('');
  };

  const openEditModal = (department) => {
    setSelectedDepartment(department);
    setForm({
      name: department.name || '',
      description: department.description || '',
      floorNumber: department.floorNumber ?? '',
      phone: department.phone || '',
      email: department.email || '',
    });
    setError('');
    setShowEditModal(true);
  };

  const openDeleteModal = (department) => {
    setSelectedDepartment(department);
    setError('');
    setShowDeleteModal(true);
  };

  const handleCreateDepartment = async (e) => {
    e.preventDefault();
    setError('');
    if (!form.name.trim()) {
      setError('Department name is required.');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || null,
        floorNumber: form.floorNumber === '' ? null : Number(form.floorNumber),
        phone: form.phone.trim() || null,
        email: form.email.trim() || null,
      };
      const created = await api.post('/departments/hospital', payload);
      setDepartments((prev) => [created, ...prev]);
      setShowAddModal(false);
      resetForm();
    } catch (err) {
      setError(err?.message || 'Failed to create department.');
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateDepartment = async (e) => {
    e.preventDefault();
    if (!selectedDepartment?.id) return;
    setError('');
    if (!form.name.trim()) {
      setError('Department name is required.');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || null,
        floorNumber: form.floorNumber === '' ? null : Number(form.floorNumber),
        phone: form.phone.trim() || null,
        email: form.email.trim() || null,
      };
      const updated = await api.put(`/departments/hospital/${selectedDepartment.id}`, payload);
      setDepartments((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
      setShowEditModal(false);
      setSelectedDepartment(null);
      resetForm();
    } catch (err) {
      setError(err?.message || 'Failed to update department.');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteDepartment = async () => {
    if (!selectedDepartment?.id) return;
    setSaving(true);
    setError('');
    try {
      await api.delete(`/departments/hospital/${selectedDepartment.id}`);
      setDepartments((prev) => prev.filter((d) => d.id !== selectedDepartment.id));
      setShowDeleteModal(false);
      setSelectedDepartment(null);
    } catch (err) {
      setError(err?.message || 'Failed to delete department.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Departments</h1>
          <p className="page-subtitle">
            {user?.hospital?.name ? `${user.hospital.name} has` : 'Your hospital has'} {departments.length} active department{departments.length === 1 ? '' : 's'}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => { resetForm(); setShowAddModal(true); }}>
          + Add Department
        </button>
      </div>

      {departments.length === 0 ? (
        <EmptyState
          icon="🏥"
          title="No departments found"
          subtitle="No active departments are configured for this hospital yet."
        />
      ) : (
        <div className="grid-3">
          {departments.map((d, i) => (
            <DepartmentCard
              key={d.id}
              department={d}
              icon={DEPT_ICONS[i % DEPT_ICONS.length]}
              onEdit={openEditModal}
              onDelete={openDeleteModal}
            />
          ))}
        </div>
      )}

      {showAddModal && (
        <Modal title="Add Department" onClose={() => { setShowAddModal(false); resetForm(); }}>
          <form onSubmit={handleCreateDepartment}>
            <div className="modal-body">
              <div className="form-group">
                <label>Department Name *</label>
                <input value={form.name} onChange={setField('name')} placeholder="e.g. Cardiology" />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea rows={3} value={form.description} onChange={setField('description')} placeholder="Short department description" />
              </div>
              <div className="form-grid-2">
                <div className="form-group">
                  <label>Floor Number</label>
                  <input type="number" min="0" value={form.floorNumber} onChange={setField('floorNumber')} placeholder="e.g. 2" />
                </div>
                <div className="form-group">
                  <label>Phone</label>
                  <input value={form.phone} onChange={setField('phone')} placeholder="+91 22 1234 5678" />
                </div>
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={form.email} onChange={setField('email')} placeholder="department@hospital.com" />
              </div>
              {error && <div style={{ color: '#d64545', fontSize: 13 }}>{error}</div>}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={() => { setShowAddModal(false); resetForm(); }}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Creating...' : 'Create Department'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {showEditModal && (
        <Modal title="Edit Department" onClose={() => { setShowEditModal(false); setSelectedDepartment(null); resetForm(); }}>
          <form onSubmit={handleUpdateDepartment}>
            <div className="modal-body">
              <div className="form-group">
                <label>Department Name *</label>
                <input value={form.name} onChange={setField('name')} />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea rows={3} value={form.description} onChange={setField('description')} />
              </div>
              <div className="form-grid-2">
                <div className="form-group">
                  <label>Floor Number</label>
                  <input type="number" min="0" value={form.floorNumber} onChange={setField('floorNumber')} />
                </div>
                <div className="form-group">
                  <label>Phone</label>
                  <input value={form.phone} onChange={setField('phone')} />
                </div>
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={form.email} onChange={setField('email')} />
              </div>
              {error && <div style={{ color: '#d64545', fontSize: 13 }}>{error}</div>}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={() => { setShowEditModal(false); setSelectedDepartment(null); resetForm(); }}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {showDeleteModal && (
        <Modal title="Delete Department" onClose={() => { setShowDeleteModal(false); setSelectedDepartment(null); setError(''); }}>
          <div className="modal-body">
            <p>Delete <strong>{selectedDepartment?.name}</strong>? This action cannot be undone.</p>
            {error && <div style={{ color: '#d64545', fontSize: 13 }}>{error}</div>}
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={() => { setShowDeleteModal(false); setSelectedDepartment(null); setError(''); }}>
              Cancel
            </button>
            <button type="button" className="btn btn-danger" disabled={saving} onClick={handleDeleteDepartment}>
              {saving ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default Departments;
