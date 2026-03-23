import React, { useState } from 'react';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import BookAppointment from './pages/BookAppointment';
import Appointments from './pages/Appointments';
import Enquiries from './pages/Enquiries';
import Doctors from './pages/Doctors';
import DoctorManagement from './pages/DoctorManagement';
import Departments from './pages/Departments';

const PAGES = {
  dashboard:          Dashboard,
  book:               BookAppointment,
  appointments:       Appointments,
  enquiries:          Enquiries,
  doctors:            Doctors,
  'doctor-management': DoctorManagement,
  departments:        Departments,
};

export default function App() {
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
}
