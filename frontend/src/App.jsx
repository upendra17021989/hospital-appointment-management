import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';
import RoleGuard from './components/RoleGuard';
import Dashboard        from './pages/Dashboard';
import BookAppointment  from './pages/BookAppointment';
import Appointments     from './pages/Appointments';
import Enquiries        from './pages/Enquiries';
import Doctors          from './pages/Doctors';
import DoctorManagement from './pages/DoctorManagement';
import Departments      from './pages/Departments';
import PatientForm      from './pages/PatientForm';
import PatientDetails   from './pages/PatientDetails';
import PrescriptionForm from './pages/PrescriptionForm';
import UserManagement   from './pages/UserManagement';

const PAGES = {
  dashboard:           Dashboard,
  book:                BookAppointment,
  appointments:        Appointments,
  enquiries:           Enquiries,
  doctors:             Doctors,
  'doctor-management': DoctorManagement,
  'user-management':   UserManagement,
  departments:         Departments,
  'patient-form':      PatientForm,
  patients:            PatientDetails,
  'prescription-form': PrescriptionForm,
};

const getPageRoles = (pageId) => {
  const roleMap = {
    'doctor-management': ['HOSPITAL_ADMIN', 'SUPER_ADMIN'],
    'user-management':   ['HOSPITAL_ADMIN', 'SUPER_ADMIN'],
    'patient-form':      ['STAFF', 'RECEPTIONIST', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'],
    'patients':          ['STAFF', 'RECEPTIONIST', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'],
    'prescription-form': ['HOSPITAL_ADMIN', 'SUPER_ADMIN'],
  };
  return roleMap[pageId] || []; // Empty array means no role restrictions
};

const AppShell = () => {
  const [page,      setPage]      = useState('dashboard');
  const [pageProps, setPageProps] = useState({});

  const handleNavigate = (newPage, props = {}) => {
    setPage(newPage);
    setPageProps(props);
  };

  const ActivePage = PAGES[page] || Dashboard;
  const requiredRoles = getPageRoles(page);

  return (
    <div className="layout">
      <Sidebar currentPage={page} onNavigate={handleNavigate} />
      <main className="main-content">
        <RoleGuard roles={requiredRoles}>
          <ActivePage onNavigate={handleNavigate} {...pageProps} />
        </RoleGuard>
      </main>
    </div>
  );
};

const AuthGate = () => {
  const { isAuthenticated, loading } = useAuth();
  const [authScreen, setAuthScreen] = useState('login');

  if (loading) {
    return (
      <div style={{ minHeight:'100vh', display:'flex', alignItems:'center', justifyContent:'center', background:'var(--bg)' }}>
        <div style={{ textAlign:'center' }}>
          <div className="spinner" style={{ width:32, height:32, borderWidth:3, margin:'0 auto 12px' }} />
          <div style={{ color:'var(--text-muted)', fontSize:14 }}>Loading...</div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    const Login  = React.lazy(() => import('./pages/Login'));
    const Signup = React.lazy(() => import('./pages/Signup'));
    return (
      <React.Suspense fallback={null}>
        {authScreen === 'signup'
          ? <Signup onSwitchToLogin={() => setAuthScreen('login')} />
          : <Login  onSwitchToSignup={() => setAuthScreen('signup')} />}
      </React.Suspense>
    );
  }

  return <AppShell />;
};

export default function App() {
  return (
    <AuthProvider>
      <AuthGate />
    </AuthProvider>
  );
}
