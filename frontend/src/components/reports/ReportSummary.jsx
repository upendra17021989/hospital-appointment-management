import React from 'react';

const formatDate = (iso) => {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('en-IN');
};

const ReportSummary = ({ startDate, endDate, generatedAt, showingText }) => {
  if (!startDate || !endDate) return null;

  return (
    <div className="report-summary-card" role="status" aria-live="polite">
      <div className="report-summary-line">{showingText}</div>
      <div className="report-summary-line">Date Range: {formatDate(startDate)} – {formatDate(endDate)}</div>
      <div className="report-summary-line report-summary-generated">Generated at: {generatedAt || '—'}</div>
    </div>
  );
};

export default ReportSummary;

