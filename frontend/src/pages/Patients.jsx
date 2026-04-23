import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useRole } from '../hooks/useRole';
import { LoadingSpinner, EmptyState } from '../components/Common';

// ── Helpers ───────────────────────────────────────────────────
const formatDate = (d) => {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
};

// ── Patient List ──────────────────────────────────────────────────
const Patients = () => {
  const navigate = useNavigate();
  const [patients,  setPatients]  = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [search,    setSearch]    = useState('');
  const [filtered,  setFiltered]  = useState([]);
  const { isStaff, isReceptionist } = useRole();

  useEffect(() => {
    api.get('/patients/hospital')
      .then(data => { 
        setPatients(data || []); 
        setFiltered(data || []); 
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!search.trim()) { 
      setFiltered(patients); 
      return; 
    }
    const q = search.toLowerCase();
    setFiltered(patients.filter(p =>
      p.fullName?.toLowerCase().includes(q) ||
      p.phone?.includes(search) ||
      p.email?.toLowerCase().includes(q) ||
      p.bloodGroup?.toLowerCase().includes(q)
    ));
  }, [search, patients]);

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Patients</h1>
          <p className="page-subtitle">{patients.length} registered patients</p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/patient-form')}>
          + Register Patient
        </button>
      </div>

      {/* Search */}
      <div className="search-bar" style={{ marginBottom: 20 }}>
        <div className="search-input-wrap" style={{ flex: 1 }}>
          <span className="search-icon">🔍</span>
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by name, phone, email or blood group..."
          />
        </div>
        {search && (
          <button className="btn btn-secondary btn-sm" onClick={() => setSearch('')}>
            Clear
          </button>
        )}
      </div>

      {filtered.length === 0 ? (
        <EmptyState icon="👥" title="No patients found" subtitle="Register a new patient to get started" />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Patient</th>
                <th>Phone</th>
                <th>Gender</th>
                <th>Age</th>
                <th>Blood Group</th>
                <th>DOB</th>
                <th>Registered</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(p => (
                <tr key={p.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div className="pd-avatar">
                        {p.firstName?.[0]}{p.lastName?.[0]}
                      </div>
                      <div>
                        <div style={{ fontWeight: 700 }}>{p.fullName}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{p.email || '—'}</div>
                      </div>
                    </div>
                  </td>
                  <td>{p.phone}</td>
                  <td style={{ textTransform: 'capitalize' }}>
                    {p.gender} {p.age || calcAge(p.dateOfBirth) || '—'} yrs
                  </td>
                  <td>
                    {p.bloodGroup
                      ? <span className="pd-blood-badge">{p.bloodGroup}</span>
                      : '—'}
                  </td>
                  <td>{p.dateOfBirth ? formatDate(p.dateOfBirth) : '—'}</td>
                  <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>{formatDate(p.createdAt)}</td>
                  <td>
                    {isStaff() || isReceptionist() ? (
                      <button
                        className="btn btn-secondary btn-sm"
                        disabled
                        title="View patient details available for Hospital Admin and Super Admin only"
                      >
                        🔒 Admin Only
                      </button>
                    ) : (
                      <a
                        href={`/patients/${p.id}`}
                        className="btn btn-primary btn-sm"
                      >
                        👁 View
                      </a>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

const calcAge = (dob) => {
  if (!dob) return null;
  const diff = Date.now() - new Date(dob).getTime();
  return Math.floor(diff / (1000 * 60 * 60 * 24 * 365.25));
};

export default Patients;

