import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { LoadingSpinner, Badge, EmptyState, Tabs } from '../components/Common';

const Appointments = () => {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('all');
  const [dateFilter, setDateFilter] = useState('');
  const [updating, setUpdating] = useState(null);

  const fetchAppointments = () => {
    setLoading(true);
    const url = dateFilter ? `/appointments?date=${dateFilter}` : '/appointments';
    api.get(url)
      .then(data => { setAppointments(data || []); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(() => { fetchAppointments(); }, [dateFilter]);

  const updateStatus = async (id, status) => {
    setUpdating(id);
    try {
      await api.patch(`/appointments/${id}/status`, { status });
      fetchAppointments();
    } finally {
      setUpdating(null);
    }
  };

  const filtered = statusFilter === 'all'
    ? appointments
    : appointments.filter(a => a.status === statusFilter);

  const countByStatus = (s) => appointments.filter(a => a.status === s).length;

  const tabs = [
    { value: 'all', label: 'All', count: appointments.length },
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
          <p className="page-subtitle">{appointments.length} total appointments</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <input
            type="date"
            value={dateFilter}
            onChange={e => setDateFilter(e.target.value)}
            style={{ padding: '9px 12px', borderRadius: 10, border: '1.5px solid var(--border)', background: 'white', fontFamily: 'DM Sans' }}
          />
          {dateFilter && (
            <button className="btn btn-secondary btn-sm" onClick={() => setDateFilter('')}>Clear</button>
          )}
        </div>
      </div>

      <Tabs tabs={tabs} active={statusFilter} onChange={setStatusFilter} />

      <div className="table-wrap">
        {loading ? <LoadingSpinner /> : filtered.length === 0 ? (
          <EmptyState icon="📅" title="No appointments found" />
        ) : (
          <table>
            <thead>
              <tr>
                <th>Token</th>
                <th>Patient</th>
                <th>Doctor</th>
                <th>Department</th>
                <th>Date</th>
                <th>Time</th>
                <th>Type</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(a => (
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
        )}
      </div>
    </div>
  );
};

export default Appointments;
