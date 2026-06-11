import React, { useEffect, useMemo, useState } from 'react';
import { consultationReceiptApi } from '../services/receiptApi';

/**
 * BillingReceiptViewer
 * - Supports showing a locally-provided receipt/payment object (optional)
 * - Supports download/reprint using backend PDF endpoints
 *
 * Usage patterns (supported):
 * 1) <BillingReceiptViewer payment={...} /> where payment has consultationPaymentId
 *    or nested payment info.
 * 2) <BillingReceiptViewer receiptNumber="RCPT-..." /> for reprints.
 */
const BillingReceiptViewer = ({ payment, receiptNumber: propReceiptNumber }) => {
  const [isDownloading, setIsDownloading] = useState(false);
  const [error, setError] = useState(null);
  const [receiptNumber, setReceiptNumber] = useState(propReceiptNumber || null);

  // Attempt to derive receiptNumber for display only
  const derivedReceiptNumber = useMemo(() => {
    if (propReceiptNumber) return propReceiptNumber;

    // If backend returns receipt fields inside payment/receipt object, try a few shapes.
    const maybe = payment?.receiptNumber || payment?.data?.receiptNumber;
    if (maybe) return maybe;

    return null;
  }, [payment, propReceiptNumber]);

  useEffect(() => {
    if (derivedReceiptNumber) setReceiptNumber(derivedReceiptNumber);
  }, [derivedReceiptNumber]);

  const printLocal = () => {
    window.print();
  };

  const downloadBlob = async (blob, suggestedFilename) => {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = suggestedFilename || 'receipt.pdf';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  };

  const handleSaveAndPrint = async () => {
    setIsDownloading(true);
    setError(null);
    try {
      const consultationPaymentId = payment?.consultationPaymentId || payment?.id || payment?.data?.id;
      if (!consultationPaymentId) {
        throw new Error('Missing consultation payment id for Save & Print');
      }

      const blob = await consultationReceiptApi.saveAndPrintByPaymentId(consultationPaymentId);
      const fn = `${receiptNumber || 'consultation-receipt'}-save-and-print.pdf`;
      await downloadBlob(blob, fn);
    } catch (e) {
      setError(e.message || String(e));
    } finally {
      setIsDownloading(false);
    }
  };

  const handleReprint = async () => {
    setIsDownloading(true);
    setError(null);
    try {
      if (!receiptNumber) throw new Error('Missing receipt number for reprint');

      const blob = await consultationReceiptApi.reprintByReceiptNumber(receiptNumber);
      const fn = `${receiptNumber}.pdf`;
      await downloadBlob(blob, fn);
    } catch (e) {
      setError(e.message || String(e));
    } finally {
      setIsDownloading(false);
    }
  };

  // Display helpers
  const display = payment?.data || payment || {};

  const paidAt = display?.paidAt ? new Date(display.paidAt).toLocaleString() : null;

  return (
    <div className="page-container" style={{ padding: '24px 0' }}>
      <div
        className="card"
        style={{ maxWidth: 760, margin: '0 auto', padding: 22, background: 'white' }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: 18 }}>Hospital Consultation Receipt</div>
            <div style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 4 }}>
              Downloadable PDF generated from backend snapshot
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Receipt No</div>
            <div style={{ fontWeight: 800 }}>{receiptNumber || '—'}</div>
            {paidAt && (
              <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 6 }}>{paidAt}</div>
            )}
          </div>
        </div>

        <div style={{ height: 14 }} />
        <hr />

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 14 }}>
          <KV k="Patient" v={display?.patientName || display?.patient?.fullName || '—'} />
          <KV k="Doctor" v={display?.doctorName || display?.doctor?.fullName || '—'} />
          <KV k="Department" v={display?.departmentName || display?.department?.name || '—'} />
          <KV
            k="Amount Paid"
            v={
              display?.amountPaid != null
                ? `₹${Number(display.amountPaid).toFixed(2)}`
                : display?.amount != null
                  ? `₹${Number(display.amount).toFixed(2)}`
                  : '—'
            }
          />
          <KV k="Payment Mode" v={display?.paymentMode || display?.payment_mode || '—'} />
          <KV k="Received By" v={display?.receivedByName || display?.receivedBy?.fullName || '—'} />
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

        {error && <div className="alert alert-danger" style={{ marginTop: 16 }}>{error}</div>}

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 22, flexWrap: 'wrap' }}>
          <button className="btn btn-secondary" onClick={() => window.history.back()} disabled={isDownloading}>
            Back
          </button>

          <button className="btn btn-secondary" onClick={printLocal} disabled={isDownloading}>
            Print (Browser)
          </button>

          <button className="btn btn-primary" onClick={handleSaveAndPrint} disabled={isDownloading}>
            {isDownloading ? 'Downloading...' : 'Save & Print Receipt (PDF)'}
          </button>

          <button
            className="btn btn-accent"
            onClick={handleReprint}
            disabled={isDownloading || !receiptNumber}
            title={receiptNumber ? 'Reprint from receipt number' : 'Receipt number required'}
          >
            {isDownloading ? 'Downloading...' : 'Reprint Receipt (PDF)'}
          </button>
        </div>
      </div>
    </div>
  );
};

const KV = ({ k, v }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{k}</div>
    <div style={{ fontSize: 14, fontWeight: 700 }}>{v || '—'}</div>
  </div>
);

export default BillingReceiptViewer;

