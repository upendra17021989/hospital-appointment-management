import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation, Outlet } from 'react-router-dom';

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

const Login  = React.lazy(() => import('./pages/Login'));
const Signup = React.lazy(() => import('./pages/Signup'));

const RequireAuth = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  return isAuthenticated ? children : <Navigate to="/login" state={{ from: location }} replace />;
};

const ProtectedRoute = ({ component: Component, roles, children }) => {
  if (Component) {
    return (
      <RequireAuth>
        <RoleGuard roles={roles}>
          <Component />
        </RoleGuard>
      </RequireAuth>
    );
  }
  return null;
};

const ROUTE_CONFIG = [

  { path: '/login', component: Login, roles: [], authRequired: false },
  { path: '/signup', component: Signup, roles: [], authRequired: false },
  { path: '/', redirectTo: '/dashboard' },
  { path: '/dashboard', component: Dashboard, roles: [], authRequired: true },
  { path: '/book-appointment', component: BookAppointment, roles: [], authRequired: true },
  { path: '/appointments', component: Appointments, roles: [], authRequired: true },
  { path: '/enquiries', component: Enquiries, roles: [], authRequired: true },
  { path: '/doctors', component: Doctors, roles: [], authRequired: true },
  { path: '/doctor-management', component: DoctorManagement, roles: ['HOSPITAL_ADMIN', 'SUPER_ADMIN'], authRequired: true },
  { path: '/user-management', component: UserManagement, roles: ['HOSPITAL_ADMIN', 'SUPER_ADMIN'], authRequired: true },
  { path: '/departments', component: Departments, roles: [], authRequired: true },
  { path: '/patient-form', component: PatientForm, roles: ['STAFF', 'RECEPTIONIST', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'], authRequired: true },
  { path: '/patients', component: PatientDetails, roles: ['STAFF', 'RECEPTIONIST', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'], authRequired: true },
  { path: '/prescription-form', component: PrescriptionForm, roles: ['HOSPITAL_ADMIN', 'SUPER_ADMIN'], authRequired: true },
];


const Layout = () => {
  return (
    <div className="layout">
      <Sidebar />
      <main className="main-content">
        <React.Suspense fallback={<div style={{padding: '2rem', textAlign: 'center'}}><div className="spinner" style={{margin: '0 auto 1rem', width: 40, height: 40}} />Loading page...</div>}>
          <Outlet />
        </React.Suspense>
      </main>
    </div>
  );
};


const AppRoutes = () => {
  const { loading } = useAuth();

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

  return (
    <Routes>
      <Route path="/login" element={
        <React.Suspense fallback={null}>
          <Login />
        </React.Suspense>
      } />
      <Route path="/signup" element={
        <React.Suspense fallback={null}>
          <Signup />
        </React.Suspense>
      } />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/" element={<Layout />}>
        {ROUTE_CONFIG.filter(r => r.authRequired).map(({ path, component, roles }) => (
          path !== '/' && (
            <Route
              key={path}
              path={path}
              element={<ProtectedRoute component={component} roles={roles} />}
            />
          )
        ))}
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};


export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}

