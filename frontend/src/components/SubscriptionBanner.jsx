import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function SubscriptionBanner() {
  const navigate = useNavigate();
  const { isAuthenticated, isTrial, isExpired, daysUntilExpiry, planSlug } = useAuth();

  if (!isAuthenticated) return null;

  // Expired — blocking banner
  if (isExpired) {
    return (
      <div className="subscription-banner banner-danger">
        <div className="banner-content">
          <span className="banner-icon">⚠️</span>
          <span className="banner-text">
            <strong>Subscription expired.</strong> Please renew to continue using all features.
          </span>
        </div>
        <button className="btn btn-sm btn-light" onClick={() => navigate('/subscription-plans')}>
          Renew Now
        </button>
      </div>
    );
  }

  // Trial ending soon
  if (isTrial && daysUntilExpiry !== null && daysUntilExpiry <= 7) {
    return (
      <div className={`subscription-banner ${daysUntilExpiry <= 2 ? 'banner-warning' : 'banner-info'}`}>
        <div className="banner-content">
          <span className="banner-icon">⏳</span>
          <span className="banner-text">
            Your trial ends in <strong>{daysUntilExpiry} day{daysUntilExpiry !== 1 ? 's' : ''}</strong>. Upgrade to keep full access.
          </span>
        </div>
        <button className="btn btn-sm btn-light" onClick={() => navigate('/subscription-plans')}>
          Upgrade
        </button>
      </div>
    );
  }

  // Active but on free plan
  if (!isTrial && planSlug === 'free') {
    return (
      <div className="subscription-banner banner-info">
        <div className="banner-content">
          <span className="banner-icon">🚀</span>
          <span className="banner-text">
            You're on the <strong>Free plan</strong>. Upgrade to unlock prescriptions, SMS, and more.
          </span>
        </div>
        <button className="btn btn-sm btn-light" onClick={() => navigate('/subscription-plans')}>
          See Plans
        </button>
      </div>
    );
  }

  return null;
}

