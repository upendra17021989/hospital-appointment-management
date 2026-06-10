import React, { useMemo } from 'react';

const ReceiptViewer = ({ payment }) => {
  const data = payment?.data;

  const receiptNo = useMemo(() => {
    if (!data?.id) return '—';
    return String(data.id).slice(0, 8).toUpperCase();
  }, [data?.id]);

  const paidAt = data?.paidAt ? new Date(data.paidAt).toLocaleString() : '—';
  const createdAt = data?.createdAt ? new Date(data.createdAt).toLocaleString() : '—';

  return (
    <div className="page-container" style={{ padding: '24px 0' }}>
      <div
        className="card"
        style={{ maxWidth: 720, margin: '0 auto', padding: 22, background: 'white' }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: 18 }}>Hospital Consultation Receipt</div>
            <div style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 4 }}>
              (Generated from payment record)
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Receipt No</div>
            <div style={{ fontWeight: 800 }}>{receiptNo}</div>
          </div>
        </div>

        <div style={{ height: 14 }} />
        <hr />

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 14 }}>
          <KV k="Payment ID" v={data?.id || '—'} />
          <KV k="Status" v={data?.status || '—'} />
          <KV
            k="Amount"
            v={data?.amount != null ? `₹${Number(data.amount).toFixed(2)}` : '—'}
          />
          <KV k="Currency" v={data?.currency || '—'} />
          <KV k="Description" v={data?.description || '—'} />
          <KV k="Paid At" v={paidAt} />
          <KV k="Recorded At" v={createdAt} />
          <KV k="Type" v={data?.description?.includes('Subscription') ? 'Consultation (Subscription Model)' : 'Consultation'} />
        </div>

        <div style={{ height: 18 }} />
        <hr />

        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, marginTop: 16 }}>
          <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
            This receipt is system generated. For any discrepancies, contact billing desk.
          </div>
          <div style={{ textAlign: 'right', fontSize: 12, color: 'var(--text-muted)' }}>
            Authorized Signatory
            <div style={{ height: 34 }} />
            ______________________
          </div>
        </div>

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 22 }}>
          <button className="btn btn-secondary" onClick={() => window.history.back()}>
            Back
          </button>
          <button className="btn btn-primary" onClick={() => window.print()}>
            Print Receipt
          </button>
        </div>
      </div>
    </div>
  );
};

const KV = ({ k, v }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{k}</div>
    <div style={{ fontSize: 14, fontWeight: 700 }}>{v}</div>
  </div>
);

export default ReceiptViewer;

