import React, { useState, useCallback } from 'react';
import api from '../services/api';
import { LoadingSpinner } from '../components/Common';

const TAB_LABELS = {
  all: 'All Patients',
  department: 'Department Wise',
  doctor: 'Doctor Wise',
  opd: 'Total OPD',
  ipd: 'Total IPD',
};

const Reports = () => {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('all');

  const [selectedDepartment, setSelectedDepartment] = useState('');
  const [departmentPatients, setDepartmentPatients] = useState(null);
  const [departmentLoading, setDepartmentLoading] = useState(false);

  const resetDepartmentDrilldown = useCallback(() => {
    setSelectedDepartment('');
    setDepartmentPatients(null);
  }, []);

  const fetchReports = useCallback(async () => {
    if (!startDate || !endDate) {
      setError('Please select both start and end dates.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const result = await api.get(`/reports/patients?startDate=${startDate}&endDate=${endDate}`);
      setData(result);
      resetDepartmentDrilldown();
    } catch (err) {
      setError(err.message || 'Failed to fetch reports');
      setData(null);
      resetDepartmentDrilldown();
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate, resetDepartmentDrilldown]);

  const fetchDepartmentPatients = useCallback(async (departmentName) => {

    if (!startDate || !endDate || !departmentName) return;

    setDepartmentLoading(true);
    setDepartmentPatients(null);
    try {
      const result = await api.get(
        `/reports/patients/by-department?startDate=${startDate}&endDate=${endDate}&department=${encodeURIComponent(departmentName)}`
      );
      setDepartmentPatients(result);
      setSelectedDepartment(departmentName);
    } catch (err) {
      setError(err.message || 'Failed to fetch department patient records');
    } finally {
      setDepartmentLoading(false);
    }
  }, [startDate, endDate]);

  const renderTabContent = () => {
    if (!data) return null;

    switch (activeTab) {

      case 'all':
        return (
          <div className="report-section">
            <div className="report-stat-row">
              <div className="report-stat-card">
                <div className="report-stat-value">{data.allPatients?.totalUniquePatients ?? 0}</div>
                <div className="report-stat-label">Unique Patients</div>
              </div>
              <div className="report-stat-card">
                <div className="report-stat-value">{data.allPatients?.totalVisits ?? 0}</div>
                <div className="report-stat-label">Total Visits</div>
              </div>
            </div>

            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Patient</th>
                    <th>Age</th>
                    <th>Gender</th>
                    <th>Phone</th>
                    <th>Last Visit</th>
                    <th>Visits</th>
                  </tr>
                </thead>
                <tbody>
                  {(data.patientsList || []).length === 0 ? (
                    <tr>
                      <td colSpan={7} className="empty-cell">No patient data available</td>
                    </tr>
                  ) : (
                    (data.patientsList || []).map((p, idx) => (
                      <tr key={p.patientId || idx}>
                        <td>{idx + 1}</td>
                        <td>
                          <strong>{p.patientName || '—'}</strong>
                        </td>
                        <td>{p.age ?? '—'}</td>
                        <td>{p.gender || '—'}</td>
                        <td>{p.phone || '—'}</td>
                        <td>
                          {p.lastVisitDate || '—'}{p.lastVisitTime ? ` · ${p.lastVisitTime}` : ''}
                        </td>
                        <td>{p.visitCount ?? 0}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        );
      case 'department':
        return (
          <div className="report-section">
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Department</th>
                    <th>Patient Count</th>
                  </tr>
                </thead>
                <tbody>
                  {(data.departmentWise || []).length === 0 ? (
                    <tr><td colSpan={3} className="empty-cell">No data available</td></tr>
                  ) : (
                    (data.departmentWise || []).map((item, idx) => {
                      const isActive = selectedDepartment === item.department;
                      return (
                        <tr key={idx}>
                          <td>{idx + 1}</td>
                          <td>
                            <button
                              type="button"
                              className={`btn-linkish ${isActive ? 'active' : ''}`}
                              style={{ padding: 0, background: 'transparent', border: 'none', cursor: 'pointer' }}
                              onClick={() => fetchDepartmentPatients(item.department)}
                            >
                              <strong>{item.department}</strong>
                            </button>
                          </td>
                          <td>{item.patientCount}</td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            <div style={{ marginTop: 16 }}>
              {departmentLoading && <LoadingSpinner />}

              {!departmentLoading && selectedDepartment && (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Patient</th>
                        <th>Age</th>
                        <th>Gender</th>
                        <th>Phone</th>
                        <th>Last Visit</th>
                        <th>Visits</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(departmentPatients?.patientsList || []).length === 0 ? (
                        <tr>
                          <td colSpan={7} className="empty-cell">No patient data available for {selectedDepartment}</td>
                        </tr>
                      ) : (
                        (departmentPatients?.patientsList || []).map((p, idx) => (
                          <tr key={p.patientId || idx}>
                            <td>{idx + 1}</td>
                            <td><strong>{p.patientName || '—'}</strong></td>
                            <td>{p.age ?? '—'}</td>
                            <td>{p.gender || '—'}</td>
                            <td>{p.phone || '—'}</td>
                            <td>
                              {p.lastVisitDate || '—'}{p.lastVisitTime ? ` · ${p.lastVisitTime}` : ''}
                            </td>
                            <td>{p.visitCount ?? 0}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              )}

              {!departmentLoading && !selectedDepartment && (
                <div className="empty-hint" style={{ opacity: 0.8, padding: '12px 0' }}>
                  Click a department name to view its patient records.
                </div>
              )}
            </div>
          </div>
        );

      case 'doctor':
        return (
          <div className="report-section">
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Doctor</th>
                    <th>Patient Count</th>
                  </tr>
                </thead>
                <tbody>
                  {(data.doctorWise || []).length === 0 ? (
                    <tr><td colSpan={3} className="empty-cell">No data available</td></tr>
                  ) : (
                    (data.doctorWise || []).map((item, idx) => (
                      <tr key={idx}>
                        <td>{idx + 1}</td>
                        <td><strong>{item.doctor}</strong></td>
                        <td>{item.patientCount}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        );
      case 'opd':
        return (
          <div className="report-section">
            <div className="report-stat-row">
              <div className="report-stat-card accent">
                <div className="report-stat-value">{data.totalOPD?.totalPatients ?? 0}</div>
                <div className="report-stat-label">OPD Patients</div>
                <div className="report-stat-desc">{data.totalOPD?.description}</div>
              </div>
            </div>
          </div>
        );
      case 'ipd':
        return (
          <div className="report-section">
            <div className="report-stat-row">
              <div className="report-stat-card dark">
                <div className="report-stat-value">{data.totalIPD?.totalPatients ?? 0}</div>
                <div className="report-stat-label">IPD Patients</div>
                <div className="report-stat-desc">{data.totalIPD?.description}</div>
              </div>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Reports</h1>
          <p className="page-subtitle">
            Patient visit statistics between selected dates
          </p>
        </div>
      </div>

      {/* Date Range Picker */}
      <div className="report-filters">
        <div className="filter-group">
          <label className="filter-label">Start Date</label>
          <input
            type="date"
            className="form-input"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label className="filter-label">End Date</label>
          <input
            type="date"
            className="form-input"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </div>
        <button className="btn btn-primary" onClick={fetchReports} disabled={loading}>
          {loading ? 'Loading...' : 'Generate Report'}
        </button>
        <button
          className="btn btn-secondary"
          onClick={() => {
            if (!startDate || !endDate) {
              setError('Please select both start and end dates to download.');
              return;
            }
            window.location.href = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5173/api'}/reports/patients/download?startDate=${startDate}&endDate=${endDate}`;
          }}
        >
          ⬇️ Download CSV
        </button>
        <button
          className="btn btn-secondary"
          onClick={() => {
            if (!startDate || !endDate) {
              setError('Please select both start and end dates to download.');
              return;
            }
            window.location.href = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5173/api'}/reports/patients/download/pdf?startDate=${startDate}&endDate=${endDate}`;
          }}
        >
          ⬇️ Download PDF
        </button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {loading && <LoadingSpinner />}

      {data && !loading && (
        <div className="report-results">
          {/* Tab Navigation */}
          <div className="report-tabs">
            {Object.entries(TAB_LABELS).map(([key, label]) => (
              <button
                key={key}
                className={`report-tab ${activeTab === key ? 'active' : ''}`}
                onClick={() => setActiveTab(key)}
              >
                {label}
              </button>
            ))}
          </div>

          {/* Tab Content */}
          <div className="report-tab-content">
            {renderTabContent()}
          </div>
        </div>
      )}
    </div>
  );
};

export default Reports;
