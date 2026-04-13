import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';


const API = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const Login = () => {
  const { saveAuth } = useAuth();
  const navigate = useNavigate();

  const [form, setForm]         = useState({ email: '', password: '' });
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);
  const [showPass, setShowPass] = useState(false);

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password) { setError('Please fill in all fields.'); return; }
    setLoading(true); setError('');
    try {
      const res  = await fetch(`${API}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      });
      const data = await res.json();
      if (!data.success) { setError(data.message || 'Login failed.'); return; }
      // saveAuth triggers re-render in AuthGate → shows AppShell automatically
      saveAuth(data.data.token, data.data);
      navigate('/dashboard', { replace: true });
    } catch {
      setError('Unable to connect to server. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      {/* Left panel */}
      <div className="auth-left">
        <div className="auth-brand">
          <div className="auth-brand-logo">✚</div>
          <div className="auth-brand-name">MediCare+</div>
        </div>
        <h1 className="auth-hero-title">Manage your hospital<br/>with confidence.</h1>
        <p className="auth-hero-sub">
          Streamline appointments, enquiries, and patient care — all in one place.
          Trusted by hospitals across India.
        </p>
        <div className="auth-features">
          {[
            'Multi-hospital support',
            'Real-time appointment slots',
            'Patient & doctor management',
            'Enquiry tracking',
          ].map(f => (
            <div key={f} className="auth-feature-item">
              <span className="auth-feature-check">✓</span> {f}
            </div>
          ))}
        </div>
      </div>

      {/* Right panel */}
      <div className="auth-right">
        <div className="auth-card">
          <div className="auth-card-header">
            <h2 className="auth-card-title">Welcome back</h2>
            <p className="auth-card-sub">Sign in to your hospital dashboard</p>
          </div>

          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={handleLogin} className="auth-form">
            <div className="form-group">
              <label>Email Address</label>
              <input
                type="email"
                value={form.email}
                onChange={set('email')}
                placeholder="admin@yourhospital.com"
                autoComplete="email"
                autoFocus
              />
            </div>

            <div className="form-group">
              <label>Password</label>
              <div className="auth-pass-wrap">
                <input
                  type={showPass ? 'text' : 'password'}
                  value={form.password}
                  onChange={set('password')}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  className="auth-pass-toggle"
                  onClick={() => setShowPass(v => !v)}
                  tabIndex={-1}
                >
                  {showPass ? '🙈' : '👁️'}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary auth-submit-btn"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} />
                  Signing in...
                </>
              ) : 'Sign In →'}
            </button>
          </form>

          <div className="auth-divider"><span>New to MediCare+?</span></div>

          <button
            className="btn btn-secondary auth-switch-btn"
            onClick={() => navigate('/signup')}
          >
            Register your hospital
          </button>

        </div>
      </div>
    </div>
  );
};

export default Login;
