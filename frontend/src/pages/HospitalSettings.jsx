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
  registrationNumber: '',
  clinicalEstablishmentRegistrationNumber: '',
  municipalLicenseNumber: '',
  pharmacyLicenseNumber: '',
  laboratoryLicenseNumber: '',
  gstNumber: '',
  panNumber: '',
  ownerDirectorName: '',
  verificationStatus: 'PENDING',
  verificationNotes: '',
  description: '',
};

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const documentTypes = [
  ['HOSPITAL_REGISTRATION_CERTIFICATE', 'Hospital Registration Certificate'],
  ['CLINICAL_ESTABLISHMENT_REGISTRATION', 'Clinical Establishment Registration'],
  ['MUNICIPAL_LICENSE', 'Local Municipal License'],
  ['PHARMACY_LICENSE', 'Pharmacy License'],
  ['LABORATORY_LICENSE', 'Laboratory License'],
  ['GST_CERTIFICATE', 'GST Certificate'],
  ['PAN_CARD', 'PAN Card'],
  ['OWNER_ID_PROOF', 'Owner ID Proof'],
];

const HospitalSettings = () => {
  const { token, user, saveAuth } = useAuth();
  const [form, setForm] = useState(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState('');
  const [error, setError] = useState('');
  const [documents, setDocuments] = useState([]);
  const [uploading, setUploading] = useState('');

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      setError('');
      const profile = await api.get('/hospital/me');
      setForm({ ...emptyProfile, ...profile });
      await loadDocuments();
    } catch (err) {
      setError(err.message || 'Failed to load hospital profile.');
    } finally {
      setLoading(false);
    }
  };

  const setField = (field) => (event) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const loadDocuments = async () => {
    try {
      const docs = await api.get('/hospital/documents');
      setDocuments(docs || []);
    } catch {
      setDocuments([]);
    }
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

  const uploadDocument = async (documentType, file) => {
    if (!file) return;
    try {
      setUploading(documentType);
      setError('');
      const payload = new FormData();
      payload.append('documentType', documentType);
      payload.append('file', file);
      const res = await fetch(`${API_BASE}/hospital/documents`, {
        method: 'POST',
        headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: payload,
      });
      const body = await res.json();
      if (!body.success) throw new Error(body.message || 'Upload failed');
      setToast('Document uploaded.');
      setTimeout(() => setToast(''), 3000);
      await loadDocuments();
    } catch (err) {
      setError(err.message || 'Failed to upload document.');
    } finally {
      setUploading('');
    }
  };

  const downloadDocument = async (doc) => {
    try {
      const res = await fetch(`${API_BASE}/hospital/documents/${doc.id}/download`, {
        headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      });
      if (!res.ok) throw new Error('Download failed');
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = doc.originalFilename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message || 'Failed to download document.');
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      {toast && <div className="dm-toast">{toast}</div>}

      <div className="page-header">
        <div>
          <h1 className="page-title">Hospital Settings</h1>
          <p className="page-subtitle">Manage profile, business verification, and receipt header details</p>
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
              <label>Contact Number</label>
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

          <div className="form-row">
            <div className="form-group">
              <label>Registration Number *</label>
              <input value={form.registrationNumber || ''} onChange={setField('registrationNumber')} />
            </div>
            <div className="form-group">
              <label>GST Number</label>
              <input value={form.gstNumber || ''} onChange={setField('gstNumber')} placeholder="If applicable" />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Clinical Establishment Registration</label>
              <input value={form.clinicalEstablishmentRegistrationNumber || ''} onChange={setField('clinicalEstablishmentRegistrationNumber')} />
            </div>
            <div className="form-group">
              <label>Local Municipal License</label>
              <input value={form.municipalLicenseNumber || ''} onChange={setField('municipalLicenseNumber')} />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Pharmacy License</label>
              <input value={form.pharmacyLicenseNumber || ''} onChange={setField('pharmacyLicenseNumber')} placeholder="If pharmacy module is used" />
            </div>
            <div className="form-group">
              <label>Laboratory License</label>
              <input value={form.laboratoryLicenseNumber || ''} onChange={setField('laboratoryLicenseNumber')} placeholder="If lab module is used" />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>PAN Number *</label>
              <input value={form.panNumber || ''} onChange={setField('panNumber')} />
            </div>
            <div className="form-group">
              <label>Owner/Director Name *</label>
              <input value={form.ownerDirectorName || ''} onChange={setField('ownerDirectorName')} />
            </div>
          </div>

          <div className="alert alert-info" style={{ marginTop: 12 }}>
            Verification status: <strong>{form.verificationStatus || 'PENDING'}</strong>
            {form.verificationNotes ? ` - ${form.verificationNotes}` : ''}
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

      <div className="table-wrap" style={{ padding: 20, marginTop: 20 }}>
        <div className="card-title">Verification Documents</div>
        <div className="form-grid">
          {documentTypes.map(([type, label]) => {
            const latest = documents.find((doc) => doc.documentType === type);
            return (
              <div className="form-group" key={type}>
                <label>{label}</label>
                <input
                  type="file"
                  accept="application/pdf,image/png,image/jpeg,image/webp"
                  disabled={uploading === type}
                  onChange={(event) => uploadDocument(type, event.target.files?.[0])}
                />
                {latest ? (
                  <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10 }}>
                    <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{latest.originalFilename}</span>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => downloadDocument(latest)}>
                      Download
                    </button>
                  </div>
                ) : (
                  <div style={{ marginTop: 6, fontSize: 12, color: 'var(--text-muted)' }}>
                    No file uploaded
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default HospitalSettings;
