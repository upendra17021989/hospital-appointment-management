import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';

const API = 'http://localhost:8080/api';

const STEPS = [
  { num: 1, label: 'Hospital Info' },
  { num: 2, label: 'Admin Account' },
  { num: 3, label: 'Review' },
];

const StepDots = ({ current }) => (
  <div className="auth-steps" style={{ marginBottom: 28 }}>
    {STEPS.map((s, idx) => (
      <React.Fragment key={s.num}>
        <div className={`auth-step-dot ${current === s.num ? 'active' : current > s.num ? 'done' : ''}`}>
          {current > s.num ? '✓' : s.num}
          <span className="auth-step-label">{s.label}</span>
        </div>
        {idx < STEPS.length - 1 && (
          <div className={`auth-step-line ${current > s.num ? 'done' : ''}`} />
        )}
      </React.Fragment>
    ))}
  </div>
);

const Signup = ({ onSwitchToLogin }) => {
  const { saveAuth } = useAuth();
  const [step,    setStep]    = useState(1);
  const [error,   setError]   = useState('');
  const [loading, setLoading] = useState(false);
  const [showPass, setShowPass] = useState(false);

  const [hospital, setHospital] = useState({
    hospitalName: '', hospitalAddress: '', hospitalCity: '',
    hospitalState: '', hospitalPhone: '', hospitalEmail: '',
    hospitalWebsite: '', licenseNumber: '',
  });

  const [admin, setAdmin] = useState({
    firstName: '', lastName: '',
    email: '', password: '', confirmPassword: '',
  });

  const setH = k => e => setHospital(h => ({ ...h, [k]: e.target.value }));
  const setA = k => e => setAdmin(a =>  ({ ...a, [k]: e.target.value }));

  const validateStep1 = () => {
    if (!hospital.hospitalName.trim()) return 'Hospital name is required.';
    if (!hospital.hospitalCity.trim()) return 'City is required.';
    if (!hospital.hospitalPhone.trim()) return 'Phone number is required.';
    return '';
  };

  const validateStep2 = () => {
    if (!admin.firstName.trim()) return 'First name is required.';
    if (!admin.lastName.trim())  return 'Last name is required.';
    if (!admin.email.trim())     return 'Email is required.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(admin.email)) return 'Invalid email address.';
    if (admin.password.length < 8) return 'Password must be at least 8 characters.';
    if (admin.password !== admin.confirmPassword) return 'Passwords do not match.';
    return '';
  };

  const goNext = () => {
    const err = step === 1 ? validateStep1() : validateStep2();
    if (err) { setError(err); return; }
    setError('');
    setStep(s => s + 1);
  };

  const goBack = () => { setError(''); setStep(s => s - 1); };

  const handleRegister = async () => {
    setLoading(true); setError('');
    try {
      const payload = {
        ...hospital,
        firstName: admin.firstName,
        lastName:  admin.lastName,
        email:     admin.email,
        password:  admin.password,
      };
      const res  = await fetch(`${API}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      if (!data.success) { setError(data.message || 'Registration failed.'); setStep(2); return; }
      saveAuth(data.data.token, data.data);
    } catch {
      setError('Unable to connect to server. Please try again.');
      setStep(2);
    } finally {
      setLoading(false);
    }
  };

  const passStrength =
    admin.password.length === 0 ? null :
    admin.password.length < 8   ? 'weak' :
    admin.password.length < 12  ? 'medium' : 'strong';

  const passStrengthLabel = { weak: 'Too short', medium: 'Good', strong: 'Strong' };

  return (
    <div className="auth-shell">
      {/* Left panel */}
      <div className="auth-left">
        <div className="auth-brand">
          <div className="auth-brand-logo">✚</div>
          <div className="auth-brand-name">MediCare+</div>
        </div>
        <h1 className="auth-hero-title">Join thousands of<br/>hospitals nationwide.</h1>
        <p className="auth-hero-sub">
          Set up your hospital in under 2 minutes. Each hospital gets a completely
          isolated workspace — your data stays yours.
        </p>
        <div className="auth-features">
          {[
            'Isolated hospital workspace',
            'Unlimited staff accounts',
            'Full appointment management',
            'Secure patient data',
          ].map(f => (
            <div key={f} className="auth-feature-item">
              <span className="auth-feature-check">✓</span> {f}
            </div>
          ))}
        </div>
      </div>

      {/* Right panel */}
      <div className="auth-right">
        <div className="auth-card auth-card-wide">
          <div className="auth-card-header">
            <h2 className="auth-card-title">Register your hospital</h2>
            <p className="auth-card-sub">Create your hospital admin account</p>
          </div>

          <StepDots current={step} />

          {error && <div className="alert alert-error">{error}</div>}

          {/* ── Step 1: Hospital Info ── */}
          {step === 1 && (
            <div className="auth-form">
              <div className="form-grid">
                <div className="form-group full">
                  <label>Hospital Name *</label>
                  <input
                    value={hospital.hospitalName}
                    onChange={setH('hospitalName')}
                    placeholder="e.g. Apollo Hospitals, Mumbai"
                    autoFocus
                  />
                </div>
                <div className="form-group full">
                  <label>Address</label>
                  <input
                    value={hospital.hospitalAddress}
                    onChange={setH('hospitalAddress')}
                    placeholder="Street address"
                  />
                </div>
                <div className="form-group">
                  <label>City *</label>
                  <input value={hospital.hospitalCity} onChange={setH('hospitalCity')} placeholder="e.g. Mumbai" />
                </div>
                <div className="form-group">
                  <label>State</label>
                  <input value={hospital.hospitalState} onChange={setH('hospitalState')} placeholder="e.g. Maharashtra" />
                </div>
                <div className="form-group">
                  <label>Hospital Phone *</label>
                  <input value={hospital.hospitalPhone} onChange={setH('hospitalPhone')} placeholder="+91 22 1234 5678" />
                </div>
                <div className="form-group">
                  <label>Hospital Email</label>
                  <input type="email" value={hospital.hospitalEmail} onChange={setH('hospitalEmail')} placeholder="info@yourhospital.com" />
                </div>
                <div className="form-group">
                  <label>Website</label>
                  <input value={hospital.hospitalWebsite} onChange={setH('hospitalWebsite')} placeholder="https://yourhospital.com" />
                </div>
                <div className="form-group">
                  <label>License Number</label>
                  <input value={hospital.licenseNumber} onChange={setH('licenseNumber')} placeholder="MH-HOSP-2024-XXXXX" />
                </div>
              </div>
              <button className="btn btn-primary auth-submit-btn" onClick={goNext}>
                Next: Admin Account →
              </button>
            </div>
          )}

          {/* ── Step 2: Admin Account ── */}
          {step === 2 && (
            <div className="auth-form">
              <div className="form-grid">
                <div className="form-group">
                  <label>First Name *</label>
                  <input value={admin.firstName} onChange={setA('firstName')} placeholder="First name" autoFocus />
                </div>
                <div className="form-group">
                  <label>Last Name *</label>
                  <input value={admin.lastName} onChange={setA('lastName')} placeholder="Last name" />
                </div>
                <div className="form-group full">
                  <label>Email Address *</label>
                  <input type="email" value={admin.email} onChange={setA('email')} placeholder="you@yourhospital.com" autoComplete="email" />
                </div>
                <div className="form-group">
                  <label>Password *</label>
                  <div className="auth-pass-wrap">
                    <input
                      type={showPass ? 'text' : 'password'}
                      value={admin.password}
                      onChange={setA('password')}
                      placeholder="Min. 8 characters"
                      autoComplete="new-password"
                    />
                    <button type="button" className="auth-pass-toggle"
                      onClick={() => setShowPass(v => !v)} tabIndex={-1}>
                      {showPass ? '🙈' : '👁️'}
                    </button>
                  </div>
                  {passStrength && (
                    <div className="auth-pass-strength">
                      <div className={`auth-pass-bar ${passStrength}`} />
                      <span>{passStrengthLabel[passStrength]}</span>
                    </div>
                  )}
                </div>
                <div className="form-group">
                  <label>Confirm Password *</label>
                  <input
                    type="password"
                    value={admin.confirmPassword}
                    onChange={setA('confirmPassword')}
                    placeholder="Repeat password"
                    autoComplete="new-password"
                    style={{
                      borderColor: admin.confirmPassword && admin.confirmPassword !== admin.password
                        ? '#c0220a' : undefined,
                    }}
                  />
                  {admin.confirmPassword && admin.confirmPassword !== admin.password && (
                    <span style={{ fontSize: 12, color: '#c0220a' }}>⚠ Passwords do not match</span>
                  )}
                  {admin.confirmPassword && admin.confirmPassword === admin.password && (
                    <span style={{ fontSize: 12, color: 'var(--accent)' }}>✓ Passwords match</span>
                  )}
                </div>
              </div>
              <div style={{ display: 'flex', gap: 12 }}>
                <button className="btn btn-secondary" onClick={goBack} style={{ flex: 1 }}>← Back</button>
                <button className="btn btn-primary" onClick={goNext} style={{ flex: 2 }}>
                  Review & Confirm →
                </button>
              </div>
            </div>
          )}

          {/* ── Step 3: Review ── */}
          {step === 3 && (
            <div className="auth-form">
              <div className="auth-review-grid">
                <div className="auth-review-section">
                  <div className="auth-review-title">🏥 Hospital Details</div>
                  {[
                    ['Name',    hospital.hospitalName],
                    ['City',    hospital.hospitalCity],
                    ['State',   hospital.hospitalState || '—'],
                    ['Phone',   hospital.hospitalPhone],
                    ['Email',   hospital.hospitalEmail || '—'],
                    ['License', hospital.licenseNumber || '—'],
                  ].map(([k, v]) => (
                    <div key={k} className="auth-review-row">
                      <span className="auth-review-key">{k}</span>
                      <span className="auth-review-val">{v}</span>
                    </div>
                  ))}
                </div>
                <div className="auth-review-section">
                  <div className="auth-review-title">👤 Admin Account</div>
                  {[
                    ['Name',  `${admin.firstName} ${admin.lastName}`],
                    ['Email', admin.email],
                    ['Role',  'Hospital Admin'],
                  ].map(([k, v]) => (
                    <div key={k} className="auth-review-row">
                      <span className="auth-review-key">{k}</span>
                      <span className="auth-review-val">{v}</span>
                    </div>
                  ))}
                </div>
              </div>

              <p className="auth-terms">
                By registering, you confirm this is a legitimate healthcare facility.
                Your hospital data is completely isolated from other hospitals on this platform.
              </p>

              <div style={{ display: 'flex', gap: 12 }}>
                <button className="btn btn-secondary" onClick={goBack} style={{ flex: 1 }}>← Edit</button>
                <button className="btn btn-primary" onClick={handleRegister} disabled={loading} style={{ flex: 2 }}>
                  {loading ? (
                    <>
                      <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} />
                      Registering...
                    </>
                  ) : '✓ Complete Registration'}
                </button>
              </div>
            </div>
          )}

          <div className="auth-divider"><span>Already registered?</span></div>
          <button className="btn btn-secondary auth-switch-btn" onClick={onSwitchToLogin}>
            Sign in to your hospital
          </button>
        </div>
      </div>
    </div>
  );
};

export default Signup;
