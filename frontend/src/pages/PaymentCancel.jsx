import React from 'react';
import { useNavigate } from 'react-router-dom';

export default function PaymentCancel() {
  const navigate = useNavigate();

  return (
    <div className="page-container payment-result-page cancel">
      <div className="payment-result-card">
        <div className="result-icon">❌</div>
        <h1>Payment Cancelled</h1>
        <p>No worries — you can upgrade anytime from the Billing & Plans page.</p>
        <div className="result-actions">
          <button className="btn btn-outline" onClick={() => navigate('/subscription-plans')}>
            View Plans
          </button>
          <button className="btn btn-primary" onClick={() => navigate('/dashboard')}>
            Back to Dashboard
          </button>
        </div>
      </div>
    </div>
  );
}

