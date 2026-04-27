import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { subscriptionApi } from '../services/api';

const PLAN_FEATURES = {
  free: ['Up to 3 doctors', 'Up to 2 users', '100 appointments/mo', 'Basic support'],
  basic: ['Up to 10 doctors', 'Up to 5 users', '500 appointments/mo', 'Prescriptions', 'Email support'],
  pro: ['Up to 25 doctors', 'Up to 15 users', 'Unlimited appointments', 'Prescriptions', 'SMS & WhatsApp', 'Custom branding', 'Priority support'],
  enterprise: ['Unlimited doctors', 'Up to 50 users', 'Unlimited everything', 'All Pro features', 'Dedicated support'],
};

const PlanCard = ({ plan, isCurrent, onSelect, billingCycle }) => {
  const monthly = plan.monthlyPrice;
  const yearly = plan.yearlyPrice;
  const price = billingCycle === 'yearly' ? yearly : monthly;
  const period = billingCycle === 'yearly' ? '/year' : '/mo';
  const isPopular = plan.slug === 'pro';
  const features = PLAN_FEATURES[plan.slug] || PLAN_FEATURES.free;

  return (
    <div className={`plan-card ${isCurrent ? 'current' : ''} ${isPopular ? 'popular' : ''}`}>
      {isPopular && <div className="plan-badge">Most Popular</div>}
      {isCurrent && <div className="plan-current-badge">Current Plan</div>}
      <h3 className="plan-name">{plan.name}</h3>
      <p className="plan-desc">{plan.description}</p>
      <div className="plan-price">
        <span className="plan-currency">$</span>
        <span className="plan-amount">{price}</span>
        <span className="plan-period">{period}</span>
      </div>
      <ul className="plan-features">
        {features.map((f, i) => (
          <li key={i}><span className="plan-check">✓</span> {f}</li>
        ))}
      </ul>
      <button
        className={`btn btn-primary plan-cta ${isCurrent ? 'disabled' : ''}`}
        onClick={() => onSelect(plan)}
        disabled={isCurrent}
      >
        {isCurrent ? 'Current Plan' : 'Choose Plan'}
      </button>
    </div>
  );
};

export default function SubscriptionPlans() {
  const navigate = useNavigate();
  const { token, subscription, planSlug } = useAuth();
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [checkoutLoading, setCheckoutLoading] = useState(false);
  const [billingCycle, setBillingCycle] = useState('monthly');
  const [error, setError] = useState(null);

  useEffect(() => {
    subscriptionApi.getPlans()
      .then(data => setPlans(data || []))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const handleSelect = async (plan) => {
    if (!token) {
      navigate('/login');
      return;
    }
    if (plan.slug === 'free') {
      // Downgrade handled server-side if needed; just refresh
      window.location.reload();
      return;
    }
    setCheckoutLoading(true);
    setError(null);
    try {
      const res = await subscriptionApi.createCheckout(plan.id, billingCycle);
      if (res.data?.checkoutUrl) {
        window.location.href = res.data.checkoutUrl;
      } else {
        setError('Failed to start checkout. Please try again.');
      }
    } catch (err) {
      setError(err.message || 'Checkout failed');
    } finally {
      setCheckoutLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="page-container" style={{ textAlign: 'center', padding: '4rem' }}>
        <div className="spinner" style={{ margin: '0 auto 1rem', width: 40, height: 40 }} />
        <p>Loading plans...</p>
      </div>
    );
  }

  return (
    <div className="page-container subscription-plans-page">
      <div className="page-header" style={{ textAlign: 'center', marginBottom: '2rem' }}>
        <h1 className="page-title">Choose Your Plan</h1>
        <p className="page-subtitle">Upgrade to unlock more features for your hospital</p>

        <div className="billing-toggle">
          <button className={billingCycle === 'monthly' ? 'active' : ''} onClick={() => setBillingCycle('monthly')}>Monthly</button>
          <button className={billingCycle === 'yearly' ? 'active' : ''} onClick={() => setBillingCycle('yearly')}>Yearly <span className="save-badge">Save 17%</span></button>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {checkoutLoading && <div className="alert alert-info">Redirecting to secure checkout...</div>}

      <div className="plans-grid">
        {plans.map(plan => (
          <PlanCard
            key={plan.id}
            plan={plan}
            isCurrent={plan.slug === planSlug}
            onSelect={handleSelect}
            billingCycle={billingCycle}
          />
        ))}
      </div>
    </div>
  );
}

