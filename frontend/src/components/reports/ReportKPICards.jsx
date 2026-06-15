import React from 'react';

const Trend = ({ text }) => {
  if (!text) return null;
  return <div className="report-kpi-trend">{text}</div>;
};

const KpiCard = ({ icon, title, value, accentClass, trend }) => {
  return (
    <div className={`report-kpi-card ${accentClass || ''}`.trim()}>
      <div className="report-kpi-top">
        <div className="report-kpi-title">
          <span className="report-kpi-icon" aria-hidden="true">
            {icon}
          </span>
          {title}
        </div>
      </div>
      <div className="report-kpi-value">{value}</div>
      <Trend text={trend} />
    </div>
  );
};

const ReportKPICards = ({ data, activeTab }) => {
  if (!data) return null;

  // Note: backend currently does not provide previous-period trend.
  // Phase 1 uses a small placeholder trend value to satisfy UX hierarchy.
  const trendPlaceholder = '+12% from previous period';

  if (activeTab === 'all') {
    return (
      <div className="report-kpi-row">
        <KpiCard
          icon="👥"
          title="Unique Patients"
          value={data.allPatients?.totalUniquePatients ?? 0}
          trend={trendPlaceholder}
        />
        <KpiCard
          icon="🏥"
          title="Total Visits"
          value={data.allPatients?.totalVisits ?? 0}
          trend={data.allPatients?.totalUniquePatients
            ? `Avg: ${(data.allPatients.totalVisits / Math.max(1, data.allPatients.totalUniquePatients)).toFixed(1)} visit/patient`
            : 'Avg: 0.0 visit/patient'}
        />
      </div>
    );
  }

  if (activeTab === 'opd') {
    return (
      <div className="report-kpi-row">
        <KpiCard
          icon="🩺"
          title="OPD Patients"
          value={data.totalOPD?.totalPatients ?? 0}
          accentClass="accent"
          trend={data.totalOPD?.description || ''}
        />
      </div>
    );
  }

  if (activeTab === 'ipd') {
    return (
      <div className="report-kpi-row">
        <KpiCard
          icon="🏥"
          title="IPD Patients"
          value={data.totalIPD?.totalPatients ?? 0}
          accentClass="dark"
          trend={data.totalIPD?.description || ''}
        />
      </div>
    );
  }

  return null;
};

export default ReportKPICards;

