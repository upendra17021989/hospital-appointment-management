import React from 'react';

import { LoadingSpinner } from '../Common';

const Phone = ({ value }) => value || '—';

const LastVisit = ({ date, time }) => (
  <>
    {date || '—'}
    {time ? ` · ${time}` : ''}
  </>
);

const PatientsTable = ({ patientsList, emptyText }) => {
  const rows = patientsList || [];

  return (
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
          {rows.length === 0 ? (
            <tr>
              <td colSpan={7} className="empty-cell">
                {emptyText}
              </td>
            </tr>
          ) : (
            rows.map((p, idx) => (
              <tr key={p.patientId || idx}>
                <td>{idx + 1}</td>
                <td>
                  <strong>{p.patientName || '—'}</strong>
                </td>
                <td>{p.age ?? '—'}</td>
                <td>{p.gender || '—'}</td>
                <td>
                  <Phone value={p.phone} />
                </td>
                <td>
                  <LastVisit date={p.lastVisitDate} time={p.lastVisitTime} />
                </td>
                <td>{p.visitCount ?? 0}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};

const ReportTable = ({
  data,
  activeTab,
  selectedDepartment,
  departmentPatients,
  departmentLoading,
  fetchDepartmentPatients,
  selectedDoctor,
  doctorPatients,
  doctorLoading,
  fetchDoctorPatients,
}) => {
  if (!data) return null;

  switch (activeTab) {
    case 'all':
      return (
        <div className="report-section">
          <PatientsTable
            patientsList={data.patientsList}
            emptyText="No patient data available"
          />
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
                  <tr>
                    <td colSpan={3} className="empty-cell">
                      No data available
                    </td>
                  </tr>
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
              <PatientsTable
                patientsList={departmentPatients?.patientsList}
                emptyText={`No patient data available for ${selectedDepartment}`}
              />
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
                  <tr>
                    <td colSpan={3} className="empty-cell">
                      No data available
                    </td>
                  </tr>
                ) : (
                  (data.doctorWise || []).map((item, idx) => {
                    const isActive = selectedDoctor === item.doctor;
                    return (
                      <tr key={idx}>
                        <td>{idx + 1}</td>
                        <td>
                          <button
                            type="button"
                            className={`btn-linkish ${isActive ? 'active' : ''}`}
                            style={{ padding: 0, background: 'transparent', border: 'none', cursor: 'pointer' }}
                            onClick={() => fetchDoctorPatients(item.doctor)}
                          >
                            <strong>{item.doctor}</strong>
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
            {doctorLoading && <LoadingSpinner />}

            {!doctorLoading && selectedDoctor && (
              <PatientsTable
                patientsList={doctorPatients?.patientsList}
                emptyText={`No patient data available for ${selectedDoctor}`}
              />
            )}

            {!doctorLoading && !selectedDoctor && (
              <div className="empty-hint" style={{ opacity: 0.8, padding: '12px 0' }}>
                Click a doctor name to view its patient records.
              </div>
            )}
          </div>
        </div>
      );

    case 'opd':
      return (
        <div className="report-section">
          <PatientsTable
            patientsList={data.totalOPD?.patientsList}
            emptyText="No patient data available for OPD"
          />
        </div>
      );

    case 'ipd':
      return (
        <div className="report-section">
          <PatientsTable
            patientsList={data.totalIPD?.patientsList}
            emptyText="No patient data available for IPD"
          />
        </div>
      );

    default:
      return null;
  }
};

export default ReportTable;

