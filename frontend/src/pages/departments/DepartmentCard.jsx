import React from 'react';

const DepartmentCard = ({ department, icon, onEdit, onDelete, canEdit }) => (
  <div className="card" style={{ border: '1px solid var(--border)' }}>
    <div style={{ fontSize: 34, marginBottom: 10 }}>{icon}</div>

    <div className="card-title">{department.name}</div>
    <p style={{ color: 'var(--text-muted)', fontSize: 13, marginBottom: 14 }}>
      {department.description || 'No description available.'}
    </p>

    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
      <div style={{ background: 'var(--bg)', padding: '8px 10px', borderRadius: 8 }}>
        <div
          style={{
            fontSize: 11,
            color: 'var(--text-muted)',
            textTransform: 'uppercase',
            letterSpacing: 0.8,
          }}
        >
          Floor
        </div>
        <div style={{ fontWeight: 700 }}>{department.floorNumber ?? 'N/A'}</div>
      </div>
      <div style={{ background: 'var(--bg)', padding: '8px 10px', borderRadius: 8 }}>
        <div
          style={{
            fontSize: 11,
            color: 'var(--text-muted)',
            textTransform: 'uppercase',
            letterSpacing: 0.8,
          }}
        >
          Doctors
        </div>
        <div style={{ fontWeight: 700 }}>{department.doctorCount ?? 0}</div>
      </div>
    </div>

    {canEdit && (
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <button className="btn btn-secondary btn-sm" onClick={() => onEdit(department)}>
          Edit
        </button>
        <button className="btn btn-danger btn-sm" onClick={() => onDelete(department)}>
          Delete
        </button>
      </div>
    )}
  </div>
);

export default DepartmentCard;

