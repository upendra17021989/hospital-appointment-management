import React, { useState, useEffect, useCallback } from 'react';
import api from '../services/api';
import { LoadingSpinner, Badge, EmptyState, Tabs } from '../components/Common';

const Appointments = () => {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Filters state
  const [statusFilter, setStatusFilter] = useState('all');
  const [dateFilter, setDateFilter] = useState('');
  
  // Pagination state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // Sorting state
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('DESC');
  
  const [updating, setUpdating] = useState(null);

  const fetchAppointments = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size, sortBy, sortDirection, date: dateFilter, status: statusFilter };
      const response = await api.get('/appointments/hospital/paged', { params });
      const pagedData = response;
      if (pagedData) {
        setAppointments(pagedData.content || []);
        setTotalPages(pagedData.totalPages || 0);
        setTotalElements(pagedData.totalElements || 0);
      } else {
        setAppointments([]);
      }
    } catch (error) {
      console.error('Error fetching appointments:', error);
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  }, [page, size, sortBy, sortDirection, dateFilter, statusFilter]);

  useEffect(() => {
    fetchAppointments();
  }, [fetchAppointments]);

  const updateStatus = async (id, status) => {
    setUpdating(id);
    try {
      await api.patch(`/appointments/hospital/${id}/status`, { status });
      fetchAppointments();
    } finally {
      setUpdating(null);
    }
  };

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

  const countByStatus = (s) => {
    // Note: In a real implementation, we'd get counts from the backend
    // For now, we'll show the current page filtered count
    return appointments.filter(a => a.status === s).length;
  };

  const tabs = [
    { value: 'all', label: 'All', count: totalElements },
    { value: 'pending', label: 'Pending', count: countByStatus('pending') },
    { value: 'confirmed', label: 'Confirmed', count: countByStatus('confirmed') },
    { value: 'completed', label: 'Completed', count: countByStatus('completed') },
    { value: 'cancelled', label: 'Cancelled', count: countByStatus('cancelled') },
  ];

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Appointments</h1>
          <p className="page-subtitle">{totalElements} total appointments</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <input
            type="date"
            value={dateFilter}
            onChange={e => {
              setDateFilter(e.target.value);
              setPage(0);
            }}
            style={{ padding: '9px 12px', borderRadius: 10, border: '1.5px solid var(--border)', background: 'white', fontFamily: 'DM Sans' }}
          />
          {dateFilter && (
            <button className="btn btn-secondary btn-sm" onClick={() => setDateFilter('')}>Clear</button>
          )}
        </div>
      </div>

      <Tabs tabs={tabs} active={statusFilter} onChange={(val) => { setStatusFilter(val); setPage(0); }} />

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

      <div className="table-wrap">
        {loading ? <LoadingSpinner /> : appointments.length === 0 ? (
          <EmptyState icon="📅" title="No appointments found" />
        ) : (
          <>
            {/* Desktop Table */}
            <table className="appointments-table-desktop">
              <thead>
                <tr>
                  <th onClick={() => handleSort('tokenNumber')} style={{ cursor: 'pointer' }}>
                    Token {getSortIcon('tokenNumber')}
                  </th>
                  <th onClick={() => handleSort('patient')} style={{ cursor: 'pointer' }}>
                    Patient {getSortIcon('patient')}
                  </th>
                  <th onClick={() => handleSort('doctor')} style={{ cursor: 'pointer' }}>
                    Doctor {getSortIcon('doctor')}
                  </th>
                  <th onClick={() => handleSort('department')} style={{ cursor: 'pointer' }}>
                    Department {getSortIcon('department')}
                  </th>
                  <th onClick={() => handleSort('appointmentDate')} style={{ cursor: 'pointer' }}>
                    Date {getSortIcon('appointmentDate')}
                  </th>
                  <th onClick={() => handleSort('appointmentTime')} style={{ cursor: 'pointer' }}>
                    Time {getSortIcon('appointmentTime')}
                  </th>
                  <th onClick={() => handleSort('appointmentType')} style={{ cursor: 'pointer' }}>
                    Type {getSortIcon('appointmentType')}
                  </th>
                  <th onClick={() => handleSort('status')} style={{ cursor: 'pointer' }}>
                    Status {getSortIcon('status')}
                  </th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {appointments.map(a => (
                  <tr key={a.id}>
                    <td><span className="token-badge">{a.tokenNumber}</span></td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{a.patient?.fullName}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{a.patient?.phone}</div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{a.doctor?.fullName}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{a.doctor?.specialization}</div>
                    </td>
                    <td>{a.department?.name}</td>
                    <td>{a.appointmentDate}</td>
                    <td>{a.appointmentTime}</td>
                    <td><Badge status={a.appointmentType} /></td>
                    <td><Badge status={a.status} /></td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        {a.status === 'pending' && (
                          <button className="btn btn-accent btn-sm" disabled={updating === a.id}
                            onClick={() => updateStatus(a.id, 'confirmed')}>Confirm</button>
                        )}
                        {a.status === 'confirmed' && (
                          <button className="btn btn-secondary btn-sm" disabled={updating === a.id}
                            onClick={() => updateStatus(a.id, 'completed')}>Complete</button>
                        )}
                        {(a.status === 'pending' || a.status === 'confirmed') && (
                          <button className="btn btn-danger btn-sm" disabled={updating === a.id}
                            onClick={() => updateStatus(a.id, 'cancelled')}>Cancel</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Mobile Cards */}
            <div className="appointments-mobile">
              {appointments.map(a => (
                <div className="appointment-card" key={a.id}>
                  <div className="appointment-card-header">
                    <span className="token-badge">{a.tokenNumber}</span>
                    <Badge status={a.status} />
                  </div>
                  <div className="appointment-card-body">
                    <div className="appointment-card-row">
                      <span className="appointment-card-label">Patient</span>
                      <span className="appointment-card-value">{a.patient?.fullName}</span>
                    </div>
                    <div className="appointment-card-row">
                      <span className="appointment-card-label">Phone</span>
                      <span className="appointment-card-value">{a.patient?.phone}</span>
                    </div>
                    <div className="appointment-card-row">
                      <span className="appointment-card-label">Doctor</span>
                      <span className="appointment-card-value">{a.doctor?.fullName}</span>
                    </div>
                    <div className="appointment-card-row">
                      <span className="appointment-card-label">Department</span>
                      <span className="appointment-card-value">{a.department?.name}</span>
                    </div>
                    <div className="appointment-card-row">
                      <span className="appointment-card-label">Date & Time</span>
                      <span className="appointment-card-value">{a.appointmentDate} at {a.appointmentTime}</span>
                    </div>
                    <div className="appointment-card-row">
                      <span className="appointment-card-label">Type</span>
                      <Badge status={a.appointmentType} />
                    </div>
                  </div>
                  <div className="appointment-card-actions">
                    {a.status === 'pending' && (
                      <button className="btn btn-accent btn-sm" disabled={updating === a.id}
                        onClick={() => updateStatus(a.id, 'confirmed')}>Confirm</button>
                    )}
                    {a.status === 'confirmed' && (
                      <button className="btn btn-secondary btn-sm" disabled={updating === a.id}
                        onClick={() => updateStatus(a.id, 'completed')}>Complete</button>
                    )}
                    {(a.status === 'pending' || a.status === 'confirmed') && (
                      <button className="btn btn-danger btn-sm" disabled={updating === a.id}
                        onClick={() => updateStatus(a.id, 'cancelled')}>Cancel</button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>

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

export default Appointments;
