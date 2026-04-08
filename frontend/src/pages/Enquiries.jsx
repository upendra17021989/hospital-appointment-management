import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { LoadingSpinner, Badge, EmptyState, Modal, Tabs } from '../components/Common';

const ENQUIRY_TYPES = ['general', 'appointment', 'billing', 'medical', 'complaint', 'feedback'];
const PRIORITIES = ['low', 'normal', 'high', 'urgent'];

const EnquiryForm = ({ departments, onSubmit, onClose, submitting }) => {
  const [form, setForm] = useState({
    name: '', email: '', phone: '', subject: '', message: '',
    enquiryType: 'general', priority: 'normal', departmentId: '',
  });

  const isValid = form.name && form.phone && form.subject && form.message;
  const set = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.value }));

  return (
    <Modal title="Submit an Enquiry" onClose={onClose}>
      <div className="form-grid">
        <div className="form-group">
          <label>Your Name *</label>
          <input value={form.name} onChange={set('name')} placeholder="Full name" />
        </div>
        <div className="form-group">
          <label>Phone *</label>
          <input value={form.phone} onChange={set('phone')} placeholder="+91 98765 43210" />
        </div>
        <div className="form-group">
          <label>Email</label>
          <input type="email" value={form.email} onChange={set('email')} placeholder="your@email.com" />
        </div>
        <div className="form-group">
          <label>Enquiry Type</label>
          <select value={form.enquiryType} onChange={set('enquiryType')}>
            {ENQUIRY_TYPES.map(t => (
              <option key={t} value={t}>{t.charAt(0).toUpperCase() + t.slice(1)}</option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Department</label>
          <select value={form.departmentId} onChange={set('departmentId')}>
            <option value="">Select department (optional)</option>
            {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </div>
        <div className="form-group">
          <label>Priority</label>
          <select value={form.priority} onChange={set('priority')}>
            {PRIORITIES.map(p => (
              <option key={p} value={p}>{p.charAt(0).toUpperCase() + p.slice(1)}</option>
            ))}
          </select>
        </div>
        <div className="form-group full">
          <label>Subject *</label>
          <input value={form.subject} onChange={set('subject')} placeholder="Brief description of your enquiry" />
        </div>
        <div className="form-group full">
          <label>Message *</label>
          <textarea value={form.message} onChange={set('message')}
            placeholder="Please provide details..." style={{ minHeight: 100 }} />
        </div>
      </div>
      <div style={{ display: 'flex', gap: 10, marginTop: 20, justifyContent: 'flex-end' }}>
        <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
        <button
          className="btn btn-primary"
          disabled={submitting || !isValid}
          onClick={() => onSubmit(form)}
        >
          {submitting ? 'Submitting...' : 'Submit Enquiry'}
        </button>
      </div>
    </Modal>
  );
};

const Enquiries = () => {
  const [enquiries, setEnquiries] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [statusFilter, setStatusFilter] = useState('all');
  const [submitting, setSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [updating, setUpdating] = useState(null);

  const fetchEnquiries = () => {
    setLoading(true);
    const url = statusFilter !== 'all'
      ? `/enquiries/hospital?status=${statusFilter}`
      : '/enquiries/hospital/all';
    api.get(url)
      .then(data => { setEnquiries(data || []); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(() => { fetchEnquiries(); }, [statusFilter]);
  useEffect(() => { api.get('/departments/hospital').then(setDepartments).catch(() => {}); }, []);

  const handleSubmit = async (form) => {
    setSubmitting(true);
    try {
      await api.post('/enquiries/hospital', {
        ...form,
        departmentId: form.departmentId || undefined,
      });
      setShowForm(false);
      setSuccessMsg('Enquiry submitted! Our team will respond within 24 hours.');
      fetchEnquiries();
      setTimeout(() => setSuccessMsg(''), 5000);
    } catch (e) {
      alert(e.message);
    } finally {
      setSubmitting(false);
    }
  };

  const updateStatus = async (id, status) => {
    setUpdating(id);
    try {
      await api.patch(`/enquiries/hospital/${id}/status?status=${status}`, {});
      fetchEnquiries();
    } finally {
      setUpdating(null);
    }
  };

  const priorityColors = { low: 'var(--accent)', normal: 'var(--text-muted)', high: 'orange', urgent: 'crimson' };

  const tabs = [
    { value: 'all', label: 'All' },
    { value: 'open', label: 'Open' },
    { value: 'in_progress', label: 'In Progress' },
    { value: 'resolved', label: 'Resolved' },
    { value: 'closed', label: 'Closed' },
  ];

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Enquiries</h1>
          <p className="page-subtitle">Manage and respond to hospital enquiries</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm(true)}>
          + New Enquiry
        </button>
      </div>

      {successMsg && (
        <div className="alert alert-success">
          ✓ {successMsg}
          <button onClick={() => setSuccessMsg('')} style={{ float: 'right', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 700 }}>×</button>
        </div>
      )}

      <Tabs tabs={tabs} active={statusFilter} onChange={setStatusFilter} />

      <div className="table-wrap">
        {loading ? <LoadingSpinner /> : enquiries.length === 0 ? (
          <EmptyState icon="💬" title="No enquiries found" />
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name & Contact</th>
                <th>Subject</th>
                <th>Type</th>
                <th>Department</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Date</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {enquiries.map(e => (
                <tr key={e.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{e.name}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{e.phone}</div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 500, maxWidth: 220 }}>{e.subject}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                      {e.message?.slice(0, 60)}...
                    </div>
                  </td>
                  <td><Badge status={e.enquiryType} /></td>
                  <td>{e.department?.name || '—'}</td>
                  <td>
                    <span style={{ fontWeight: 700, fontSize: 12, color: priorityColors[e.priority] }}>
                      {e.priority?.toUpperCase()}
                    </span>
                  </td>
                  <td><Badge status={e.status} /></td>
                  <td style={{ fontSize: 13, color: 'var(--text-muted)' }}>
                    {e.createdAt ? new Date(e.createdAt).toLocaleDateString('en-IN') : '—'}
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 6 }}>
                      {e.status === 'open' && (
                        <button className="btn btn-secondary btn-sm" disabled={updating === e.id}
                          onClick={() => updateStatus(e.id, 'in_progress')}>Handle</button>
                      )}
                      {e.status === 'in_progress' && (
                        <button className="btn btn-accent btn-sm" disabled={updating === e.id}
                          onClick={() => updateStatus(e.id, 'resolved')}>Resolve</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showForm && (
        <EnquiryForm
          departments={departments}
          onSubmit={handleSubmit}
          onClose={() => setShowForm(false)}
          submitting={submitting}
        />
      )}
    </div>
  );
};

export default Enquiries;
