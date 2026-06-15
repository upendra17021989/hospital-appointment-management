import React from 'react';

const ReportTabs = ({ tabLabels, activeTab, onChange }) => {
  return (
    <div className="report-tabs">
      {Object.entries(tabLabels).map(([key, label]) => (
        <button
          key={key}
          className={`report-tab ${activeTab === key ? 'active' : ''}`}
          onClick={() => onChange(key)}
          type="button"
        >
          {label}
        </button>
      ))}
    </div>
  );
};

export default ReportTabs;

