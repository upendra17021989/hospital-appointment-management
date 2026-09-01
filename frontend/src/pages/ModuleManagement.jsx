import React, { useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { useToast } from '../context/ToastContext';

const LABELS = {
  APPOINTMENTS: 'Appointments', PATIENTS: 'Patients', CLINICAL: 'Clinical',
  CONSULTATION_BILLING: 'Consultation Billing', REPORTS: 'Reports',
  USER_MANAGEMENT: 'User Management', BILLING_PLANS: 'Billing & Plans',
};

export default function ModuleManagement() {
  const [data, setData] = useState(null);
  const [hospitalId, setHospitalId] = useState('');
  const [saving, setSaving] = useState('');
  const { showToast } = useToast();

  const load = () => api.get('/admin/module-settings').then(setData)
    .catch(e => showToast(e.message, { type: 'error' }));
  useEffect(() => { load(); }, []);

  const hospital = useMemo(() => data?.hospitals?.find(h => h.id === hospitalId), [data, hospitalId]);

  const updateGlobal = async (module, enabled) => {
    setSaving(`global-${module}`);
    try { await api.put(`/admin/module-settings/global/${module}`, { enabled }); await load(); showToast('Global setting updated', { type: 'success' }); }
    catch (e) { showToast(e.message, { type: 'error' }); } finally { setSaving(''); }
  };

  const updateHospital = async (module, value) => {
    setSaving(`${hospitalId}-${module}`);
    try {
      if (value === 'inherit') await api.delete(`/admin/module-settings/hospitals/${hospitalId}/${module}`);
      else await api.put(`/admin/module-settings/hospitals/${hospitalId}/${module}`, { enabled: value === 'enabled' });
      await load(); showToast('Hospital setting updated', { type: 'success' });
    } catch (e) { showToast(e.message, { type: 'error' }); } finally { setSaving(''); }
  };

  if (!data) return <div className="page-container"><div className="spinner" /></div>;
  return (
    <div className="page-container">
      <div className="page-header">
        <div><h1 className="page-title">Module Management</h1><p className="text-muted">Control modules globally or override them for one hospital.</p></div>
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <div className="card-header"><h3>All hospitals</h3></div>
        <div className="card-body" style={{ display: 'grid', gap: 12 }}>
          {data.modules.map(module => (
            <div key={module} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
              <div><strong>{LABELS[module] || module}</strong><div className="text-muted" style={{ fontSize: 12 }}>Default for every hospital</div></div>
              <button disabled={saving === `global-${module}`} className={`btn btn-sm ${data.global[module] ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => updateGlobal(module, !data.global[module])}>{data.global[module] ? 'Enabled' : 'Disabled'}</button>
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="card-header"><h3>Hospital override</h3></div>
        <div className="card-body">
          <label className="form-label" htmlFor="module-hospital">Hospital</label>
          <select id="module-hospital" className="form-control" value={hospitalId} onChange={e => setHospitalId(e.target.value)} style={{ marginBottom: 20 }}>
            <option value="">Select a hospital</option>
            {data.hospitals.map(h => <option key={h.id} value={h.id}>{h.name}{h.city ? ' - ' + h.city : ''}</option>)}
          </select>
          {hospital && <div style={{ display: 'grid', gap: 12 }}>
            {data.modules.map(module => {
              const overridden = Object.prototype.hasOwnProperty.call(hospital.overrides, module);
              const value = overridden ? (hospital.overrides[module] ? 'enabled' : 'disabled') : 'inherit';
              return <div key={module} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
                <div><strong>{LABELS[module] || module}</strong><div className="text-muted" style={{ fontSize: 12 }}>Effective: {hospital.effective[module] ? 'Enabled' : 'Disabled'}</div></div>
                <select className="form-control" style={{ width: 150 }} value={value} disabled={saving === `${hospitalId}-${module}`}
                  onChange={e => updateHospital(module, e.target.value)}>
                  <option value="inherit">Inherit global</option><option value="enabled">Enabled</option><option value="disabled">Disabled</option>
                </select>
              </div>;
            })}
          </div>}
        </div>
      </div>
    </div>
  );
}
