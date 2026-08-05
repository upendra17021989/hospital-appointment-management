import React, { useEffect, useId, useRef, useState } from 'react';
import { EmptyState, LoadingSpinner } from './Common';

export const PageHeader = ({ title, subtitle, eyebrow, actions }) => (
  <header className="ui-page-header">
    <div className="ui-page-header-copy">
      {eyebrow && <div className="ui-page-eyebrow">{eyebrow}</div>}
      <h1 className="page-title">{title}</h1>
      {subtitle && <p className="page-subtitle">{subtitle}</p>}
    </div>
    {actions && <div className="ui-page-header-actions">{actions}</div>}
  </header>
);

export const FilterBar = ({ children, actions, ariaLabel = 'Filters' }) => (
  <section className="filter-bar" aria-label={ariaLabel}>
    <div className="filter-bar-fields">{children}</div>
    {actions && <div className="filter-bar-actions">{actions}</div>}
  </section>
);

export const FormField = ({ label, hint, error, required = false, children, className = '' }) => {
  const generatedId = useId();
  const childId = children?.props?.id || generatedId;
  const hintId = hint ? `${childId}-hint` : undefined;
  const errorId = error ? `${childId}-error` : undefined;
  const describedBy = [errorId, hintId].filter(Boolean).join(' ') || undefined;

  return (
    <div className={`form-field ${error ? 'form-field--error' : ''} ${className}`.trim()}>
      <label htmlFor={childId}>
        {label}{required && <span className="form-field-required" aria-hidden="true"> *</span>}
      </label>
      {React.isValidElement(children) && React.cloneElement(children, {
        id: childId,
        'aria-invalid': error ? 'true' : undefined,
        'aria-describedby': describedBy,
        required: required || children.props.required,
      })}
      {error && <div className="form-field-error" id={errorId} role="alert">{error}</div>}
      {hint && <div className="form-field-hint" id={hintId}>{hint}</div>}
    </div>
  );
};

export const DataTable = ({
  columns,
  rows,
  getRowKey = row => row.id,
  loading = false,
  emptyTitle = 'No records found',
  emptySubtitle,
  caption,
  className = '',
}) => (
  <div className="data-table-shell">
    {loading ? <LoadingSpinner /> : rows.length === 0 ? (
      <EmptyState title={emptyTitle} subtitle={emptySubtitle} />
    ) : (
      <div className="data-table-scroll" tabIndex="0" role="region" aria-label={caption || 'Data table'}>
        <table className={`data-table ${className}`.trim()}>
          {caption && <caption className="sr-only">{caption}</caption>}
          <thead>
            <tr>{columns.map(column => <th key={column.key} scope="col">{column.header}</th>)}</tr>
          </thead>
          <tbody>
            {rows.map(row => (
              <tr key={getRowKey(row)}>
                {columns.map(column => <td key={column.key}>{column.render ? column.render(row) : row[column.key]}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )}
  </div>
);

export const Pagination = ({ page, totalPages, totalElements, onChange, disabled = false }) => {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <div className="pagination__meta">
        Page {page + 1} of {totalPages}{totalElements !== undefined ? ` · ${totalElements} records` : ''}
      </div>
      <div className="pagination__actions">
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => onChange(0)} disabled={disabled || page === 0}>First</button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => onChange(page - 1)} disabled={disabled || page === 0}>Previous</button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => onChange(page + 1)} disabled={disabled || page >= totalPages - 1}>Next</button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => onChange(totalPages - 1)} disabled={disabled || page >= totalPages - 1}>Last</button>
      </div>
    </nav>
  );
};

export const ActionMenu = ({ label = 'More actions', items }) => {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);

  useEffect(() => {
    const close = event => {
      if (!rootRef.current?.contains(event.target)) setOpen(false);
    };
    document.addEventListener('pointerdown', close);
    return () => document.removeEventListener('pointerdown', close);
  }, []);

  return (
    <div className="action-menu" ref={rootRef}>
      <button type="button" className="btn btn-secondary btn-sm action-menu-trigger" onClick={() => setOpen(value => !value)} aria-haspopup="menu" aria-expanded={open}>
        {label} <span aria-hidden="true">⋯</span>
      </button>
      {open && (
        <div className="action-menu-popover" role="menu">
          {items.map(item => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={`action-menu-item ${item.danger ? 'action-menu-item--danger' : ''}`}
              disabled={item.disabled}
              onClick={() => { setOpen(false); item.onSelect(); }}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

export const Dialog = ({ title, onClose, children, size = 'md', closeOnBackdrop = true }) => {
  const titleId = useId();
  const dialogRef = useRef(null);
  const returnFocusRef = useRef(null);

  useEffect(() => {
    returnFocusRef.current = document.activeElement;
    const dialog = dialogRef.current;
    dialog?.querySelector('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href]')?.focus();

    const handleKeyDown = event => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key !== 'Tab' || !dialog) return;
      const items = [...dialog.querySelectorAll('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex="-1"])')];
      if (items.length === 0) return;
      const first = items[0];
      const last = items[items.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      returnFocusRef.current?.focus?.();
    };
  }, [onClose]);

  return (
    <div className="modal-overlay" onMouseDown={event => {
      if (closeOnBackdrop && event.target === event.currentTarget) onClose();
    }}>
      <div ref={dialogRef} className={`modal modal--${size}`} role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <div className="modal-header">
          <h2 className="modal-title" id={titleId}>{title}</h2>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close dialog">×</button>
        </div>
        {children}
      </div>
    </div>
  );
};

export const ConfirmDialog = ({ open, title, message, confirmLabel = 'Confirm', cancelLabel = 'Cancel', danger = false, busy = false, onConfirm, onClose }) => {
  if (!open) return null;
  return (
    <Dialog title={title} onClose={onClose} size="sm" closeOnBackdrop={!busy}>
      <p className="confirm-dialog-message">{message}</p>
      <div className="confirm-dialog-actions">
        <button type="button" className="btn btn-secondary" onClick={onClose} disabled={busy}>{cancelLabel}</button>
        <button type="button" className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} onClick={onConfirm} disabled={busy}>
          {busy ? 'Please wait…' : confirmLabel}
        </button>
      </div>
    </Dialog>
  );
};
