import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
import BookAppointment from './pages/BookAppointment';
import Appointments from './pages/Appointments';
import Enquiries from './pages/Enquiries';
import Doctors from './pages/Doctors';
import DoctorManagement from './pages/DoctorManagement';
import Departments from './pages/Departments';

const PAGES = {
  dashboard:           Dashboard,
  book:                BookAppointment,
  appointments:        Appointments,
  enquiries:           Enquiries,
  doctors:             Doctors,
  'doctor-management': DoctorManagement,
  departments:         Departments,
};

// ── Inner app — shown only when authenticated ─────────────────
const AppShell = () => {
  const [page, setPage] = useState('dashboard');
  const ActivePage = PAGES[page] || Dashboard;

  return (
    <div className="layout">
      <Sidebar currentPage={page} onNavigate={setPage} />
      <main className="main-content">
        <ActivePage onNavigate={setPage} />
      </main>
    </div>
  );
};

// ── Auth gate — routes between Login / Signup / App ───────────
const AuthGate = () => {
  const { isAuthenticated, loading } = useAuth();
  const [authScreen, setAuthScreen] = useState('login'); // 'login' | 'signup'

  if (loading) {
    return (
      <div style={{
        minHeight: '100vh', display: 'flex',
        alignItems: 'center', justifyContent: 'center',
        background: 'var(--bg)',
      }}>
        <div style={{ textAlign: 'center' }}>
          <div className="spinner" style={{ width: 32, height: 32, borderWidth: 3, margin: '0 auto 12px' }} />
          <div style={{ color: 'var(--text-muted)', fontSize: 14 }}>Loading...</div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    if (authScreen === 'signup') {
      return (
        <Signup
          onNavigate={() => {}} // handled inside Signup via saveAuth
          onSwitchToLogin={() => setAuthScreen('login')}
        />
      );
    }
    return (
      <Login
        onNavigate={() => {}} // handled inside Login via saveAuth
        onSwitchToSignup={() => setAuthScreen('signup')}
      />
    );
  }

  return <AppShell />;
};

// ── Root ──────────────────────────────────────────────────────
export default function App() {
  return (
    <AuthProvider>
      <AuthGate />
    </AuthProvider>
  );
}
