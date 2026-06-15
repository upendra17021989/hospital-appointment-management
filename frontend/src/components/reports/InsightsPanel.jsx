import React, { useMemo } from 'react';

const normalizeGender = (g) => {
  const s = (g || '').toString().trim().toLowerCase();
  if (!s) return 'Unknown';
  if (s.startsWith('m')) return 'Male';
  if (s.startsWith('f')) return 'Female';
  if (s.includes('female')) return 'Female';
  if (s.includes('male')) return 'Male';
  return g.toString();
};

const formatNumber = (n) => (typeof n === 'number' && Number.isFinite(n) ? n : 0);

const InsightsPanel = ({ data, activeTab }) => {
  const patientsList = data?.patientsList || [];

  const insights = useMemo(() => {
    const totalPatients = patientsList.length;

    const genderCounts = {};
    let ageSum = 0;
    let ageCount = 0;

    const visitCounts = [];
    for (const p of patientsList) {
      const g = normalizeGender(p?.gender);
      genderCounts[g] = (genderCounts[g] || 0) + 1;

      const age = p?.age;
      if (typeof age === 'number' && Number.isFinite(age)) {
        ageSum += age;
        ageCount += 1;
      }

      const vc = Number(p?.visitCount ?? 0);
      visitCounts.push(Number.isFinite(vc) ? vc : 0);
    }

    const avgAge = ageCount ? ageSum / ageCount : null;

    const topGenderEntry = Object.entries(genderCounts).sort((a, b) => b[1] - a[1])[0];
    const topGender = topGenderEntry ? topGenderEntry[0] : null;
    const topGenderCount = topGenderEntry ? topGenderEntry[1] : 0;

    const mostVisits = Math.max(...visitCounts, 0);
    const returningCount = visitCounts.filter((v) => v > 1).length;

    return {
      totalPatients,
      topGender,
      topGenderCount,
      avgAge,
      mostVisits,
      returningCount,
    };
  }, [patientsList]);

  if (!data) return null;

  const title = activeTab === 'all' ? 'Insights (All Patients)' : 'Insights';

  const renderEmpty = () => (
    <div className="insights-card">
      <div className="insights-title">{title}</div>
      <div className="insights-empty">
        No patient insights available for the selected range.
      </div>
    </div>
  );

  if (insights.totalPatients === 0) return renderEmpty();

  const avgAgeText = insights.avgAge == null ? '—' : `${insights.avgAge.toFixed(1)} yrs`;
  const topGenderPct = insights.totalPatients
    ? Math.round((insights.topGenderCount / insights.totalPatients) * 100)
    : 0;

  return (
    <div className="insights-row">
      <div className="insights-card">
        <div className="insights-title">{title}</div>
        <div className="insights-metrics">
          <div className="insights-metric">
            <div className="insights-metric__label">Top Gender</div>
            <div className="insights-metric__value">
              {insights.topGender || '—'}
              <span className="insights-metric__sub">
                {insights.topGender ? ` (${topGenderPct}%)` : ''}
              </span>
            </div>
          </div>

          <div className="insights-metric">
            <div className="insights-metric__label">Avg Age</div>
            <div className="insights-metric__value">{avgAgeText}</div>
          </div>

          <div className="insights-metric">
            <div className="insights-metric__label">Most Visits (single patient)</div>
            <div className="insights-metric__value">{formatNumber(insights.mostVisits)}</div>
          </div>

          <div className="insights-metric">
            <div className="insights-metric__label">Returning Patients</div>
            <div className="insights-metric__value">
              {formatNumber(insights.returningCount)}
              <span className="insights-metric__sub">
                {insights.totalPatients ? ` (${Math.round((insights.returningCount / insights.totalPatients) * 100)}%)` : ''}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InsightsPanel;

