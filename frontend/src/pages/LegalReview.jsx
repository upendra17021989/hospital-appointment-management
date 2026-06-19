import React, { useEffect, useState } from 'react';
import { Badge, EmptyState, LoadingSpinner, Modal, PageHeader, Tabs } from '../components/Common';
import { legalApi, saveBlob } from '../services/legalApi';

const LegalReview = () => {
  const [status, setStatus] = useState('PENDING');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [notes, setNotes] = useState('');
  const [message, setMessage] = useState('');

  const load = async (nextStatus = status) => {
    setLoading(true);
    try {
      setItems(await legalApi.adminSignedAgreements(nextStatus || undefined) || []);
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(status); }, [status]);

  const review = async (reviewStatus) => {
    if (!selected) return;
    if (reviewStatus === 'REJECTED' && !notes.trim()) {
      setMessage('Error: Rejection notes are required.');
      return;
    }
    try {
      await legalApi.reviewSignedAgreement(selected.id, { status: reviewStatus, notes });
      setSelected(null);
      setNotes('');
      setMessage(`Signed agreement marked ${reviewStatus.toLowerCase()}.`);
      await load(status);
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    }
  };

  const download = async (item) => {
    const blob = await legalApi.downloadSignedAgreement(item.id, true);
    saveBlob(blob, item.originalFilename);
  };

  return (
    <div>
      <PageHeader title="Legal Review" subtitle="Review signed service agreements submitted by hospitals" />
      {message && <div className={`alert ${message.startsWith('Error') ? 'alert-error' : 'alert-success'}`}>{message}</div>}
      <Tabs active={status} onChange={setStatus} tabs={[
        { value: 'PENDING', label: 'Pending' },
        { value: 'ACCEPTED', label: 'Accepted' },
        { value: 'REJECTED', label: 'Rejected' },
        { value: '', label: 'All' },
      ]} />
      <div className="table-wrap table-wrap--scrollable">
        {loading ? <LoadingSpinner /> : items.length === 0 ? <EmptyState icon="LR" title="No signed agreements found" /> : (
          <table>
            <thead><tr><th>Hospital</th><th>File</th><th>Status</th><th>Uploaded</th><th>Notes</th><th>Actions</th></tr></thead>
            <tbody>{items.map(item => (
              <tr key={item.id}>
                <td>{item.hospitalName || '-'}</td>
                <td>{item.originalFilename}</td>
                <td><Badge status={(item.reviewStatus || 'PENDING').toLowerCase()} /></td>
                <td>{item.uploadedAt ? new Date(item.uploadedAt).toLocaleString('en-IN') : '-'}</td>
                <td>{item.reviewNotes || '-'}</td>
                <td>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button className="btn btn-secondary btn-sm" onClick={() => download(item)}>Download</button>
                    <button className="btn btn-primary btn-sm" onClick={() => { setSelected(item); setNotes(item.reviewNotes || ''); }}>Review</button>
                  </div>
                </td>
              </tr>
            ))}</tbody>
          </table>
        )}
      </div>
      {selected && (
        <Modal title={`Review ${selected.hospitalName}`} onClose={() => setSelected(null)}>
          <div className="form-group">
            <label>Review Notes</label>
            <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={4} />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
            <button className="btn btn-secondary" onClick={() => review('PENDING')}>Mark Pending</button>
            <button className="btn btn-danger" onClick={() => review('REJECTED')}>Reject</button>
            <button className="btn btn-primary" onClick={() => review('ACCEPTED')}>Accept</button>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default LegalReview;
