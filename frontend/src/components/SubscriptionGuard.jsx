import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Route wrapper that blocks access if subscription has expired.
 * SUPER_ADMIN always passes. Free-plan users pass but may see upsells.
 */
export default function SubscriptionGuard({ children, requireFeature }) {
  const { isAuthenticated, isExpired, planSlug, prescriptionsEnabled } = useAuth();

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  // Block if fully expired (not even free plan access)
  if (isExpired && planSlug !== 'free') {
    return <Navigate to="/subscription-plans" replace />;
  }

  // Feature-specific checks
  if (requireFeature === 'prescriptions' && !prescriptionsEnabled) {
    return (
      <div className="page-container">
        <div className="empty-state">
          <div className="empty-icon">🔒</div>
          <h3>Feature Locked</h3>
          <p>Prescriptions require a paid plan. Upgrade to unlock this feature.</p>
          <button className="btn btn-primary" onClick={() => window.location.href = '/subscription-plans'}>
            Upgrade Plan
          </button>
        </div>
      </div>
    );
  }

  return children;
}

