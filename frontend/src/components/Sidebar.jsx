import React from 'react';
import { Icon } from './Common';

const NAV_GROUPS = [
  {
    key: 'main',
    label: 'Overview',
    items: [
      { id: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
    ],
  },
  {
    key: 'appointments',
    label: 'Appointments',
    items: [
      { id: 'book', label: 'Book Appointment', icon: 'appointment' },
      { id: 'appointments', label: 'All Appointments', icon: 'appointment' },
    ],
  },
  {
    key: 'support',
    label: 'Support',
    items: [
      { id: 'enquiries', label: 'Enquiries', icon: 'enquiry' },
    ],
  },
  {
    key: 'directory',
    label: 'Directory',
    items: [
      { id: 'doctors',           label: 'Doctors',           icon: 'doctor' },
      { id: 'doctor-management', label: 'Manage Doctors',    icon: 'doctor' },
      { id: 'departments',       label: 'Departments',       icon: 'department' },
    ],
  },
];

const Sidebar = ({ currentPage, onNavigate }) => {
  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-logo">MediCare+</div>
        <div className="sidebar-tagline">Hospital Management System</div>
      </div>

      <nav className="sidebar-nav">
        {NAV_GROUPS.map(group => (
          <div key={group.key}>
            <div className="nav-section-label">{group.label}</div>
            {group.items.map(item => (
              <button
                key={item.id}
                className={`nav-item ${currentPage === item.id ? 'active' : ''}`}
                onClick={() => onNavigate(item.id)}
              >
                <span className="nav-icon">
                  <Icon name={item.icon} />
                </span>
                {item.label}
              </button>
            ))}
          </div>
        ))}
      </nav>

      <div style={{
        padding: '16px 24px',
        borderTop: '1px solid rgba(255,255,255,0.08)',
        fontSize: 12,
        color: 'rgba(240,235,227,0.3)',
      }}>
        © 2026 MediCare+ Hospital
      </div>
    </aside>
  );
};

export default Sidebar;
