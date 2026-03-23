import React from 'react';

// ── Icon ─────────────────────────────────────────────────────
export const Icon = ({ name, size = 18 }) => {
  const icons = {
    dashboard: (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
        <rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
      </svg>
    ),
    appointment: (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <rect x="3" y="4" width="18" height="18" rx="2"/>
        <line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/>
        <line x1="3" y1="10" x2="21" y2="10"/><polyline points="8 14 10 16 16 11"/>
      </svg>
    ),
    enquiry: (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
    ),
    doctor: (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
        <circle cx="12" cy="7" r="4"/><polyline points="8 17 12 21 16 17"/>
      </svg>
    ),
    patient: (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
        <circle cx="12" cy="7" r="4"/>
      </svg>
    ),
    department: (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
        <polyline points="9 22 9 12 15 12 15 22"/>
      </svg>
    ),
    search: (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
      </svg>
    ),
    plus: (
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
      </svg>
    ),
  };
  return icons[name] || null;
};

// ── Badge ─────────────────────────────────────────────────────
export const Badge = ({ status }) => (
  <span className={`badge badge-${status}`}>
    {status?.replace(/_/g, ' ')}
  </span>
);

// ── LoadingSpinner ────────────────────────────────────────────
export const LoadingSpinner = () => (
  <div className="loading">
    <div className="spinner" />
    <span>Loading...</span>
  </div>
);

// ── EmptyState ────────────────────────────────────────────────
export const EmptyState = ({ icon, title, subtitle }) => (
  <div className="empty-state">
    <div className="empty-state-icon">{icon}</div>
    <div className="empty-state-title">{title}</div>
    {subtitle && <div style={{ fontSize: 13 }}>{subtitle}</div>}
  </div>
);

// ── Modal ─────────────────────────────────────────────────────
export const Modal = ({ title, onClose, children }) => (
  <div className="modal-overlay" onClick={onClose}>
    <div className="modal" onClick={e => e.stopPropagation()}>
      <div className="modal-header">
        <div className="modal-title">{title}</div>
        <button className="modal-close" onClick={onClose}>×</button>
      </div>
      {children}
    </div>
  </div>
);

// ── PageHeader ────────────────────────────────────────────────
export const PageHeader = ({ title, subtitle, children }) => (
  <div className="page-header">
    <div>
      <h1 className="page-title">{title}</h1>
      {subtitle && <p className="page-subtitle">{subtitle}</p>}
    </div>
    {children}
  </div>
);

// ── Tabs ──────────────────────────────────────────────────────
export const Tabs = ({ tabs, active, onChange }) => (
  <div className="tabs">
    {tabs.map(tab => (
      <button
        key={tab.value}
        className={`tab ${active === tab.value ? 'active' : ''}`}
        onClick={() => onChange(tab.value)}
      >
        {tab.label}
        {tab.count !== undefined && (
          <span style={{ marginLeft: 6, fontSize: 11, opacity: 0.8 }}>({tab.count})</span>
        )}
      </button>
    ))}
  </div>
);
