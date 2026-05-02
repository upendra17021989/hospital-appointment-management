const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Get JWT token from localStorage
const getToken = () => localStorage.getItem('hms_token');

// Build headers — automatically attach Bearer token if present
const headers = (extra = {}) => {
  const h = { 'Content-Type': 'application/json', ...extra };
  const token = getToken();
  if (token) h['Authorization'] = `Bearer ${token}`;
  return h;
};

const api = {
  get: async (path, options = {}) => {
    let url = `${API_BASE}${path}`;
    if (options.params) {
      const searchParams = new URLSearchParams();
      Object.entries(options.params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          searchParams.append(key, value);
        }
      });
      const queryString = searchParams.toString();
      if (queryString) {
        url += `?${queryString}`;
      }
    }
    const res = await fetch(url, { headers: headers() });
    if (!res.ok) throw new Error(`API error: ${res.status}`);
    const data = await res.json();
    return data.data;
  },

  post: async (path, body) => {
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!data.success) throw new Error(data.message || 'Request failed');
    return data;
  },

  patch: async (path, body) => {
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'PATCH',
      headers: headers(),
      body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!data.success) throw new Error(data.message || 'Request failed');
    return data;
  },

  put: async (path, body) => {
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'PUT',
      headers: headers(),
      body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!data.success) throw new Error(data.message || 'Request failed');
    return data;
  },

  delete: async (path) => {
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'DELETE',
      headers: headers(),
    });
    if (!res.ok) throw new Error(`Delete failed: ${res.status}`);
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  },
};

// ── Subscription API helpers ──────────────────────────────────

export const subscriptionApi = {
  getPlans: () => api.get('/subscriptions/plans'),
  getMySubscription: () => api.get('/subscriptions/me'),
  getUsage: () => api.get('/subscriptions/usage'),
  createCheckout: (planId, billingCycle = 'monthly') =>
    api.post(`/subscriptions/checkout?planId=${planId}&billingCycle=${billingCycle}`),
  cancel: () => api.post('/subscriptions/cancel'),
};

// ── Payment API helpers ───────────────────────────────────────

export const paymentApi = {
  getHistory: (page = 0, size = 20) =>
    api.get(`/payments/history?page=${page}&size=${size}`),
};

export default api;
