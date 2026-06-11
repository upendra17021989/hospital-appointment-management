import { API_BASE } from './utils';

const getToken = () => localStorage.getItem('hms_token');

export const consultationPaymentApi = {
  createPayment: async (payload) => {
    const url = `${API_BASE}/consultation-payments`;
    const token = getToken();

    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(payload),
    });

    const data = await res.json().catch(() => null);

    if (!res.ok) {
      const msg = data?.message || data?.error || `Consultation payment failed: ${res.status}`;
      throw new Error(msg);
    }

    // API returns ApiResponse.success(data)
    if (data?.success === false) {
      throw new Error(data?.message || 'Consultation payment failed');
    }

    return data?.data || data;
  },
};

