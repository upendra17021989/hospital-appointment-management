import React, { useState, useEffect, useCallback } from 'react';
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

const calcAge = (dob) => {
  if (!dob) return null;
  const diff = Date.now() - new Date(dob).getTime();
  return Math.floor(diff / (1000 * 60 * 60 * 24 * 365.25));
};

// ── Patient List ──────────────────────────────────────────────────
const Patients = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  
  // Pagination state
  const [patients, setPatients] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // Sorting state
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('DESC');
  
  const { isStaff, isReceptionist } = useRole();

  const fetchPatients = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size, sortBy, sortDirection };
      const response = await api.get('/patients/hospital/paged', { params });
      const pagedData = response;
      if (pagedData) {
        setPatients(pagedData.content || []);
        setTotalPages(pagedData.totalPages || 0);
        setTotalElements(pagedData.totalElements || 0);
      } else {
        setPatients([]);
      }
    } catch (error) {
      console.error('Error fetching patients:', error);
      setPatients([]);
    } finally {
      setLoading(false);
    }
  }, [page, size, sortBy, sortDirection]);

  useEffect(() => {
    fetchPatients();
  }, [fetchPatients]);

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setPage(newPage);
    }
  };

  const handleSizeChange = (newSize) => {
    setSize(newSize);
    setPage(0); // Reset to first page
  };

  const handleSort = (field) => {
    if (sortBy === field) {
      // Toggle direction if same field
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC');
    } else {
      setSortBy(field);
      setSortDirection('DESC');
    }
    setPage(0); // Reset to first page
  };

  const getSortIcon = (field) => {
    if (sortBy !== field) return '⇅';
    return sortDirection === 'ASC' ? '↑' : '↓';
  };

  // Filter patients locally by search (for client-side filtering within current page)
  const filtered = search.trim() 
    ? patients.filter(p =>
        p.fullName?.toLowerCase().includes(search.toLowerCase()) ||
        p.phone?.includes(search) ||
        p.email?.toLowerCase().includes(search.toLowerCase()) ||
        p.bloodGroup?.toLowerCase().includes(search.toLowerCase())
      )
    : patients;

  if (loading && page === 0) return <LoadingSpinner />;

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Patients</h1>
          <p className="page-subtitle">{totalElements} registered patients</p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/patient-form')}>
          + Register Patient
        </button>
      </div>

      {/* Search & Controls */}
      <div className="search-bar" style={{ marginBottom: 20, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <div className="search-input-wrap" style={{ flex: 1, minWidth: 200 }}>
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

      {/* Table Controls */}
      <div style={{ marginBottom: 15, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <label style={{ fontSize: 14, color: 'var(--text-muted)' }}>Show:</label>
          <select 
            value={size} 
            onChange={e => handleSizeChange(Number(e.target.value))}
            style={{ padding: '6px 10px', borderRadius: 4, border: '1px solid var(--border)', fontSize: 14 }}
          >
            <option value={5}>5</option>
            <option value={10}>10</option>
            <option value={15}>15</option>
            <option value={100}>100</option>
          </select>
          <span style={{ fontSize: 14, color: 'var(--text-muted)' }}>records</span>
        </div>
      </div>

      {filtered.length === 0 && !loading ? (
        <EmptyState icon="👥" title="No patients found" subtitle="Register a new patient to get started" />
      ) : (
        <div className="table-wrap">
          {/* Desktop Table */}
          <table className="patients-table-desktop">
            <thead>
              <tr>
                <th onClick={() => handleSort('firstName')} style={{ cursor: 'pointer' }}>
                  Patient {getSortIcon('firstName')}
                </th>
                <th onClick={() => handleSort('phone')} style={{ cursor: 'pointer' }}>
                  Phone {getSortIcon('phone')}
                </th>
                <th onClick={() => handleSort('gender')} style={{ cursor: 'pointer' }}>
                  Gender {getSortIcon('gender')}
                </th>
                <th onClick={() => handleSort('age')} style={{ cursor: 'pointer' }}>
                  Age {getSortIcon('age')}
                </th>
                <th onClick={() => handleSort('bloodGroup')} style={{ cursor: 'pointer' }}>
                  Blood Group {getSortIcon('bloodGroup')}
                </th>
                <th onClick={() => handleSort('createdAt')} style={{ cursor: 'pointer' }}>
                  Registered {getSortIcon('createdAt')}
                </th>
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
                    {p.gender}
                  </td>
                  <td>{p.age || calcAge(p.dateOfBirth) || '—'} yrs</td>
                  <td>
                    {p.bloodGroup
                      ? <span className="pd-blood-badge">{p.bloodGroup}</span>
                      : '—'}
                  </td>
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

          {/* Mobile Cards */}
          <div className="patients-mobile">
            {filtered.map(p => (
              <div className="patient-card" key={p.id}>
                <div className="patient-card-header">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div className="pd-avatar">
                      {p.firstName?.[0]}{p.lastName?.[0]}
                    </div>
                    <div>
                      <div style={{ fontWeight: 700 }}>{p.fullName}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{p.email || '—'}</div>
                    </div>
                  </div>
                  {p.bloodGroup && (
                    <span className="pd-blood-badge">{p.bloodGroup}</span>
                  )}
                </div>
                <div className="patient-card-body">
                  <div className="patient-card-row">
                    <span className="patient-card-label">Phone</span>
                    <span className="patient-card-value">{p.phone}</span>
                  </div>
                  <div className="patient-card-row">
                    <span className="patient-card-label">Gender</span>
                    <span className="patient-card-value" style={{ textTransform: 'capitalize' }}>{p.gender}</span>
                  </div>
                  <div className="patient-card-row">
                    <span className="patient-card-label">Age</span>
                    <span className="patient-card-value">{p.age || calcAge(p.dateOfBirth) || '—'} yrs</span>
                  </div>
                  <div className="patient-card-row">
                    <span className="patient-card-label">Date of Birth</span>
                    <span className="patient-card-value">{p.dateOfBirth ? formatDate(p.dateOfBirth) : '—'}</span>
                  </div>
                  <div className="patient-card-row">
                    <span className="patient-card-label">Registered</span>
                    <span className="patient-card-value">{formatDate(p.createdAt)}</span>
                  </div>
                </div>
                <div className="patient-card-actions">
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
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div style={{ marginTop: 20, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <button 
            className="btn btn-secondary btn-sm"
            onClick={() => handlePageChange(0)}
            disabled={page === 0}
            title="First page"
          >
            ⏮
          </button>
          <button 
            className="btn btn-secondary btn-sm"
            onClick={() => handlePageChange(page - 1)}
            disabled={page === 0}
            title="Previous page"
          >
            ◀ Prev
          </button>
          
          <span style={{ padding: '0 15px', fontSize: 14 }}>
            Page {page + 1} of {totalPages}
            {totalElements > 0 && ` (${totalElements} records)`}
          </span>
          
          <button 
            className="btn btn-secondary btn-sm"
            onClick={() => handlePageChange(page + 1)}
            disabled={page >= totalPages - 1}
            title="Next page"
          >
            Next ▶
          </button>
          <button 
            className="btn btn-secondary btn-sm"
            onClick={() => handlePageChange(totalPages - 1)}
            disabled={page >= totalPages - 1}
            title="Last page"
          >
            ⏭
          </button>
        </div>
      )}
    </div>
  );
};

export default Patients;
