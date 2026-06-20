import React, { useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { consultationReceiptApi, printBlob, saveBlob } from '../services/receiptApi';
import { Badge, EmptyState, LoadingSpinner, Modal, PageHeader, Tabs } from '../components/Common';
import { useRole } from '../hooks/useRole';

const paymentModes = ['CASH', 'UPI', 'CARD', 'NET_BANKING', 'OTHER'];
const emptyLine = { particulars: 'Consultation Fee', amount: '' };

const formatMoney = (value) => `Rs ${Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const formatDateTime = (value) => value ? new Date(value).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-';
const today = () => new Date().toISOString().slice(0, 10);

const StatCard = ({ label, value }) => (
  <div className="stat-card">
    <div className="stat-value">{value}</div>
    <div className="stat-label">{label}</div>
  </div>
);

const ConsultationReceipts = () => {
  const { user, role, hasAnyRole } = useRole();
  const canCreate = hasAnyRole(['STAFF', 'RECEPTIONIST', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']);
  const canReport = hasAnyRole(['ACCOUNTANT', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']);
  const canVoid = hasAnyRole(['HOSPITAL_ADMIN', 'SUPER_ADMIN']);

  const [activeTab, setActiveTab] = useState('dashboard');
  const [stats, setStats] = useState({ todayReceipts: 0, todayCollection: 0, pendingReceipts: 0 });
  const [receipts, setReceipts] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 1, totalElements: 0, size: 10 });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [viewReceipt, setViewReceipt] = useState(null);
  const [filters, setFilters] = useState({
    receiptNumber: '',
    patientName: '',
    doctorName: '',
    paymentMode: '',
    receiptStatus: '',
    startDate: '',
    endDate: '',
  });
  const [sort, setSort] = useState({ sortBy: 'receiptDateTime', sortDirection: 'DESC' });

  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [form, setForm] = useState({
    patientId: '',
    doctorId: '',
    appointmentId: '',
    paymentMode: 'CASH',
    amountPaid: '',
    paymentReference: '',
    receivedByName: user?.fullName || '',
    lineItems: [{ ...emptyLine }],
  });
  const [saving, setSaving] = useState(false);
  const [existingAppointmentReceipt, setExistingAppointmentReceipt] = useState(null);
  const [checkingAppointmentReceipt, setCheckingAppointmentReceipt] = useState(false);

  const [reportFilters, setReportFilters] = useState({ startDate: today(), endDate: today() });
  const [report, setReport] = useState({ dailyCollection: [], paymentModeWiseCollection: [], doctorWiseCollection: [] });

  const selectedDoctor = useMemo(() => doctors.find(d => d.id === form.doctorId), [doctors, form.doctorId]);
  const selectedPatient = useMemo(() => patients.find(p => p.id === form.patientId), [patients, form.patientId]);

  const loadDashboard = async (page = pageInfo.number) => {
    setLoading(true);
    try {
      const [nextStats, list] = await Promise.all([
        consultationReceiptApi.stats(),
        consultationReceiptApi.list({ ...filters, ...sort, page, size: pageInfo.size }),
      ]);
      setStats(nextStats || {});
      setReceipts(list?.content || []);
      setPageInfo({
        number: list?.number ?? page,
        totalPages: list?.totalPages ?? 1,
        totalElements: list?.totalElements ?? 0,
        size: list?.size ?? pageInfo.size,
      });
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sort]);

  useEffect(() => {
    if (!canCreate) return;
    Promise.all([
      api.get('/patients/hospital').catch(() => []),
      api.get('/doctors/hospital/list').catch(() => []),
    ]).then(([p, d]) => {
      setPatients(p || []);
      setDoctors(d || []);
    });
  }, [canCreate]);

  useEffect(() => {
    if (!form.patientId) {
      setAppointments([]);
      return;
    }
    api.get(`/appointments/hospital?patientId=${form.patientId}`)
      .then(setAppointments)
      .catch(() => setAppointments([]));
  }, [form.patientId]);

  useEffect(() => {
    if (!selectedDoctor?.consultationFee) return;
    const fee = Number(selectedDoctor.consultationFee).toFixed(2);
    setForm(f => ({
      ...f,
      amountPaid: f.amountPaid || fee,
      lineItems: f.lineItems?.length
        ? f.lineItems.map((li, idx) => idx === 0 && !li.amount ? { ...li, amount: fee } : li)
        : [{ particulars: 'Consultation Fee', amount: fee }],
    }));
  }, [selectedDoctor]);

  useEffect(() => {
    if (!form.appointmentId) {
      setExistingAppointmentReceipt(null);
      return;
    }

    let cancelled = false;
    setCheckingAppointmentReceipt(true);
    consultationReceiptApi.activeByAppointment(form.appointmentId)
      .then((receipt) => {
        if (!cancelled) setExistingAppointmentReceipt(receipt || null);
      })
      .catch(() => {
        if (!cancelled) setExistingAppointmentReceipt(null);
      })
      .finally(() => {
        if (!cancelled) setCheckingAppointmentReceipt(false);
      });

    return () => { cancelled = true; };
  }, [form.appointmentId]);

  const loadReport = async () => {
    try {
      const data = await consultationReceiptApi.report(reportFilters);
      setReport(data || { dailyCollection: [], paymentModeWiseCollection: [], doctorWiseCollection: [] });
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    }
  };

  useEffect(() => {
    if (activeTab === 'reports' && canReport) loadReport();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  const setFilter = (key) => (e) => setFilters(f => ({ ...f, [key]: e.target.value }));
  const setField = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.value }));

  const sortBy = (field) => {
    setSort(s => ({
      sortBy: field,
      sortDirection: s.sortBy === field && s.sortDirection === 'ASC' ? 'DESC' : 'ASC',
    }));
  };

  const createReceipt = async (shouldPrint) => {
    if (existingAppointmentReceipt) {
      setMessage(`Receipt ${existingAppointmentReceipt.receiptNumber} already exists for this appointment. Use Print or PDF on the existing receipt.`);
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      const amountPaid = Number(form.amountPaid || 0);
      const lineItems = form.lineItems.map(li => ({
        particulars: li.particulars,
        amount: Number(li.amount || 0),
      }));

      const payment = await api.post('/consultation-payments', {
        appointmentId: form.appointmentId,
        amountPaid,
        consultationFee: amountPaid,
        paymentMode: form.paymentMode,
        paymentReference: form.paymentReference,
        receivedByName: form.receivedByName || user?.fullName || 'Staff',
        lineItems,
      });

      const receipt = await consultationReceiptApi.create({
        consultationPaymentId: payment?.data?.consultationPaymentId,
        amountPaid,
        receivedByName: form.receivedByName || user?.fullName || 'Staff',
        paymentMode: form.paymentMode,
        paymentReference: form.paymentReference,
        lineItems,
      });

      const saved = receipt?.data;
      setMessage(`Receipt ${saved?.receiptNumber || ''} saved successfully`);
      setForm({
        patientId: '',
        doctorId: '',
        appointmentId: '',
        paymentMode: 'CASH',
        amountPaid: '',
        paymentReference: '',
        receivedByName: user?.fullName || '',
        lineItems: [{ ...emptyLine }],
      });
      setExistingAppointmentReceipt(null);
      await loadDashboard(0);
      if (shouldPrint && saved?.id) {
        const blob = await consultationReceiptApi.downloadById(saved.id);
        printBlob(blob);
      }
      setActiveTab('dashboard');
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setSaving(false);
    }
  };

  const updateLine = (idx, key, value) => {
    setForm(f => ({
      ...f,
      lineItems: f.lineItems.map((li, i) => i === idx ? { ...li, [key]: value } : li),
    }));
  };

  const addLine = () => setForm(f => ({ ...f, lineItems: [...f.lineItems, { particulars: '', amount: '' }] }));
  const removeLine = (idx) => setForm(f => ({ ...f, lineItems: f.lineItems.filter((_, i) => i !== idx) }));
  const lineTotal = form.lineItems.reduce((sum, li) => sum + Number(li.amount || 0), 0);
  const formValid = form.patientId
    && form.doctorId
    && form.appointmentId
    && Number(form.amountPaid) > 0
    && Math.round(lineTotal * 100) === Math.round(Number(form.amountPaid || 0) * 100)
    && !existingAppointmentReceipt
    && !checkingAppointmentReceipt;

  const downloadReceipt = async (receipt) => {
    const blob = await consultationReceiptApi.downloadById(receipt.id);
    saveBlob(blob, `${receipt.receiptNumber}.pdf`);
  };

  const printReceipt = async (receipt) => {
    const blob = await consultationReceiptApi.downloadById(receipt.id);
    printBlob(blob);
  };

  const voidReceipt = async (receipt) => {
    if (!window.confirm(`Void receipt ${receipt.receiptNumber}?`)) return;
    await consultationReceiptApi.void(receipt.id);
    await loadDashboard(pageInfo.number);
  };

  const exportReport = async (format) => {
    const blob = await consultationReceiptApi.exportReport({ ...reportFilters, format });
    saveBlob(blob, `consultation-collections.${format === 'excel' ? 'xlsx' : format}`);
  };

  const filteredAppointments = appointments.filter(a => !form.doctorId || a.doctor?.id === form.doctorId);

  return (
    <div>
      <PageHeader
        title="Consultation Receipts"
        subtitle="Create, print, search, void, and report consultation fee receipts"
      >
        {canCreate && (
          <button className="btn btn-primary" onClick={() => setActiveTab('create')}>
            + New Receipt
          </button>
        )}
      </PageHeader>

      {message && (
        <div className={`alert ${message.startsWith('Error') ? 'alert-error' : 'alert-success'}`}>
          {message}
        </div>
      )}

      <Tabs
        active={activeTab}
        onChange={setActiveTab}
        tabs={[
          { value: 'dashboard', label: 'Dashboard' },
          ...(canCreate ? [{ value: 'create', label: 'Create Receipt' }] : []),
          ...(canReport ? [{ value: 'reports', label: 'Reports' }] : []),
        ]}
      />

      {activeTab === 'dashboard' && (
        <>
          <div className="stats-grid">
            <StatCard label="Today's Receipts" value={stats.todayReceipts || 0} />
            <StatCard label="Today's Collection" value={formatMoney(stats.todayCollection)} />
            <StatCard label="Pending Receipts" value={stats.pendingReceipts || 0} />
          </div>

          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-title">Search & Filter</div>
            <div className="form-grid">
              <div className="form-group"><label>Receipt Number</label><input value={filters.receiptNumber} onChange={setFilter('receiptNumber')} /></div>
              <div className="form-group"><label>Patient Name</label><input value={filters.patientName} onChange={setFilter('patientName')} /></div>
              <div className="form-group"><label>Doctor Name</label><input value={filters.doctorName} onChange={setFilter('doctorName')} /></div>
              <div className="form-group"><label>Payment Mode</label><select value={filters.paymentMode} onChange={setFilter('paymentMode')}><option value="">All</option>{paymentModes.map(m => <option key={m} value={m}>{m.replace('_', ' ')}</option>)}</select></div>
              <div className="form-group"><label>Status</label><select value={filters.receiptStatus} onChange={setFilter('receiptStatus')}><option value="">All</option><option value="ACTIVE">Active</option><option value="VOIDED">Voided</option><option value="PENDING">Pending</option></select></div>
              <div className="form-group"><label>Start Date</label><input type="date" value={filters.startDate} onChange={setFilter('startDate')} /></div>
              <div className="form-group"><label>End Date</label><input type="date" value={filters.endDate} onChange={setFilter('endDate')} /></div>
            </div>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 12 }}>
              <button className="btn btn-secondary" onClick={() => setFilters({ receiptNumber: '', patientName: '', doctorName: '', paymentMode: '', receiptStatus: '', startDate: '', endDate: '' })}>Reset</button>
              <button className="btn btn-primary" onClick={() => loadDashboard(0)}>Search</button>
            </div>
          </div>

          <div className="table-wrap table-wrap--scrollable">
            {loading ? <LoadingSpinner /> : receipts.length === 0 ? (
              <EmptyState icon="RC" title="No receipts found" subtitle="Create or adjust filters to see consultation receipts" />
            ) : (
              <table>
                <thead>
                  <tr>
                    <th><button className="link-btn" onClick={() => sortBy('receiptNumber')}>Receipt Number</button></th>
                    <th><button className="link-btn" onClick={() => sortBy('receiptDateTime')}>Receipt Date</button></th>
                    <th><button className="link-btn" onClick={() => sortBy('patientName')}>Patient Name</button></th>
                    <th><button className="link-btn" onClick={() => sortBy('doctorName')}>Doctor Name</button></th>
                    <th><button className="link-btn" onClick={() => sortBy('amountPaid')}>Amount</button></th>
                    <th>Payment Mode</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {receipts.map(r => (
                    <tr key={r.id}>
                      <td><strong>{r.receiptNumber}</strong></td>
                      <td>{formatDateTime(r.receiptDateTime)}</td>
                      <td>{r.patientName}</td>
                      <td>{r.doctorName || '-'}</td>
                      <td>{formatMoney(r.amountPaid)}</td>
                      <td>{r.paymentMode?.replace('_', ' ')}</td>
                      <td><Badge status={(r.receiptStatus || 'ACTIVE').toLowerCase()} /></td>
                      <td>
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                          <button className="btn btn-secondary btn-sm" onClick={() => setViewReceipt(r)}>View</button>
                          <button className="btn btn-secondary btn-sm" disabled={r.receiptStatus === 'VOIDED'} onClick={() => printReceipt(r)}>Print</button>
                          <button className="btn btn-secondary btn-sm" disabled={r.receiptStatus === 'VOIDED'} onClick={() => downloadReceipt(r)}>PDF</button>
                          {canVoid && <button className="btn btn-danger btn-sm" disabled={r.receiptStatus === 'VOIDED'} onClick={() => voidReceipt(r)}>Void</button>}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <div className="pagination">
              <span className="pagination__meta">{pageInfo.totalElements} receipts</span>
              <button className="btn btn-secondary btn-sm" disabled={pageInfo.number <= 0} onClick={() => loadDashboard(pageInfo.number - 1)}>Previous</button>
              <span>Page {pageInfo.number + 1} of {Math.max(pageInfo.totalPages, 1)}</span>
              <button className="btn btn-secondary btn-sm" disabled={pageInfo.number + 1 >= pageInfo.totalPages} onClick={() => loadDashboard(pageInfo.number + 1)}>Next</button>
            </div>
          </div>
        </>
      )}

      {activeTab === 'create' && canCreate && (
        <div className="card">
          <div className="card-title">Create Receipt</div>
          <div className="form-grid">
            <div className="form-group">
              <label>Patient Selection *</label>
              <select value={form.patientId} onChange={e => setForm(f => ({ ...f, patientId: e.target.value, appointmentId: '' }))}>
                <option value="">Select patient</option>
                {patients.map(p => <option key={p.id} value={p.id}>{p.fullName} - {p.phone}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Doctor Selection *</label>
              <select value={form.doctorId} onChange={e => setForm(f => ({ ...f, doctorId: e.target.value, appointmentId: '' }))}>
                <option value="">Select doctor</option>
                {doctors.map(d => <option key={d.id} value={d.id}>{d.fullName} - {d.department?.name || d.specialization}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Appointment *</label>
              <select value={form.appointmentId} onChange={setField('appointmentId')} disabled={!form.patientId}>
                <option value="">Select appointment</option>
                {filteredAppointments.map(a => <option key={a.id} value={a.id}>{a.appointmentDate} {a.appointmentTime} - {a.status}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Payment Mode *</label>
              <select value={form.paymentMode} onChange={setField('paymentMode')}>
                {paymentModes.map(m => <option key={m} value={m}>{m.replace('_', ' ')}</option>)}
              </select>
            </div>
            <div className="form-group"><label>Amount Paid *</label><input type="number" min="0" step="0.01" value={form.amountPaid} onChange={setField('amountPaid')} /></div>
            <div className="form-group"><label>Payment Reference</label><input value={form.paymentReference} onChange={setField('paymentReference')} /></div>
            <div className="form-group"><label>Received By</label><input value={form.receivedByName} onChange={setField('receivedByName')} /></div>
          </div>

          <div style={{ marginTop: 20 }}>
            <div className="table-header">
              <div className="card-title" style={{ margin: 0 }}>Line Items</div>
              <button className="btn btn-secondary btn-sm" onClick={addLine}>+ Add Item</button>
            </div>
            <table>
              <thead><tr><th>#</th><th>Particulars</th><th>Amount</th><th></th></tr></thead>
              <tbody>
                {form.lineItems.map((li, idx) => (
                  <tr key={idx}>
                    <td>{idx + 1}</td>
                    <td><input value={li.particulars} onChange={e => updateLine(idx, 'particulars', e.target.value)} /></td>
                    <td><input type="number" min="0" step="0.01" value={li.amount} onChange={e => updateLine(idx, 'amount', e.target.value)} /></td>
                    <td><button className="btn btn-secondary btn-sm" disabled={form.lineItems.length === 1} onClick={() => removeLine(idx)}>Remove</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div style={{ textAlign: 'right', marginTop: 10, fontWeight: 700 }}>
              Line total: {formatMoney(lineTotal)}
            </div>
            {selectedPatient && selectedDoctor && (
              <div className="alert alert-info" style={{ marginTop: 12 }}>
                Receipt for {selectedPatient.fullName} with {selectedDoctor.fullName}
              </div>
            )}
            {checkingAppointmentReceipt && (
              <div className="alert alert-info" style={{ marginTop: 12 }}>
                Checking whether this appointment already has a receipt...
              </div>
            )}
            {existingAppointmentReceipt && (
              <div className="alert alert-error" style={{ marginTop: 12 }}>
                Receipt {existingAppointmentReceipt.receiptNumber} already exists for this appointment. Create is disabled.
                <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => setViewReceipt(existingAppointmentReceipt)}>
                    View
                  </button>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => printReceipt(existingAppointmentReceipt)}>
                    Print
                  </button>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => downloadReceipt(existingAppointmentReceipt)}>
                    PDF
                  </button>
                </div>
              </div>
            )}
            {Math.round(lineTotal * 100) !== Math.round(Number(form.amountPaid || 0) * 100) && (
              <div className="alert alert-error" style={{ marginTop: 12 }}>
                Line item total must equal Amount Paid.
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 20 }}>
            <button className="btn btn-secondary" disabled={saving || !formValid} onClick={() => createReceipt(false)}>Save</button>
            <button className="btn btn-primary" disabled={saving || !formValid} onClick={() => createReceipt(true)}>
              {saving ? 'Saving...' : 'Save & Print'}
            </button>
          </div>
        </div>
      )}

      {activeTab === 'reports' && canReport && (
        <div className="card">
          <div className="card-title">Collection Reports</div>
          <div className="form-grid">
            <div className="form-group"><label>Start Date</label><input type="date" value={reportFilters.startDate} onChange={e => setReportFilters(f => ({ ...f, startDate: e.target.value }))} /></div>
            <div className="form-group"><label>End Date</label><input type="date" value={reportFilters.endDate} onChange={e => setReportFilters(f => ({ ...f, endDate: e.target.value }))} /></div>
          </div>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginBottom: 16 }}>
            <button className="btn btn-primary" onClick={loadReport}>Run Report</button>
            <button className="btn btn-secondary" onClick={() => exportReport('csv')}>CSV</button>
            <button className="btn btn-secondary" onClick={() => exportReport('excel')}>Excel</button>
            <button className="btn btn-secondary" onClick={() => exportReport('pdf')}>PDF</button>
          </div>
          <div className="grid-3">
            <ReportBlock title="Daily Collection" rows={report.dailyCollection} labelKey="date" />
            <ReportBlock title="Payment Mode Wise" rows={report.paymentModeWiseCollection} labelKey="paymentMode" />
            <ReportBlock title="Doctor Wise" rows={report.doctorWiseCollection} labelKey="doctorName" />
          </div>
        </div>
      )}

      {viewReceipt && (
        <Modal title={`Receipt ${viewReceipt.receiptNumber}`} onClose={() => setViewReceipt(null)}>
          <div style={{ display: 'grid', gap: 10 }}>
            <div><strong>Date:</strong> {formatDateTime(viewReceipt.receiptDateTime)}</div>
            <div><strong>Patient:</strong> {viewReceipt.patientName}</div>
            <div><strong>Doctor:</strong> {viewReceipt.doctorName}</div>
            <div><strong>Payment:</strong> {viewReceipt.paymentMode?.replace('_', ' ')} {viewReceipt.paymentReference ? `- ${viewReceipt.paymentReference}` : ''}</div>
            <div><strong>Status:</strong> {viewReceipt.receiptStatus}</div>
            <table>
              <thead><tr><th>#</th><th>Particulars</th><th>Amount</th></tr></thead>
              <tbody>
                {(viewReceipt.lineItems || []).map((li, idx) => (
                  <tr key={idx}><td>{idx + 1}</td><td>{li.particulars}</td><td>{formatMoney(li.amount)}</td></tr>
                ))}
              </tbody>
            </table>
            <div style={{ textAlign: 'right', fontWeight: 800 }}>Total: {formatMoney(viewReceipt.amountPaid)}</div>
          </div>
        </Modal>
      )}
    </div>
  );
};

const ReportBlock = ({ title, rows, labelKey }) => (
  <div className="card" style={{ background: 'var(--bg)' }}>
    <div className="card-title">{title}</div>
    {rows?.length ? (
      <table>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={idx}>
              <td>{String(row[labelKey] || '-').replace('_', ' ')}</td>
              <td style={{ textAlign: 'right', fontWeight: 700 }}>{formatMoney(row.totalCollected)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    ) : (
      <EmptyState icon="0" title="No collection" />
    )}
  </div>
);

export default ConsultationReceipts;
