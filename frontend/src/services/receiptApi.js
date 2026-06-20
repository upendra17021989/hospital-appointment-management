import { API_BASE } from './utils';
import api from './api';

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
  list: (params) => api.get('/consultation-receipts', { params }),
  stats: () => api.get('/consultation-receipts/stats'),
  create: (payload) => api.post('/consultation-receipts', payload),
  get: (id) => api.get(`/consultation-receipts/${id}`),
  activeByAppointment: (appointmentId) => api.get(`/consultation-receipts/appointment/${appointmentId}/active`),
  update: (id, payload) => api.put(`/consultation-receipts/${id}`, payload),
  void: (id) => api.post(`/consultation-receipts/${id}/void`, {}),
  patientHistory: (patientId) => api.get(`/consultation-receipts/patient/${patientId}`),
  report: (params) => api.get('/consultation-receipts/reports/collections', { params }),

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

  downloadById: async (receiptId) => {
    const token = getToken();
    const res = await fetch(`${API_BASE}/consultation-receipts/${receiptId}/pdf`, {
      method: 'GET',
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

  exportReport: async (params = {}) => {
    const token = getToken();
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.append(key, value);
    });
    const res = await fetch(`${API_BASE}/consultation-receipts/reports/collections/export?${query}`, {
      method: 'GET',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(text || `Export failed: ${res.status}`);
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

export const saveBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
};

export const printBlob = (blob) => {
  const url = URL.createObjectURL(blob);
  const win = window.open(url, '_blank');
  if (win) {
    win.onload = () => win.print();
  }
  setTimeout(() => URL.revokeObjectURL(url), 60000);
};
