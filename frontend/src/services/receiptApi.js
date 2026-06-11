import { API_BASE } from './utils';

// Minimal helper for authenticated PDF download.
// NOTE: we keep token retrieval consistent with api.js
const getToken = () => localStorage.getItem('hms_token');

const downloadPdf = async (url) => {
  const token = getToken();

  const res = await fetch(url, {
    method: 'POST',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `PDF download failed: ${res.status}`);
  }

  const blob = await res.blob();
  return blob;
};

export const consultationReceiptApi = {
  saveAndPrintByPaymentId: async (consultationPaymentId) => {
    const url = `${API_BASE}/consultation-receipts/payment/${consultationPaymentId}/pdf`;

    // This endpoint is POST and returns application/pdf
    // We intentionally use fetch+blob (no window.location)
    const token = getToken();

    const res = await fetch(url, {
      method: 'POST',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(text || `Receipt download failed: ${res.status}`);
    }

    return await res.blob();
  },

  reprintByReceiptNumber: async (receiptNumber) => {
    const url = `${API_BASE}/consultation-receipts/hospital/${encodeURIComponent(receiptNumber)}/pdf`;

    const token = getToken();

    const res = await fetch(url, {
      method: 'GET',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(text || `Receipt reprint failed: ${res.status}`);
    }

    return await res.blob();
  },
};

