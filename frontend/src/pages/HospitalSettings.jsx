import React, { useEffect, useState } from 'react';
import { Icon, LoadingSpinner } from '../components/Common';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const emptyProfile = {
  name: '',
  address: '',
  city: '',
  state: '',
  pincode: '',
  phone: '',
  email: '',
  website: '',
  logoUrl: '',
  licenseNumber: '',
  description: '',
};

const HospitalSettings = () => {
  const { token, user, saveAuth } = useAuth();
  const [form, setForm] = useState(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      setError('');
      const profile = await api.get('/hospital/me');
      setForm({ ...emptyProfile, ...profile });
    } catch (err) {
      setError(err.message || 'Failed to load hospital profile.');
    } finally {
      setLoading(false);
    }
  };

  const setField = (field) => (event) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const saveProfile = async (event) => {
    event.preventDefault();
    try {
      setSaving(true);
      setError('');
      const response = await api.put('/hospital/me', form);
      const updated = response.data;
      setForm({ ...emptyProfile, ...updated });
      if (token && user) {
        saveAuth(token, { ...user, hospital: { ...user.hospital, ...updated } });
      }
      setToast('Hospital profile updated.');
      setTimeout(() => setToast(''), 3000);
    } catch (err) {
      setError(err.message || 'Failed to update hospital profile.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      {toast && <div className="dm-toast">{toast}</div>}

      <div className="page-header">
        <div>
          <h1 className="page-title">Hospital Settings</h1>
          <p className="page-subtitle">Manage the contact details used on consultation receipt PDFs</p>
        </div>
      </div>

      {error && (
        <div className="alert alert-error" style={{ marginBottom: 16 }}>
          {error}
        </div>
      )}

      <form onSubmit={saveProfile}>
        <div className="table-wrap" style={{ padding: 20 }}>
          <div className="form-row">
            <div className="form-group">
              <label>Hospital Name *</label>
              <input value={form.name} onChange={setField('name')} required />
            </div>
            <div className="form-group">
              <label>Phone</label>
              <input value={form.phone || ''} onChange={setField('phone')} placeholder="+91 98765 43210" />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={form.email || ''} onChange={setField('email')} placeholder="clinic@example.com" />
            </div>
            <div className="form-group">
              <label>Website</label>
              <input value={form.website || ''} onChange={setField('website')} placeholder="https://example.com" />
            </div>
          </div>

          <div className="form-group">
            <label>Address</label>
            <textarea
              value={form.address || ''}
              onChange={setField('address')}
              rows={3}
              placeholder="Street, landmark, building"
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>City</label>
              <input value={form.city || ''} onChange={setField('city')} />
            </div>
            <div className="form-group">
              <label>State</label>
              <input value={form.state || ''} onChange={setField('state')} />
            </div>
            <div className="form-group">
              <label>Pincode</label>
              <input value={form.pincode || ''} onChange={setField('pincode')} />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Logo URL</label>
              <input value={form.logoUrl || ''} onChange={setField('logoUrl')} placeholder="https://..." />
            </div>
            <div className="form-group">
              <label>License Number</label>
              <input value={form.licenseNumber || ''} onChange={setField('licenseNumber')} />
            </div>
          </div>

          <div className="form-group">
            <label>Description</label>
            <textarea value={form.description || ''} onChange={setField('description')} rows={3} />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 18 }}>
            <button type="button" className="btn btn-secondary" onClick={loadProfile} disabled={saving}>
              Reset
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <Icon name="plus" /> {saving ? 'Saving...' : 'Save Settings'}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default HospitalSettings;
