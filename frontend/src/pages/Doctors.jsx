import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { LoadingSpinner, EmptyState, Icon } from '../components/Common';

const DoctorCard = ({ doctor }) => (
  <div className="doctor-card">
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 14 }}>
      <div className="doctor-avatar">
        {doctor.firstName?.[0]}{doctor.lastName?.[0]}
      </div>
      <div>
        <div className="doctor-name">Dr. {doctor.firstName} {doctor.lastName}</div>
        <div className="doctor-spec">{doctor.specialization}</div>
      </div>
    </div>

    <div className="doctor-dept">{doctor.department?.name}</div>

    <div style={{ marginTop: 8, display: 'flex', gap: 16 }}>
      <div>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.8 }}>Experience</div>
        <div style={{ fontWeight: 700, fontSize: 14 }}>{doctor.experienceYears} Years</div>
      </div>
      <div>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.8 }}>Consultation</div>
        <div style={{ fontWeight: 700, fontSize: 14, color: 'var(--primary)' }}>₹{doctor.consultationFee?.toFixed(0)}</div>
      </div>
    </div>

    {doctor.qualification && (
      <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 10 }}>{doctor.qualification}</div>
    )}

    {doctor.languagesSpoken?.length > 0 && (
      <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
        {doctor.languagesSpoken.map(l => (
          <span key={l} style={{ fontSize: 11, background: 'var(--bg)', padding: '2px 8px', borderRadius: 6, border: '1px solid var(--border)' }}>
            {l}
          </span>
        ))}
      </div>
    )}
  </div>
);

const Doctors = () => {
  const [doctors, setDoctors] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [deptFilter, setDeptFilter] = useState('');

  useEffect(() => {
    Promise.all([
      api.get('/doctors').catch(() => []),
      api.get('/departments').catch(() => []),
    ]).then(([d, dept]) => {
      setDoctors(d || []);
      setDepartments(dept || []);
      setLoading(false);
    });
  }, []);

  const filtered = doctors.filter(d => {
    const matchSearch = !search
      || d.fullName?.toLowerCase().includes(search.toLowerCase())
      || d.specialization?.toLowerCase().includes(search.toLowerCase());
    const matchDept = !deptFilter || d.department?.id === deptFilter;
    return matchSearch && matchDept;
  });

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Our Doctors</h1>
          <p className="page-subtitle">{doctors.length} specialists available</p>
        </div>
      </div>

      <div className="search-bar">
        <div className="search-input-wrap">
          <span className="search-icon"><Icon name="search" /></span>
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by name or specialization..."
          />
        </div>
        <select
          value={deptFilter}
          onChange={e => setDeptFilter(e.target.value)}
          style={{ padding: '10px 14px', borderRadius: 10, border: '1.5px solid var(--border)', minWidth: 180, background: 'white' }}
        >
          <option value="">All Departments</option>
          {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
        </select>
      </div>

      {loading ? <LoadingSpinner /> : filtered.length === 0 ? (
        <EmptyState icon="👨‍⚕️" title="No doctors found" subtitle="Try a different search or department" />
      ) : (
        <div className="doctor-grid">
          {filtered.map(d => <DoctorCard key={d.id} doctor={d} />)}
        </div>
      )}
    </div>
  );
};

export default Doctors;
