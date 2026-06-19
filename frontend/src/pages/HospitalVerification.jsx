import React, { useEffect, useMemo, useState } from 'react';
import { Badge, EmptyState, LoadingSpinner, Modal, PageHeader, Tabs } from '../components/Common';
import { hospitalVerificationApi, saveBlob } from '../services/hospitalVerificationApi';

const statusTabs = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: '', label: 'All' },
];

const documentLabels = {
  HOSPITAL_REGISTRATION_CERTIFICATE: 'Hospital Registration Certificate',
  CLINICAL_ESTABLISHMENT_REGISTRATION: 'Clinical Establishment Registration',
  MUNICIPAL_LICENSE: 'Local Municipal License',
  PHARMACY_LICENSE: 'Pharmacy License',
  LABORATORY_LICENSE: 'Laboratory License',
  GST_CERTIFICATE: 'GST Certificate',
  PAN_CARD: 'PAN Card',
  OWNER_ID_PROOF: 'Owner ID Proof',
};

const formatDateTime = (value) => value
  ? new Date(value).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  : '-';

const HospitalVerification = () => {
  const [status, setStatus] = useState('PENDING');
  const [hospitals, setHospitals] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [selected, setSelected] = useState(null);
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);
  const [query, setQuery] = useState('');

  const loadHospitals = async (nextStatus = status) => {
    setLoading(true);
    setMessage('');
    try {
      const data = await hospitalVerificationApi.list(nextStatus || undefined);
      setHospitals(data || []);
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHospitals(status);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return hospitals;
    return hospitals.filter(h => [
      h.name,
      h.city,
      h.phone,
      h.email,
      h.registrationNumber,
      h.clinicalEstablishmentRegistrationNumber,
      h.municipalLicenseNumber,
      h.pharmacyLicenseNumber,
      h.laboratoryLicenseNumber,
      h.gstNumber,
      h.panNumber,
      h.ownerDirectorName,
    ].some(v => String(v || '').toLowerCase().includes(q)));
  }, [hospitals, query]);

  const openReview = async (hospital) => {
    setMessage('');
    try {
      const full = await hospitalVerificationApi.get(hospital.id);
      setSelected(full);
      setNotes(full?.verificationNotes || '');
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    }
  };

  const review = async (nextStatus) => {
    if (!selected) return;
    if (nextStatus === 'REJECTED' && !notes.trim()) {
      setMessage('Error: Rejection notes are required.');
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      const res = await hospitalVerificationApi.review(selected.id, { status: nextStatus, notes });
      setSelected(res?.data || null);
      setMessage(`Hospital marked ${nextStatus.toLowerCase()}.`);
      await loadHospitals(status);
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setSaving(false);
    }
  };

  const downloadDoc = async (doc) => {
    if (!selected) return;
    try {
      const blob = await hospitalVerificationApi.downloadDocument(selected.id, doc.id);
      saveBlob(blob, doc.originalFilename);
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    }
  };

  return (
    <div>
      <PageHeader
        title="Hospital Verification"
        subtitle="Review hospital business details and approve uploaded KYC documents"
      />

      {message && <div className={`alert ${message.startsWith('Error') ? 'alert-error' : 'alert-success'}`}>{message}</div>}

      <Tabs active={status} onChange={setStatus} tabs={statusTabs} />

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="form-grid">
          <div className="form-group">
            <label>Search</label>
            <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Name, city, registration, license, GST, PAN, owner..." />
          </div>
          <div className="form-group">
            <label>&nbsp;</label>
            <button className="btn btn-secondary" onClick={() => loadHospitals(status)}>Refresh</button>
          </div>
        </div>
      </div>

      <div className="table-wrap table-wrap--scrollable">
        {loading ? <LoadingSpinner /> : filtered.length === 0 ? (
          <EmptyState icon="HV" title="No hospitals found" subtitle="Try another status or search term" />
        ) : (
          <table>
            <thead>
              <tr>
                <th>Hospital</th>
                <th>Owner</th>
                <th>Registration</th>
                <th>Clinical Reg.</th>
                <th>Municipal</th>
                <th>GST</th>
                <th>PAN</th>
                <th>Documents</th>
                <th>Status</th>
                <th>Registered</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(h => (
                <tr key={h.id}>
                  <td>
                    <strong>{h.name}</strong>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{[h.city, h.state].filter(Boolean).join(', ') || h.email}</div>
                  </td>
                  <td>{h.ownerDirectorName || '-'}</td>
                  <td>{h.registrationNumber || h.licenseNumber || '-'}</td>
                  <td>{h.clinicalEstablishmentRegistrationNumber || '-'}</td>
                  <td>{h.municipalLicenseNumber || '-'}</td>
                  <td>{h.gstNumber || '-'}</td>
                  <td>{h.panNumber || '-'}</td>
                  <td>{h.documents?.length || 0}/8</td>
                  <td><Badge status={(h.verificationStatus || 'PENDING').toLowerCase()} /></td>
                  <td>{formatDateTime(h.createdAt)}</td>
                  <td>
                    <button className="btn btn-primary btn-sm" onClick={() => openReview(h)}>Review</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {selected && (
        <Modal title={`Review ${selected.name}`} onClose={() => setSelected(null)}>
          <div style={{ display: 'grid', gap: 14 }}>
            <div className="grid-3">
              <Info label="Registration" value={selected.registrationNumber || selected.licenseNumber} />
              <Info label="Clinical Establishment" value={selected.clinicalEstablishmentRegistrationNumber} />
              <Info label="Municipal License" value={selected.municipalLicenseNumber} />
              <Info label="GST" value={selected.gstNumber || 'Not applicable'} />
              <Info label="PAN" value={selected.panNumber} />
            </div>
            <div className="grid-3">
              <Info label="Pharmacy License" value={selected.pharmacyLicenseNumber || 'Not applicable'} />
              <Info label="Laboratory License" value={selected.laboratoryLicenseNumber || 'Not applicable'} />
              <Info label="Owner/Director" value={selected.ownerDirectorName} />
              <Info label="Contact" value={selected.phone} />
              <Info label="Email" value={selected.email} />
            </div>
            <Info label="Address" value={[selected.address, selected.city, selected.state, selected.pincode].filter(Boolean).join(', ')} />

            <div>
              <div className="card-title">Documents</div>
              <table>
                <tbody>
                  {(selected.documents || []).map(doc => (
                    <tr key={doc.id}>
                      <td>{documentLabels[doc.documentType] || doc.documentType}</td>
                      <td>{doc.originalFilename}</td>
                      <td>{formatDateTime(doc.uploadedAt)}</td>
                      <td><button className="btn btn-secondary btn-sm" onClick={() => downloadDoc(doc)}>Download</button></td>
                    </tr>
                  ))}
                  {(!selected.documents || selected.documents.length === 0) && (
                    <tr><td colSpan="4">No documents uploaded.</td></tr>
                  )}
                </tbody>
              </table>
            </div>

            <div className="form-group">
              <label>Verification Notes</label>
              <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={3} placeholder="Approval note or rejection reason" />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, flexWrap: 'wrap' }}>
              <button className="btn btn-secondary" disabled={saving} onClick={() => review('PENDING')}>Mark Pending</button>
              <button className="btn btn-danger" disabled={saving} onClick={() => review('REJECTED')}>Reject</button>
              <button className="btn btn-primary" disabled={saving} onClick={() => review('VERIFIED')}>Approve</button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

const Info = ({ label, value }) => (
  <div>
    <div style={{ fontSize: 12, color: 'var(--text-muted)', fontWeight: 700 }}>{label}</div>
    <div style={{ fontWeight: 700 }}>{value || '-'}</div>
  </div>
);

export default HospitalVerification;
