import React, { useMemo } from 'react';

const normalizeGender = (g) => {
  const s = (g || '').toString().trim().toLowerCase();
  if (!s) return 'Unknown';
  if (s.startsWith('m')) return 'Male';
  if (s.startsWith('f')) return 'Female';
  // Sometimes values can be like 'male'/'female'/'other'
  if (s.includes('female')) return 'Female';
  if (s.includes('male')) return 'Male';
  return g.toString();
};

// Lightweight SVG charts (no extra deps)
const VisitsTrendChart = ({ points }) => {
  if (!points || points.length === 0) return <div className="analytics-empty">No trend data</div>;

  const w = 420;
  const h = 180;
  const padding = 18;

  const maxY = Math.max(...points.map((p) => p.value), 1);
  const minY = 0;

  const xStep = points.length === 1 ? 0 : (w - padding * 2) / (points.length - 1);

  const toX = (i) => padding + i * xStep;
  const toY = (val) => {
    const t = (val - minY) / (maxY - minY || 1);
    return h - padding - t * (h - padding * 2);
  };

  const lineD = points
    .map((p, i) => {
      const x = toX(i);
      const y = toY(p.value);
      return `${i === 0 ? 'M' : 'L'} ${x} ${y}`;
    })
    .join(' ');

  return (
    <div className="analytics-card">
      <div className="analytics-title">Visits Trend</div>
      <svg viewBox={`0 0 ${w} ${h}`} className="analytics-svg" role="img" aria-label="Visits trend chart">
        {/* grid */}
        {[0, 0.25, 0.5, 0.75, 1].map((t, idx) => {
          const y = padding + (h - padding * 2) * (1 - t);
          return <line key={idx} x1={padding} x2={w - padding} y1={y} y2={y} className="analytics-grid" />;
        })}

        <path d={lineD} fill="none" stroke="var(--analytics-primary)" strokeWidth="2.3" />

        {points.map((p, i) => {
          const x = toX(i);
          const y = toY(p.value);
          return (
            <g key={p.label || i}>
              <circle cx={x} cy={y} r="4.2" fill="var(--analytics-primary)" />
            </g>
          );
        })}

        {/* x labels (sparse) */}
        {points.map((p, i) => {
          const show = points.length <= 8 ? true : i % Math.ceil(points.length / 7) === 0 || i === points.length - 1;
          if (!show) return null;
          const x = toX(i);
          return (
            <text key={`t-${p.label || i}`} x={x} y={h - 4} textAnchor="middle" className="analytics-x-label">
              {p.label}
            </text>
          );
        })}
      </svg>
    </div>
  );
};

const GenderSplitChart = ({ counts }) => {
  const entries = Object.entries(counts || {}).filter(([, v]) => v > 0);
  if (entries.length === 0) return <div className="analytics-empty">No gender data</div>;

  // Pie via SVG arcs
  const total = entries.reduce((s, [, v]) => s + v, 0) || 1;
  const w = 260;
  const h = 210;
  const cx = 80;
  const cy = 90;
  const r = 62;

  const palette = [
    'rgba(99, 102, 241, 0.95)',
    'rgba(16, 185, 129, 0.95)',
    'rgba(245, 158, 11, 0.95)',
    'rgba(244, 63, 94, 0.95)',
    'rgba(59, 130, 246, 0.95)',
  ];

  const polarToCartesian = (centerX, centerY, radius, angleInDegrees) => {
    const angleInRadians = ((angleInDegrees - 90) * Math.PI) / 180.0;
    return {
      x: centerX + radius * Math.cos(angleInRadians),
      y: centerY + radius * Math.sin(angleInRadians),
    };
  };

  const describeArc = (startAngle, endAngle) => {
    const start = polarToCartesian(cx, cy, r, endAngle);
    const end = polarToCartesian(cx, cy, r, startAngle);

    const largeArcFlag = endAngle - startAngle <= 180 ? '0' : '1';

    return `M ${start.x} ${start.y} A ${r} ${r} 0 ${largeArcFlag} 0 ${end.x} ${end.y}`;
  };

  let currentAngle = 0;
  const slices = entries.map(([label, value], idx) => {
    const frac = value / total;
    const start = currentAngle;
    const end = currentAngle + frac * 360;
    currentAngle = end;
    const color = palette[idx % palette.length];
    return { label, value, color, d: describeArc(start, end) };
  });

  return (
    <div className="analytics-card">
      <div className="analytics-title">Gender Split</div>
      <div className="analytics-gender">
        <svg viewBox={`0 0 ${w} ${h}`} className="analytics-svg analytics-svg--gender" role="img" aria-label="Gender split chart">
          {slices.map((s, i) => (
            <path key={i} d={s.d} fill="none" stroke={s.color} strokeWidth="18" strokeLinecap="round" />
          ))}
          <circle cx={cx} cy={cy} r={34} fill="rgba(0,0,0,0.02)" />
          <text x={cx} y={cy - 4} textAnchor="middle" className="analytics-gender-center">
            {total}
          </text>
          <text x={cx} y={cy + 14} textAnchor="middle" className="analytics-gender-center-sub">
            patients
          </text>
        </svg>

        <div className="analytics-legend">
          {slices.map((s, i) => (
            <div className="analytics-legend-item" key={i}>
              <span className="analytics-legend-dot" style={{ background: s.color }} aria-hidden="true" />
              <span className="analytics-legend-label">{s.label}</span>
              <span className="analytics-legend-value">{Math.round((s.value / total) * 100)}%</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

const AnalyticsCharts = ({ data, loading }) => {
  const patientsList = data?.patientsList || [];

  const analytics = useMemo(() => {
    // Gender counts
    const genderCounts = {};
    for (const p of patientsList) {
      const g = normalizeGender(p?.gender);
      genderCounts[g] = (genderCounts[g] || 0) + 1;
    }

    // Visits trend approximation:
    // We only have lastVisitDate/time per patient in patientsList.
    // Group by lastVisitDate and use visitCount as weight (approx).
    const trendByDate = {};
    for (const p of patientsList) {
      const d = p?.lastVisitDate;
      if (!d) continue;
      const key = d.toString();
      const weight = Number(p?.visitCount || 1);
      trendByDate[key] = (trendByDate[key] || 0) + weight;
    }

    const sortedDates = Object.keys(trendByDate).sort();
    const points = sortedDates.map((dateStr) => {
      const dt = new Date(dateStr);
      const label = Number.isNaN(dt.getTime()) ? dateStr : dt.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
      return { label, value: trendByDate[dateStr] };
    });

    return { genderCounts, points };
  }, [patientsList]);

  if (loading) {
    return (
      <div className="analytics-row">
        <div className="analytics-grid">
          <div className="analytics-skeleton-card">
            <div className="analytics-skeleton-title" />
            <div className="analytics-skeleton-chart" />
          </div>
          <div className="analytics-skeleton-card">
            <div className="analytics-skeleton-title" />
            <div className="analytics-skeleton-chart analytics-skeleton-chart--gender" />
            <div className="analytics-skeleton-legend" />
          </div>
        </div>
      </div>
    );
  }

  const hasAnyData = (data?.patientsList || []).length > 0;

  return (
    <div className="analytics-row">
      <div className="analytics-grid">
        <VisitsTrendChart points={analytics.points} />
        <GenderSplitChart counts={analytics.genderCounts} />
      </div>
      {!hasAnyData && (
        <div className="analytics-empty" style={{ marginTop: 10 }}>
          No analytics available for the selected range.
        </div>
      )}
    </div>
  );
};


export default AnalyticsCharts;

