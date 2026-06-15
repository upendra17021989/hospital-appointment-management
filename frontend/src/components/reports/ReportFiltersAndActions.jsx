import React from 'react';

import { LoadingSpinner } from '../Common';

const formatRangeHint = (startDate, endDate) => {
  if (!startDate || !endDate) return '';
  try {
    const s = new Date(startDate);
    const e = new Date(endDate);
    return `${s.toLocaleDateString('en-IN')} – ${e.toLocaleDateString('en-IN')}`;
  } catch {
    return `${startDate} – ${endDate}`;
  }
};

const ReportFiltersAndActions = ({
  startDate,
  endDate,
  setStartDate,
  setEndDate,
  loading,
  onGenerate,
  onDownloadCsv,
  onDownloadPdf,
  onDownloadExcel,
  error,
  activeTabLabel,
}) => {
  const handleExport = (format) => {
    if (loading) return;
    if (format === 'csv') return onDownloadCsv?.();
    if (format === 'pdf') return onDownloadPdf?.();
    if (format === 'excel') return onDownloadExcel?.();
  };

  return (
    <div>
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

        <div className="report-actions">
          <button className="btn btn-primary" onClick={onGenerate} disabled={loading}>
            {loading ? 'Loading...' : 'Generate Report'}
          </button>

          <div className="export-dropdown">
            <button
              type="button"
              className="btn btn-secondary export-dropdown__btn"
              disabled={loading}
              onClick={() => {
                const el = document.getElementById('report-export-select');
                if (el) el.focus();
              }}
            >
              Export ▼
            </button>

            <select
              id="report-export-select"
              className="export-dropdown__select"
              disabled={loading}
              defaultValue=""
              onChange={(e) => {
                const v = e.target.value;
                if (!v) return;
                handleExport(v);
                e.target.value = '';
              }}
            >
              <option value="" disabled>
                Choose format
              </option>
              <option value="csv">CSV</option>
              <option value="pdf">PDF</option>
              <option value="excel">Excel (.xlsx)</option>
            </select>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading && <LoadingSpinner />}

      {!!activeTabLabel && (
        <div className="report-summary-hint" aria-hidden="true">
          {formatRangeHint(startDate, endDate)}
        </div>
      )}
    </div>
  );
};

export default ReportFiltersAndActions;


