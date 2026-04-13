import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { LoadingSpinner, Badge } from '../components/Common';


const Dashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);

  const [todayAppts, setTodayAppts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const today = new Date().toISOString().split('T')[0];
    Promise.all([
      api.get('/dashboard/stats').catch(() => null),
      api.get(`/appointments?date=${today}`).catch(() => []),
    ]).then(([s, appts]) => {
      setStats(s);
      setTodayAppts(appts || []);
      setLoading(false);
    });
  }, []);

  if (loading) return <LoadingSpinner />;

  const statCards = [
    { label: "Today's Appointments", value: stats?.todayAppointments ?? 0, color: 'primary', icon: '📅' },
    { label: 'Pending Confirmation', value: stats?.pendingAppointments ?? 0, color: 'gold', icon: '⏳' },
    { label: 'Open Enquiries', value: stats?.openEnquiries ?? 0, color: 'accent', icon: '💬' },
    { label: 'Total Patients', value: stats?.totalPatients ?? 0, color: 'dark', icon: '👥' },
    { label: 'Active Doctors', value: stats?.totalDoctors ?? 0, color: 'accent', icon: '👨‍⚕️' },
    { label: 'Departments', value: stats?.totalDepartments ?? 0, color: 'primary', icon: '🏥' },
  ];

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Good morning 👋</h1>
          <p className="page-subtitle">
            {new Date().toLocaleDateString('en-IN', {
              weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
            })}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/book-appointment')}>
          + Book Appointment
        </button>

      </div>

      {/* Stats */}
      <div className="stats-grid">
        {statCards.map((s, i) => (
          <div key={i} className={`stat-card ${s.color}`}>
            <div className="stat-value">{s.value}</div>
            <div className="stat-label">{s.label}</div>
            <div className="stat-icon">{s.icon}</div>
          </div>
        ))}
      </div>

      {/* Today's Appointments */}
      <div className="table-wrap">
        <div className="table-header">
          <div>
            <div className="card-title" style={{ margin: 0 }}>Today's Appointments</div>
            <div style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 2 }}>
              {todayAppts.length} scheduled
            </div>
          </div>
        </div>

        {todayAppts.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📅</div>
            <div className="empty-state-title">No appointments today</div>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Token</th>
                <th>Patient</th>
                <th>Doctor</th>
                <th>Time</th>
                <th>Type</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {todayAppts.slice(0, 10).map(a => (
                <tr key={a.id}>
                  <td><span className="token-badge">{a.tokenNumber}</span></td>
                  <td><strong>{a.patient?.fullName}</strong></td>
                  <td>{a.doctor?.fullName}</td>
                  <td>{a.appointmentTime}</td>
                  <td><Badge status={a.appointmentType} /></td>
                  <td><Badge status={a.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default Dashboard;
