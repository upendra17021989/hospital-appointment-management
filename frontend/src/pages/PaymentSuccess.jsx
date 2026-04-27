import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

export default function PaymentSuccess() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [countdown, setCountdown] = useState(5);

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown(c => {
        if (c <= 1) {
          clearInterval(timer);
          navigate('/dashboard');
          return 0;
        }
        return c - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [navigate]);

  const sessionId = searchParams.get('session_id');

  return (
    <div className="page-container payment-result-page success">
      <div className="payment-result-card">
        <div className="result-icon">🎉</div>
        <h1>Payment Successful!</h1>
        <p>Thank you for upgrading your plan. Your subscription is now active.</p>
        {sessionId && <p className="session-id">Session: {sessionId}</p>}
        <p className="redirect-msg">Redirecting to dashboard in {countdown} seconds...</p>
        <button className="btn btn-primary" onClick={() => navigate('/dashboard')}>
          Go to Dashboard
        </button>
      </div>
    </div>
  );
}

