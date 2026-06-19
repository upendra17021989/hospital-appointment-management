import api from './api';
import { API_BASE } from './utils';

const getToken = () => localStorage.getItem('hms_token');

export const hospitalVerificationApi = {
  list: (status) => api.get('/admin/hospital-verifications', { params: { status } }),
  get: (hospitalId) => api.get(`/admin/hospital-verifications/${hospitalId}`),
  review: (hospitalId, payload) => api.post(`/admin/hospital-verifications/${hospitalId}/review`, payload),
  downloadDocument: async (hospitalId, documentId) => {
    const token = getToken();
    const res = await fetch(`${API_BASE}/admin/hospital-verifications/${hospitalId}/documents/${documentId}/download`, {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(text || `Download failed: ${res.status}`);
    }
    return res.blob();
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
