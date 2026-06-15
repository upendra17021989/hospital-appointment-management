import React, { useState, useCallback } from 'react';
import api from '../services/api';

import {
  LoadingSpinner,
} from '../components/Common';

import ReportFiltersAndActions from '../components/reports/ReportFiltersAndActions';
import ReportTabs from '../components/reports/ReportTabs';
import ReportKPICards from '../components/reports/ReportKPICards';
import ReportTable from '../components/reports/ReportTable';
import AnalyticsCharts from '../components/reports/AnalyticsCharts';
import InsightsPanel from '../components/reports/InsightsPanel';


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

  const [selectedDoctor, setSelectedDoctor] = useState('');
  const [doctorPatients, setDoctorPatients] = useState(null);
  const [doctorLoading, setDoctorLoading] = useState(false);

  const resetDepartmentDrilldown = useCallback(() => {
    setSelectedDepartment('');
    setDepartmentPatients(null);
  }, []);

  const resetDoctorDrilldown = useCallback(() => {
    setSelectedDoctor('');
    setDoctorPatients(null);
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
      resetDoctorDrilldown();
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

  const fetchDoctorPatients = useCallback(async (doctorName) => {
    if (!startDate || !endDate || !doctorName) return;

    setDoctorLoading(true);
    setDoctorPatients(null);
    try {
      const result = await api.get(
        `/reports/patients/by-doctor?startDate=${startDate}&endDate=${endDate}&doctor=${encodeURIComponent(doctorName)}`
      );
      setDoctorPatients(result);
      setSelectedDoctor(doctorName);
    } catch (err) {
      setError(err.message || 'Failed to fetch doctor patient records');
    } finally {
      setDoctorLoading(false);
    }
  }, [startDate, endDate]);

  const downloadCsv = useCallback(() => {
    if (!startDate || !endDate) {
      setError('Please select both start and end dates to download.');
      return;
    }

    const token = localStorage.getItem('hms_token');
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5173/api';
    const url = `${baseUrl}/reports/patients/download?startDate=${startDate}&endDate=${endDate}`;

    if (!token) {
      setError('Missing auth token. Please login again.');
      return;
    }

    fetch(url, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then(async (res) => {
        if (!res.ok) throw new Error(`Download failed: ${res.status}`);
        const blob = await res.blob();
        const cd = res.headers.get('Content-Disposition');
        const match = cd && cd.match(/filename="?([^\"]+)"?/);
        const filename = match ? match[1] : `patient-report-${startDate}-${endDate}.csv`;
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
      })
      .catch((e) => setError(e.message || 'Failed to download CSV'));
  }, [startDate, endDate]);

  const downloadPdf = useCallback(() => {
    if (!startDate || !endDate) {
      setError('Please select both start and end dates to download.');
      return;
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5173/api';

    const token = localStorage.getItem('hms_token');
    if (!token) {
      setError('Missing auth token. Please login again.');
      return;
    }

    let url = null;
    if (activeTab === 'all') {
      url = `${baseUrl}/reports/patients/download/pdf?startDate=${startDate}&endDate=${endDate}`;
    } else if (activeTab === 'department') {
      if (!selectedDepartment) {
        setError('Select a department to download Department-wise PDF.');
        return;
      }
      url = `${baseUrl}/reports/patients/by-department/download/pdf?startDate=${startDate}&endDate=${endDate}&department=${encodeURIComponent(selectedDepartment)}`;
    } else if (activeTab === 'doctor') {
      if (!selectedDoctor) {
        setError('Select a doctor to download Doctor-wise PDF.');
        return;
      }
      url = `${baseUrl}/reports/patients/by-doctor/download/pdf?startDate=${startDate}&endDate=${endDate}&doctor=${encodeURIComponent(selectedDoctor)}`;
    } else if (activeTab === 'opd') {
      url = `${baseUrl}/reports/patients/opd/download/pdf?startDate=${startDate}&endDate=${endDate}`;
    } else if (activeTab === 'ipd') {
      url = `${baseUrl}/reports/patients/ipd/download/pdf?startDate=${startDate}&endDate=${endDate}`;
    } else {
      setError('Invalid tab selection for PDF download.');
      return;
    }

    fetch(url, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then(async (res) => {
        if (!res.ok) throw new Error(`Download failed: ${res.status}`);
        const blob = await res.blob();
        const cd = res.headers.get('Content-Disposition');
        const match = cd && cd.match(/filename="?([^\"]+)"?/);
        const filename = match ? match[1] : `patient-report-${startDate}-${endDate}.pdf`;

        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
      })
      .catch((e) => setError(e.message || 'Failed to download PDF'));
  }, [activeTab, selectedDepartment, selectedDoctor, startDate, endDate]);

  const downloadExcel = useCallback(() => {
    if (!startDate || !endDate) {
      setError('Please select both start and end dates to download.');
      return;
    }

    if (activeTab !== 'all') {
      setError('Excel export is available for All Patients tab only.');
      return;
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5173/api';
    const token = localStorage.getItem('hms_token');
    if (!token) {
      setError('Missing auth token. Please login again.');
      return;
    }

    const url = `${baseUrl}/reports/patients/excel?startDate=${startDate}&endDate=${endDate}`;

    fetch(url, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then(async (res) => {
        if (!res.ok) throw new Error(`Download failed: ${res.status}`);
        const blob = await res.blob();
        const cd = res.headers.get('Content-Disposition');
        const match = cd && cd.match(/filename="?([^\"]+)"?/);
        const filename = match ? match[1] : `patient-report-${startDate}-${endDate}.xlsx`;

        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
      })
      .catch((e) => setError(e.message || 'Failed to download Excel'));
  }, [activeTab, startDate, endDate]);

  const activeTabLabel = TAB_LABELS[activeTab];

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Reports</h1>
          <p className="page-subtitle">Patient visit statistics between selected dates</p>
        </div>
      </div>

      <ReportFiltersAndActions
        startDate={startDate}
        endDate={endDate}
        setStartDate={(v) => {
          setStartDate(v);
          // manual edit resets quick selection
          setActiveQuickKey(null);
        }}
        setEndDate={(v) => {
          setEndDate(v);
          setActiveQuickKey(null);
        }}
        loading={loading}
        onGenerate={fetchReports}
        onDownloadCsv={downloadCsv}
        onDownloadPdf={downloadPdf}
        onDownloadExcel={downloadExcel}
        error={error}
        activeTabLabel={data ? activeTabLabel : ''}
      />


      {error && <div className="alert alert-error">{error}</div>}

      {loading && <LoadingSpinner />}

      {data && !loading && (
        <div className="report-results">
          <div className="report-tabs">
            <ReportTabs tabLabels={TAB_LABELS} activeTab={activeTab} onChange={setActiveTab} />
          </div>

          {/* KPI Cards (only for patient-centric tabs in Phase 1) */}
          <ReportKPICards data={data} activeTab={activeTab} />

          {/* Phase 6 — Insights panel */}
          <InsightsPanel data={data} activeTab={activeTab} />

          {/* Phase 4 — Analytics charts (Phase 7 skeleton handled inside) */}
          <AnalyticsCharts data={data} loading={loading} />


          <div className="report-tab-content">
            <ReportTable

              data={data}
              activeTab={activeTab}
              selectedDepartment={selectedDepartment}
              departmentPatients={departmentPatients}
              departmentLoading={departmentLoading}
              fetchDepartmentPatients={fetchDepartmentPatients}
              selectedDoctor={selectedDoctor}
              doctorPatients={doctorPatients}
              doctorLoading={doctorLoading}
              fetchDoctorPatients={fetchDoctorPatients}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default Reports;

