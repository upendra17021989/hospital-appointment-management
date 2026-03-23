import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { LoadingSpinner } from '../components/Common';

const DEPT_ICONS = ['❤️', '🦴', '🧠', '👶', '🌸', '🔬', '👁️', '👃', '💊', '🎗️'];

const DepartmentCard = ({ department, icon }) => (
  <div
    className="card"
    style={{ border: '1px solid var(--border)', transition: 'all 0.2s', cursor: 'default' }}
    onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.boxShadow = 'var(--shadow-lg)'; }}
    onMouseLeave={e => { e.currentTarget.style.transform = ''; e.currentTarget.style.boxShadow = ''; }}
  >
    <div style={{ fontSize: 36, marginBottom: 12 }}>{icon}</div>
    <div className="card-title">{department.name}</div>
    <p style={{ color: 'var(--text-muted)', fontSize: 13, marginBottom: 14 }}>{department.description}</p>

    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 12 }}>
      <div style={{ background: 'var(--bg)', padding: '8px 10px', borderRadius: 8 }}>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.8 }}>Floor</div>
        <div style={{ fontWeight: 700 }}>{department.floorNumber}</div>
      </div>
      <div style={{ background: 'var(--bg)', padding: '8px 10px', borderRadius: 8 }}>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.8 }}>Doctors</div>
        <div style={{ fontWeight: 700 }}>{department.doctorCount}</div>
      </div>
    </div>

    {department.phone && (
      <div style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 4 }}>📞 {department.phone}</div>
    )}
    {department.email && (
      <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>✉️ {department.email}</div>
    )}
  </div>
);

const Departments = () => {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/departments')
      .then(d => { setDepartments(d || []); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Departments</h1>
          <p className="page-subtitle">Browse our {departments.length} medical departments</p>
        </div>
      </div>

      {loading ? <LoadingSpinner /> : (
        <div className="grid-3">
          {departments.map((d, i) => (
            <DepartmentCard
              key={d.id}
              department={d}
              icon={DEPT_ICONS[i % DEPT_ICONS.length]}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default Departments;
