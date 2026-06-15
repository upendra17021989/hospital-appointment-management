import React from 'react';

const QUICK_OPTIONS = [
  { key: 'today', label: 'Today' },
  { key: 'last7', label: 'Last 7 Days' },
  { key: 'last30', label: 'Last 30 Days' },
  { key: 'thisMonth', label: 'This Month' },
  { key: 'lastMonth', label: 'Last Month' },
  { key: 'custom', label: 'Custom' },
];

const toISODate = (d) => {
  const dt = new Date(d);
  const yyyy = dt.getFullYear();
  const mm = String(dt.getMonth() + 1).padStart(2, '0');
  const dd = String(dt.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

const getMonthStart = (date) => {
  const d = new Date(date);
  d.setDate(1);
  d.setHours(0, 0, 0, 0);
  return d;
};

const getMonthEnd = (date) => {
  const d = new Date(date);
  d.setMonth(d.getMonth() + 1);
  d.setDate(0); // last day of previous month
  d.setHours(0, 0, 0, 0);
  return d;
};

const applyQuickRange = (key, today = new Date()) => {
  const now = new Date(today);
  const end = new Date(now);
  end.setHours(0, 0, 0, 0);

  if (key === 'today') {
    const start = new Date(end);
    return { startDate: toISODate(start), endDate: toISODate(end) };
  }

  if (key === 'last7') {
    const start = new Date(end);
    start.setDate(start.getDate() - 7);
    return { startDate: toISODate(start), endDate: toISODate(end) };
  }

  if (key === 'last30') {
    const start = new Date(end);
    start.setDate(start.getDate() - 30);
    return { startDate: toISODate(start), endDate: toISODate(end) };
  }

  if (key === 'thisMonth') {
    const start = getMonthStart(end);
    const endDate = end;
    return { startDate: toISODate(start), endDate: toISODate(endDate) };
  }

  if (key === 'lastMonth') {
    const first = getMonthStart(end);
    first.setMonth(first.getMonth() - 1);
    const last = getMonthEnd(end);
    // lastMonth end = day before this month start
    const lastMonthEnd = new Date(getMonthStart(end));
    lastMonthEnd.setDate(lastMonthEnd.getDate() - 1);
    return { startDate: toISODate(first), endDate: toISODate(lastMonthEnd) };
  }

  // custom
  return { startDate: '', endDate: '' };
};

const QuickDateFilters = ({ activeKey, onSelect, disabled }) => {
  return (
    <div className="quick-date-filters" aria-label="Quick date filters">
      {QUICK_OPTIONS.map((opt) => {
        const isActive = activeKey === opt.key;
        return (
          <button
            type="button"
            key={opt.key}
            className={`quick-date-pill ${isActive ? 'active' : ''}`}
            disabled={disabled}
            onClick={() => {
              if (disabled) return;
              const next = applyQuickRange(opt.key);
              onSelect(opt.key, next.startDate, next.endDate);
            }}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
};

export { applyQuickRange };
export default QuickDateFilters;

