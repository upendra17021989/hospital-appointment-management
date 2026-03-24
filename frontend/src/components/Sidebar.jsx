import React from 'react';
import { Icon } from './Common';
import { useAuth } from '../context/AuthContext';

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
      { id: 'book',         label: 'Book Appointment', icon: 'appointment' },
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
      { id: 'doctors',           label: 'Doctors',        icon: 'doctor' },
      { id: 'doctor-management', label: 'Manage Doctors', icon: 'doctor' },
      { id: 'departments',       label: 'Departments',    icon: 'department' },
    ],
  },
];

const Sidebar = ({ currentPage, onNavigate }) => {
  const { user, logout } = useAuth();

  return (
    <aside className="sidebar">
      {/* Brand */}
      <div className="sidebar-header">
        <div className="sidebar-logo">MediCare+</div>
        <div className="sidebar-tagline">Hospital Management System</div>
      </div>

      {/* Hospital badge */}
      {user?.hospital && (
        <div style={{ padding: '12px 12px 0' }}>
          <div className="sidebar-hospital">
            <div className="sidebar-hospital-name">
              🏥 {user.hospital.name}
            </div>
            {user.hospital.city && (
              <div className="sidebar-hospital-city">{user.hospital.city}</div>
            )}
          </div>
        </div>
      )}

      {/* Navigation */}
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
                <span className="nav-icon"><Icon name={item.icon} /></span>
                {item.label}
              </button>
            ))}
          </div>
        ))}
      </nav>

      {/* Footer — user info + logout */}
      <div style={{ borderTop: '1px solid rgba(255,255,255,0.08)' }}>
        {user && (
          <div style={{
            padding: '14px 24px 6px',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
          }}>
            <div style={{
              width: 32, height: 32, borderRadius: '50%',
              background: 'linear-gradient(135deg, var(--primary), var(--gold))',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 12, fontWeight: 800, color: 'white', flexShrink: 0,
            }}>
              {user.fullName?.[0] || user.email?.[0]?.toUpperCase()}
            </div>
            <div style={{ overflow: 'hidden' }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: '#f0ebe3', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user.fullName}
              </div>
              <div style={{ fontSize: 11, color: 'rgba(240,235,227,0.4)', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                {user.role?.replace('_', ' ')}
              </div>
            </div>
          </div>
        )}
        <button className="sidebar-logout" onClick={logout}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          Sign Out
        </button>
        <div style={{ padding: '8px 24px 16px', fontSize: 11, color: 'rgba(240,235,227,0.2)' }}>
          © 2026 MediCare+
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
