import React, { useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import BillingReceiptViewer from './BillingReceiptViewer';
import api from '../services/api';



/**
 * ConsultationReceiptPrint
 * Dedicated screen for consultation receipt printing.
 *
 * - Accepts consultationPaymentId from URL params (optional, passed by navigation)
 * - Uses BillingReceiptViewer UI for PDF generation/reprint
 * - IMPORTANT UX: appointment will be marked completed only AFTER user clicks
 *   "Save & Print Receipt (PDF)" on this screen.
 */

const ConsultationReceiptPrint = () => {
  const location = useLocation();

  // We expect navigation to pass appointment id + appt snapshot in state.
  // (Keeping it resilient because route params parsing isn't in this project yet.)
  const state = location?.state || {};

  const { consultationPayment, appointmentId } = state;

  const [pendingComplete, setPendingComplete] = useState(false);

  // Pass-through viewer props.
  // BillingReceiptViewer supports either: payment={...} or receiptNumber.
  const viewerPayment = useMemo(() => {
    return consultationPayment;
  }, [consultationPayment]);

  return (
    <BillingReceiptViewer
      payment={viewerPayment}
      receiptNumber={undefined}
      onAfterSaveAndPrint={async () => {
        // Mark appointment completed AFTER user clicks Save & Print on this screen.
        if (appointmentId) {
          setPendingComplete(true);
          await api.patch(`/appointments/hospital/${appointmentId}/status`, { status: 'completed' });
        }
      }}
    />
  );
};

export default ConsultationReceiptPrint;

