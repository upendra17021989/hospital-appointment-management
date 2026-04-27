import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Icon } from './Common';
import { useAuth } from '../context/AuthContext';
import { useRole } from '../hooks/useRole';


const getNavGroups = (role) => {
  const baseGroups = [
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
  ];

  // Role-based navigation items
  const roleBasedGroups = [];

  if (['HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role)) {
    roleBasedGroups.push({
      key: 'management',
      label: 'Management',
      items: [
        { id: 'doctor-management', label: 'Manage Doctors', icon: 'doctor' },
        { id: 'user-management',   label: 'Manage Users',   icon: 'user' },
        { id: 'departments',       label: 'Departments',    icon: 'department' },
      ],
    });
  }

  if (['STAFF', 'RECEPTIONIST', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role)) {
    roleBasedGroups.push({
      key: 'patients',
      label: 'Patients',
      items: [
        { id: 'patients',     label: 'Patient Records',  icon: 'patient' },
        { id: 'patient-form', label: 'Register Patient', icon: 'patient' },
      ],
    });
  }

  if (['HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role)) {
    roleBasedGroups.push({
      key: 'clinical',
      label: 'Clinical',
      items: [
        { id: 'prescription-form', label: 'Prescriptions', icon: 'prescription' },
      ],
    });
  }

  if (['HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role)) {
    roleBasedGroups.push({
      key: 'billing',
      label: 'Billing & Plans',
      items: [
        { id: 'subscription-plans', label: 'Plans', icon: 'appointment' },
        { id: 'billing-history', label: 'Billing History', icon: 'patient' },
      ],
    });
  }

  roleBasedGroups.push({
    key: 'directory',
    label: 'Directory',
    items: [
      { id: 'doctors',     label: 'Doctors',     icon: 'doctor' },
      { id: 'departments', label: 'Departments', icon: 'department' },
    ],
  });

  roleBasedGroups.push({
    key: 'support',
    label: 'Support',
    items: [
      { id: 'enquiries', label: 'Enquiries', icon: 'enquiry' },
    ],
  });

  return [...baseGroups, ...roleBasedGroups];
};

const Sidebar = ({ isOpen, onClose }) => {
  const location = useLocation();
  const { user, logout } = useAuth();
  const { role } = useRole();

  const navGroups = getNavGroups(role);

  const getPath = (id) => {
    return id === 'book' ? '/book-appointment' : `/${id}`;
  };

  const isActive = (id) => {
    return location.pathname === getPath(id);
  };

  const handleNavClick = () => {
    if (onClose) onClose();
  };


  return (
    <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
      {/* Brand */}
      <div className="sidebar-header">
        <div>
          <div className="sidebar-logo">MediCare+</div>
          <div className="sidebar-tagline">Hospital Management System</div>
        </div>
        <button
          className="sidebar-close"
          onClick={onClose}
          aria-label="Close navigation menu"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      {/* Hospital badge */}
      {user?.hospital && (
        <div style={{ padding: '12px 12px 0' }}>
          <div className="sidebar-hospital">
            <div className="sidebar-hospital-name">🏥 {user.hospital.name}</div>
            {user.hospital.city && (
              <div className="sidebar-hospital-city">{user.hospital.city}</div>
            )}
          </div>
        </div>
      )}

      {/* Navigation - Scrollable */}
      <nav className="sidebar-nav">
        {navGroups.map(group => (
          <div key={group.key} style={{ marginBottom: '12px' }}>
            <div className="nav-section-label">{group.label}</div>
{group.items.map(item => (
              <Link
                key={item.id}
                to={getPath(item.id)}
                className={`nav-item ${isActive(item.id) ? 'active' : ''}`}
                onClick={handleNavClick}
              >
                <span className="nav-icon"><Icon name={item.icon} /></span>
                {item.label}
              </Link>
            ))}

          </div>
        ))}
      </nav>

      {/* Footer — user info + logout - Always visible at bottom */}
      <div className="sidebar-footer">
        {user && (
          <div style={{ padding: '14px 24px 6px', display: 'flex', alignItems: 'center', gap: 10 }}>
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
