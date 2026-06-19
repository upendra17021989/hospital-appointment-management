import api from './api';
import { API_BASE } from './utils';

const getToken = () => localStorage.getItem('hms_token');

const fetchBlob = async (path) => {
  const token = getToken();
  const res = await fetch(`${API_BASE}${path}`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `Download failed: ${res.status}`);
  }
  return res.blob();
};

export const certificateApi = {
  list: (params) => api.get('/medical-certificates', { params }),
  create: (payload) => api.post('/medical-certificates', payload),
  get: (id) => api.get(`/medical-certificates/${id}`),
  update: (id, payload) => api.put(`/medical-certificates/${id}`, payload),
  void: (id) => api.post(`/medical-certificates/${id}/void`, {}),
  patientHistory: (patientId) => api.get(`/medical-certificates/patient/${patientId}`),
  downloadPdf: (id) => fetchBlob(`/medical-certificates/${id}/pdf`),
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
  if (win) win.onload = () => win.print();
  setTimeout(() => URL.revokeObjectURL(url), 60000);
};
