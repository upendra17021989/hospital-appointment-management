import React from 'react';
import QuickDateFilters, { applyQuickRange } from './QuickDateFilters';

const formatISO = (d) => {
  if (!d) return '';
  const dt = new Date(d);
  if (Number.isNaN(dt.getTime())) return d;
  const yyyy = dt.getFullYear();
  const mm = String(dt.getMonth() + 1).padStart(2, '0');
  const dd = String(dt.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

const Phase2ReportControls = ({
  activeQuickKey,
  onQuickSelect,
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
  onGenerate,
  onDownloadCsv,
  onDownloadPdf,
  loading,
  error,
  activeTabLabel,
  generatedAt,
  showingCountText,
}) => {
  return (
    <div>
      <div className="report-filters report-filters--phase2">
        <div className="filter-group">
          <label className="filter-label">Start Date</label>
          <input
            type="date"
            className="form-input"
            value={startDate}
            onChange={(e) => {
              onStartDateChange(e.target.value);
              // when user edits manually, clear quick selection by sending null key
              onQuickSelect(null, { startDate: formatISO(e.target.value), endDate });
            }}
          />
        </div>

        <div className="filter-group">
          <label className="filter-label">End Date</label>
          <input
            type="date"
            className="form-input"
            value={endDate}
            onChange={(e) => {
              onEndDateChange(e.target.value);
              onQuickSelect(null, { startDate, endDate: formatISO(e.target.value) });
            }}
          />
        </div>

        <div className="report-actions report-actions--phase2">
          <button className="btn btn-primary" onClick={onGenerate} disabled={loading}>
            {loading ? 'Generating...' : 'Generate Report'}
          </button>

          <button className="btn btn-secondary" onClick={onDownloadCsv} disabled={loading}>
            ⬇️ Download CSV
          </button>

          <button className="btn btn-secondary" onClick={onDownloadPdf} disabled={loading}>
            ⬇️ Download PDF
          </button>
        </div>

        {/* Quick date pills */}
        <QuickDateFilters
          activeKey={activeQuickKey}
          disabled={loading}
          onSelect={(key, nextStartDate, nextEndDate) => {
            const resolvedKey = key;
            const resolvedStart = nextStartDate || applyQuickRange(key).startDate;
            const resolvedEnd = nextEndDate || applyQuickRange(key).endDate;
            onQuickSelect(resolvedKey, { startDate: resolvedStart, endDate: resolvedEnd });
          }}
        />

        {error && <div className="alert alert-error">{error}</div>}

        {/* Summary (phase2) */}
        {startDate && endDate && (
          <div className="report-summary-card">
            <div className="report-summary-line">{showingCountText}</div>
            <div className="report-summary-line">
              Date Range: {new Date(startDate).toLocaleDateString('en-IN')} – {new Date(endDate).toLocaleDateString('en-IN')}
            </div>
            <div className="report-summary-line">
              Generated at: {generatedAt ? generatedAt : '—'}
            </div>
            {activeTabLabel ? (
              <div className="report-summary-line report-summary-muted">
                Showing: {activeTabLabel}
              </div>
            ) : null}
          </div>
        )}
      </div>
    </div>
  );
};


export default Phase2ReportControls;
