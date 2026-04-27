import React, { useState, useEffect } from 'react';
import { paymentApi } from '../services/api';

export default function BillingHistory() {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    paymentApi.getHistory()
      .then(data => setPayments(data || []))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const formatDate = (iso) => {
    if (!iso) return '-';
    return new Date(iso).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  };

  const statusBadge = (status) => {
    const map = {
      succeeded: 'badge-success',
      pending: 'badge-warning',
      failed: 'badge-danger',
      refunded: 'badge-info',
    };
    return <span className={`badge ${map[status] || 'badge-secondary'}`}>{status}</span>;
  };

  return (
    <div className="page-container billing-history-page">
      <div className="page-header">
        <h1 className="page-title">Billing History</h1>
        <p className="page-subtitle">View your past payments and invoices</p>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem' }}>
          <div className="spinner" style={{ margin: '0 auto 1rem', width: 32, height: 32 }} />
          <p>Loading history...</p>
        </div>
      ) : payments.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📄</div>
          <h3>No payments yet</h3>
          <p>Your payment history will appear here once you make a purchase.</p>
        </div>
      ) : (
        <div className="billing-table-wrapper">
          <table className="data-table billing-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Amount</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {payments.map(p => (
                <tr key={p.id}>
                  <td>{formatDate(p.createdAt)}</td>
                  <td>{p.description || 'Payment'}</td>
                  <td>${p.amount} {p.currency}</td>
                  <td>{statusBadge(p.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

