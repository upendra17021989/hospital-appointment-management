import React, { useEffect, useMemo, useState } from 'react';
import { Badge, EmptyState, LoadingSpinner, PageHeader, Tabs } from '../components/Common';
import { legalApi, saveBlob } from '../services/legalApi';

const labels = {
  SERVICE_AGREEMENT: 'Service Agreement',
  PRIVACY_POLICY: 'Privacy Policy',
  DATA_PROCESSING_AGREEMENT: 'Data Processing Agreement',
  SUPPORT_TERMS: 'Support Terms',
  CANCELLATION_POLICY: 'Cancellation Policy',
};

const LegalAgreements = () => {
  const [activeTab, setActiveTab] = useState('documents');
  const [documents, setDocuments] = useState([]);
  const [acceptances, setAcceptances] = useState([]);
  const [signed, setSigned] = useState([]);
  const [loading, setLoading] = useState(true);
  const [acceptChecked, setAcceptChecked] = useState(false);
  const [message, setMessage] = useState('');
  const [uploading, setUploading] = useState(false);

  const allAccepted = useMemo(() => documents.length > 0 && documents.every(d => d.accepted), [documents]);

  const load = async () => {
    setLoading(true);
    try {
      const [docs, history, agreements] = await Promise.all([
        legalApi.activeDocuments().catch(() => []),
        legalApi.acceptances().catch(() => []),
        legalApi.signedAgreements().catch(() => []),
      ]);
      setDocuments(docs || []);
      setAcceptances(history || []);
      setSigned(agreements || []);
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const acceptAll = async () => {
    if (!acceptChecked) {
      setMessage('Error: Please confirm acceptance before continuing.');
      return;
    }
    try {
      await legalApi.acceptAll('I have read and accept the active legal documents, including Service Agreement, Privacy Policy, and Data Processing Agreement.');
      setMessage('Legal documents accepted.');
      await load();
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    }
  };

  const upload = async (file) => {
    if (!file) return;
    setUploading(true);
    try {
      await legalApi.uploadSignedAgreement(file);
      setMessage('Signed service agreement uploaded for review.');
      await load();
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setUploading(false);
    }
  };

  const download = async (item) => {
    const blob = await legalApi.downloadSignedAgreement(item.id);
    saveBlob(blob, item.originalFilename);
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader title="Legal & Agreements" subtitle="Accept legal terms and upload your signed service agreement" />
      {message && <div className={`alert ${message.startsWith('Error') ? 'alert-error' : 'alert-success'}`}>{message}</div>}
      <Tabs active={activeTab} onChange={setActiveTab} tabs={[
        { value: 'documents', label: 'Legal Documents' },
        { value: 'signed', label: 'Signed Agreement' },
        { value: 'history', label: 'Acceptance History' },
      ]} />

      {activeTab === 'documents' && (
        <div className="card">
          {documents.length === 0 ? <EmptyState icon="LD" title="No active legal documents" /> : (
            <div style={{ display: 'grid', gap: 14 }}>
              {documents.map(doc => (
                <div key={doc.id} className="card" style={{ background: 'var(--bg)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                    <div>
                      <div className="card-title">{labels[doc.documentType] || doc.title}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Version {doc.version} · Effective {doc.effectiveDate}</div>
                    </div>
                    <Badge status={doc.accepted ? 'verified' : 'pending'} />
                  </div>
                  <p style={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{doc.content}</p>
                </div>
              ))}
              <label style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                <input type="checkbox" checked={acceptChecked} onChange={e => setAcceptChecked(e.target.checked)} />
                <span>I accept the active Service Agreement, Privacy Policy, and Data Processing Agreement on behalf of my hospital.</span>
              </label>
              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <button className="btn btn-primary" disabled={allAccepted} onClick={acceptAll}>{allAccepted ? 'Accepted' : 'Accept Legal Documents'}</button>
              </div>
            </div>
          )}
        </div>
      )}

      {activeTab === 'signed' && (
        <div className="card">
          <div className="card-title">Signed Service Agreement</div>
          <input type="file" accept="application/pdf" disabled={uploading} onChange={e => upload(e.target.files?.[0])} />
          <div style={{ marginTop: 16 }}>
            {signed.length === 0 ? <EmptyState icon="SA" title="No signed agreement uploaded" /> : (
              <table>
                <thead><tr><th>File</th><th>Status</th><th>Uploaded</th><th>Notes</th><th></th></tr></thead>
                <tbody>{signed.map(item => (
                  <tr key={item.id}>
                    <td>{item.originalFilename}</td>
                    <td><Badge status={(item.reviewStatus || 'PENDING').toLowerCase()} /></td>
                    <td>{item.uploadedAt ? new Date(item.uploadedAt).toLocaleString('en-IN') : '-'}</td>
                    <td>{item.reviewNotes || '-'}</td>
                    <td><button className="btn btn-secondary btn-sm" onClick={() => download(item)}>Download</button></td>
                  </tr>
                ))}</tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {activeTab === 'history' && (
        <div className="table-wrap">
          {acceptances.length === 0 ? <EmptyState icon="AH" title="No acceptance history" /> : (
            <table>
              <thead><tr><th>Document</th><th>Version</th><th>Accepted By</th><th>Accepted At</th><th>Plan Snapshot</th></tr></thead>
              <tbody>{acceptances.map(a => (
                <tr key={a.id}>
                  <td>{labels[a.documentType] || a.documentType}</td>
                  <td>{a.documentVersion}</td>
                  <td>{a.acceptedByName}<br/><span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{a.acceptedByEmail}</span></td>
                  <td>{a.acceptedAt ? new Date(a.acceptedAt).toLocaleString('en-IN') : '-'}</td>
                  <td>{a.subscriptionPlan || '-'} {a.maxUsers ? `· ${a.maxUsers} users` : ''}</td>
                </tr>
              ))}</tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
};

export default LegalAgreements;
