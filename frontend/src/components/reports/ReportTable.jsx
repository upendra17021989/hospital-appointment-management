import React, { useEffect, useMemo, useState } from 'react';

import { LoadingSpinner } from '../Common';

const Phone = ({ value }) => {
  const s = (value ?? '').toString().trim();
  if (!s) return '—';
  return s;
};

const LastVisit = ({ date, time }) => {
  if (!date && !time) return '—';
  const dStr = date ? date.toString() : '';
  const tStr = time ? ` · ${time}` : '';
  return (
    <>
      {dStr}
      {tStr}
    </>
  );
};

const debounce = (fn, wait) => {
  let t;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), wait);
  };
};

const sortableValue = (p) => {
  const age = p?.age;
  const visits = Number(p?.visitCount ?? 0);
  const phone = (p?.phone ?? '').toString();
  const name = (p?.patientName ?? '').toString();
  const gender = (p?.gender ?? '').toString();
  const lastVisitDate = p?.lastVisitDate ? new Date(p.lastVisitDate.toString()) : null;
  return {
    age: age == null ? null : Number(age),
    visits,
    phone,
    name,
    gender,
    lastVisitDate,
  };
};

const PatientsTable = ({ patientsList, emptyText }) => {
  const rows = patientsList || [];

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const [sortKey, setSortKey] = useState('lastVisitDate');
  const [sortDir, setSortDir] = useState('desc');

  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState(1);

  useEffect(() => {
    setPage(1);
  }, [debouncedSearch, pageSize, sortKey, sortDir]);

  useEffect(() => {
    const handler = debounce((v) => setDebouncedSearch(v), 300);
    handler(search);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  const filtered = useMemo(() => {
    const q = debouncedSearch.trim().toLowerCase();
    if (!q) return rows;

    return rows.filter((p) => {
      const sv = sortableValue(p);
      const haystack = [sv.name, sv.gender, sv.phone].filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(q);
    });
  }, [rows, debouncedSearch]);

  const sorted = useMemo(() => {
    const dir = sortDir === 'asc' ? 1 : -1;
    const copy = [...filtered];

    const cmp = (a, b) => {
      const sa = sortableValue(a);
      const sb = sortableValue(b);

      let av;
      let bv;
      if (sortKey === 'age') {
        av = sa.age;
        bv = sb.age;
      } else if (sortKey === 'visits') {
        av = sa.visits;
        bv = sb.visits;
      } else if (sortKey === 'phone') {
        av = sa.phone;
        bv = sb.phone;
      } else if (sortKey === 'lastVisitDate') {
        av = sa.lastVisitDate;
        bv = sb.lastVisitDate;
      } else {
        av = sa.name;
        bv = sb.name;
      }

      const aNull = av == null || (av instanceof Date && Number.isNaN(av.getTime()));
      const bNull = bv == null || (bv instanceof Date && Number.isNaN(bv.getTime()));
      if (aNull && bNull) return 0;
      if (aNull) return 1 * dir;
      if (bNull) return -1 * dir;

      if (av instanceof Date && bv instanceof Date) return dir * (av.getTime() - bv.getTime());
      if (typeof av === 'number' && typeof bv === 'number') return dir * (av - bv);

      return dir * String(av).localeCompare(String(bv), undefined, { sensitivity: 'base' });
    };

    copy.sort(cmp);
    return copy;
  }, [filtered, sortDir, sortKey]);

  const totalPages = Math.max(1, Math.ceil(sorted.length / pageSize));
  const safePage = Math.min(page, totalPages);

  const paged = useMemo(() => {
    const startIdx = (safePage - 1) * pageSize;
    return sorted.slice(startIdx, startIdx + pageSize);
  }, [sorted, safePage, pageSize]);

  const onSort = (key) => {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('desc');
    }
  };

  const SortTh = ({ label, k }) => {
    const active = sortKey === k;
    const arrow = active ? (sortDir === 'asc' ? ' ↑' : ' ↓') : '';
    return (
      <th>
        <button type="button" className="th-sort" onClick={() => onSort(k)}>
          {label}
          {arrow}
        </button>
      </th>
    );
  };

  return (
    <div className="table-section">
      <div className="table-controls">
        <div className="search-bar">
          <div className="search-input-wrap">
            <span className="search-icon">🔎</span>
            <input
              className="form-input"
              style={{ paddingLeft: 40 }}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search name / gender / phone"
              aria-label="Search patients"
            />
          </div>
        </div>

        <div className="table-controls__right">
          <div className="page-size">
            <label className="page-size__label">Rows</label>
            <select
              className="form-select"
              value={pageSize}
              onChange={(e) => setPageSize(Number(e.target.value))}
              aria-label="Rows per page"
            >
              {[10, 20, 50].map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="table-wrap table-wrap--scrollable">
        <table>
          <thead className="sticky-thead">
            <tr>
              <th>#</th>
              <th>Patient</th>
              <SortTh label="Age" k="age" />
              <th>Gender</th>
              <SortTh label="Phone" k="phone" />
              <SortTh label="Last Visit" k="lastVisitDate" />
              <SortTh label="Visits" k="visits" />
            </tr>
          </thead>
          <tbody>
            {paged.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-cell">
                  {emptyText}
                </td>
              </tr>
            ) : (
              paged.map((p, idx) => (
                <tr key={p.patientId || idx}>
                  <td>{(safePage - 1) * pageSize + idx + 1}</td>
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

      <div className="pagination">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={safePage <= 1}
          onClick={() => setPage((p) => Math.max(1, p - 1))}
        >
          Prev
        </button>

        <div className="pagination__meta">
          Page <strong>{safePage}</strong> of <strong>{totalPages}</strong>
        </div>

        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={safePage >= totalPages}
          onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
        >
          Next
        </button>
      </div>
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

