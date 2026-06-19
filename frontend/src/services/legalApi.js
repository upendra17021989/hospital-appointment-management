import api from './api';
import { API_BASE } from './utils';

const getToken = () => localStorage.getItem('hms_token');

const authedFetch = (path, options = {}) => {
  const token = getToken();
  return fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  });
};

export const legalApi = {
  activeDocuments: () => api.get('/legal/documents/active'),
  acceptAll: (acceptanceText) => api.post('/legal/accept', { acceptanceText }),
  acceptances: () => api.get('/legal/acceptances'),
  signedAgreements: () => api.get('/legal/signed-agreements'),
  uploadSignedAgreement: async (file) => {
    const data = new FormData();
    data.append('file', file);
    const res = await authedFetch('/legal/signed-agreements', { method: 'POST', body: data });
    const body = await res.json();
    if (!body.success) throw new Error(body.message || 'Upload failed');
    return body.data;
  },
  downloadSignedAgreement: async (id, admin = false) => {
    const path = admin ? `/legal/admin/signed-agreements/${id}/download` : `/legal/signed-agreements/${id}/download`;
    const res = await authedFetch(path);
    if (!res.ok) throw new Error(`Download failed: ${res.status}`);
    return res.blob();
  },
  adminSignedAgreements: (status) => api.get('/legal/admin/signed-agreements', { params: { status } }),
  reviewSignedAgreement: (id, payload) => api.post(`/legal/admin/signed-agreements/${id}/review`, payload),
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
