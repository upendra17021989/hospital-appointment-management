import React, { useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { certificateApi, printBlob, saveBlob } from '../services/certificateApi';
import { Badge, EmptyState, LoadingSpinner, Modal, PageHeader, Tabs } from '../components/Common';
import { useRole } from '../hooks/useRole';

const certificateTypes = [
  ['SICK_LEAVE', 'Sick Leave / Absence Certificate'],
  ['FITNESS', 'Fitness Certificate'],
  ['FIT_TO_FLY', 'Fit-to-Fly Certificate'],
  ['FORM_1A_DRIVING_LICENSE', 'Form 1A Driving License Medical Certificate'],
  ['VACCINATION', 'Vaccination Certificate'],
  ['RECOVERY', 'Recovery Certificate'],
  ['CARETAKER_MEDICAL_LEAVE', 'Caretaker / Medical Leave Certificate'],
  ['CARA_ADOPTION_FITNESS', 'CARA Adoption Medical Fitness Certificate'],
];

const typeLabels = Object.fromEntries(certificateTypes);
const today = () => new Date().toISOString().slice(0, 10);
const formatDate = (value) => value ? new Date(value).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-';

const fieldSchemas = {
  SICK_LEAVE: [
    ['fromDate', 'Leave From', 'date', true],
    ['toDate', 'Leave Until', 'date', true],
    ['restDays', 'Rest Days', 'number'],
    ['workRestriction', 'Work Restriction', 'text'],
  ],
  FITNESS: [
    ['fitFor', 'Fit For', 'text'],
    ['fitnessDate', 'Fitness Date', 'date'],
    ['limitations', 'Limitations', 'text'],
  ],
  FIT_TO_FLY: [
    ['flightDate', 'Flight Date', 'date', true],
    ['destination', 'Destination', 'text'],
    ['stableCondition', 'Stable Condition', 'text'],
    ['contraindications', 'Contraindications', 'text'],
  ],
  FORM_1A_DRIVING_LICENSE: [
    ['vision', 'Vision', 'text'],
    ['hearing', 'Hearing', 'text'],
    ['physicalFitness', 'Physical Fitness', 'text'],
    ['identificationMarks', 'Identification Marks', 'text'],
  ],
  VACCINATION: [
    ['vaccineName', 'Vaccine Name', 'text', true],
    ['doseNumber', 'Dose Number', 'text'],
    ['batchNumber', 'Batch Number', 'text'],
    ['vaccinationDate', 'Vaccination Date', 'date'],
  ],
  RECOVERY: [
    ['condition', 'Condition', 'text'],
    ['recoveryDate', 'Recovery Date', 'date'],
    ['isolationFrom', 'Isolation From', 'date'],
    ['isolationTo', 'Isolation To', 'date'],
  ],
  CARETAKER_MEDICAL_LEAVE: [
    ['caretakerName', 'Caretaker Name', 'text'],
    ['relation', 'Relation', 'text'],
    ['fromDate', 'Care From', 'date', true],
    ['toDate', 'Care Until', 'date', true],
  ],
  CARA_ADOPTION_FITNESS: [
    ['generalHealth', 'General Health', 'text'],
    ['infectiousDisease', 'Infectious Disease Status', 'text'],
    ['mentalFitness', 'Mental Fitness', 'text'],
    ['fitnessRemark', 'Fitness Remark', 'text'],
  ],
};

const emptyForm = {
  patientId: '',
  doctorId: '',
  appointmentId: '',
  certificateType: 'SICK_LEAVE',
  issueDate: today(),
  validFrom: '',
  validUntil: '',
  diagnosisOrReason: '',
  remarks: '',
  issuedByName: '',
  dynamicFields: {},
};

const MedicalCertificates = () => {
  const { user, hasAnyRole } = useRole();
  const canVoid = hasAnyRole(['HOSPITAL_ADMIN', 'SUPER_ADMIN']);
  const [activeTab, setActiveTab] = useState('list');
  const [certificates, setCertificates] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 1, totalElements: 0, size: 10 });
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [viewCertificate, setViewCertificate] = useState(null);
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [filters, setFilters] = useState({
    certificateNumber: '',
    patientName: '',
    doctorName: '',
    certificateType: '',
    certificateStatus: '',
    startDate: '',
    endDate: '',
  });
  const [sort, setSort] = useState({ sortBy: 'createdAt', sortDirection: 'DESC' });
  const [form, setForm] = useState({ ...emptyForm, issuedByName: user?.fullName || '' });

  const selectedPatient = useMemo(() => patients.find(p => p.id === form.patientId), [patients, form.patientId]);
  const selectedDoctor = useMemo(() => doctors.find(d => d.id === form.doctorId), [doctors, form.doctorId]);
  const visibleAppointments = appointments.filter(a => !form.doctorId || a.doctor?.id === form.doctorId);

  const loadCertificates = async (page = pageInfo.number) => {
    setLoading(true);
    try {
      const data = await certificateApi.list({ ...filters, ...sort, page, size: pageInfo.size });
      setCertificates(data?.content || []);
      setPageInfo({
        number: data?.number ?? page,
        totalPages: data?.totalPages ?? 1,
        totalElements: data?.totalElements ?? 0,
        size: data?.size ?? pageInfo.size,
      });
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCertificates(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sort]);

  useEffect(() => {
    Promise.all([
      api.get('/patients/hospital').catch(() => []),
      api.get('/doctors/hospital/list').catch(() => []),
    ]).then(([p, d]) => {
      setPatients(p || []);
      setDoctors(d || []);
    });
  }, []);

  useEffect(() => {
    if (!form.patientId) {
      setAppointments([]);
      return;
    }
    api.get(`/appointments/hospital?patientId=${form.patientId}`)
      .then(data => setAppointments(data || []))
      .catch(() => setAppointments([]));
  }, [form.patientId]);

  const setFilter = (key) => (e) => setFilters(f => ({ ...f, [key]: e.target.value }));
  const setField = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.value }));
  const setDynamicField = (key) => (e) => setForm(f => ({
    ...f,
    dynamicFields: { ...f.dynamicFields, [key]: e.target.value },
  }));

  const sortBy = (field) => setSort(s => ({
    sortBy: field,
    sortDirection: s.sortBy === field && s.sortDirection === 'ASC' ? 'DESC' : 'ASC',
  }));

  const createCertificate = async (shouldPrint) => {
    if (!form.patientId || !form.doctorId || !form.certificateType) {
      setMessage('Error: Patient, doctor, and certificate type are required.');
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      const payload = {
        ...form,
        appointmentId: form.appointmentId || null,
        validFrom: form.validFrom || null,
        validUntil: form.validUntil || null,
      };
      const res = await certificateApi.create(payload);
      const saved = res?.data;
      setMessage(`Certificate ${saved?.certificateNumber || ''} created successfully`);
      setForm({ ...emptyForm, issuedByName: user?.fullName || '' });
      await loadCertificates(0);
      if (shouldPrint && saved?.id) {
        const blob = await certificateApi.downloadPdf(saved.id);
        printBlob(blob);
      }
      setActiveTab('list');
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setSaving(false);
    }
  };

  const downloadCertificate = async (certificate) => {
    const blob = await certificateApi.downloadPdf(certificate.id);
    saveBlob(blob, `${certificate.certificateNumber}.pdf`);
  };

  const printCertificate = async (certificate) => {
    const blob = await certificateApi.downloadPdf(certificate.id);
    printBlob(blob);
  };

  const voidCertificate = async (certificate) => {
    if (!window.confirm(`Void certificate ${certificate.certificateNumber}?`)) return;
    await certificateApi.void(certificate.id);
    await loadCertificates(pageInfo.number);
  };

  const resetFilters = () => {
    setFilters({ certificateNumber: '', patientName: '', doctorName: '', certificateType: '', certificateStatus: '', startDate: '', endDate: '' });
  };

  return (
    <div>
      <PageHeader
        title="Medical Certificates"
        subtitle="Create, search, print, and manage dynamic medical certificates"
      >
        <button className="btn btn-primary" onClick={() => setActiveTab('create')}>+ New Certificate</button>
      </PageHeader>

      {message && <div className={`alert ${message.startsWith('Error') ? 'alert-error' : 'alert-success'}`}>{message}</div>}

      <Tabs
        active={activeTab}
        onChange={setActiveTab}
        tabs={[
          { value: 'list', label: 'Certificates' },
          { value: 'create', label: 'Create Certificate' },
        ]}
      />

      {activeTab === 'list' && (
        <>
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="card-title">Search & Filter</div>
            <div className="form-grid">
              <div className="form-group"><label>Certificate Number</label><input value={filters.certificateNumber} onChange={setFilter('certificateNumber')} /></div>
              <div className="form-group"><label>Patient Name</label><input value={filters.patientName} onChange={setFilter('patientName')} /></div>
              <div className="form-group"><label>Doctor Name</label><input value={filters.doctorName} onChange={setFilter('doctorName')} /></div>
              <div className="form-group"><label>Type</label><select value={filters.certificateType} onChange={setFilter('certificateType')}><option value="">All</option>{certificateTypes.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
              <div className="form-group"><label>Status</label><select value={filters.certificateStatus} onChange={setFilter('certificateStatus')}><option value="">All</option><option value="ACTIVE">Active</option><option value="VOIDED">Voided</option></select></div>
              <div className="form-group"><label>Start Date</label><input type="date" value={filters.startDate} onChange={setFilter('startDate')} /></div>
              <div className="form-group"><label>End Date</label><input type="date" value={filters.endDate} onChange={setFilter('endDate')} /></div>
            </div>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 12 }}>
              <button className="btn btn-secondary" onClick={resetFilters}>Reset</button>
              <button className="btn btn-primary" onClick={() => loadCertificates(0)}>Search</button>
            </div>
          </div>

          <div className="table-wrap table-wrap--scrollable">
            {loading ? <LoadingSpinner /> : certificates.length === 0 ? (
              <EmptyState icon="MC" title="No certificates found" subtitle="Create a certificate or adjust filters to continue" />
            ) : (
              <table>
                <thead>
                  <tr>
                    <th><button className="link-btn" onClick={() => sortBy('certificateNumber')}>Certificate No.</button></th>
                    <th><button className="link-btn" onClick={() => sortBy('certificateType')}>Type</button></th>
                    <th><button className="link-btn" onClick={() => sortBy('patientName')}>Patient</button></th>
                    <th><button className="link-btn" onClick={() => sortBy('doctorName')}>Doctor</button></th>
                    <th><button className="link-btn" onClick={() => sortBy('issueDate')}>Issue Date</button></th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {certificates.map(c => (
                    <tr key={c.id}>
                      <td><strong>{c.certificateNumber}</strong></td>
                      <td>{typeLabels[c.certificateType] || c.certificateType}</td>
                      <td>{c.patientName}</td>
                      <td>{c.doctorName}</td>
                      <td>{formatDate(c.issueDate)}</td>
                      <td><Badge status={(c.certificateStatus || 'ACTIVE').toLowerCase()} /></td>
                      <td>
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                          <button className="btn btn-secondary btn-sm" onClick={() => setViewCertificate(c)}>View</button>
                          <button className="btn btn-secondary btn-sm" disabled={c.certificateStatus === 'VOIDED'} onClick={() => printCertificate(c)}>Print</button>
                          <button className="btn btn-secondary btn-sm" disabled={c.certificateStatus === 'VOIDED'} onClick={() => downloadCertificate(c)}>PDF</button>
                          {canVoid && <button className="btn btn-danger btn-sm" disabled={c.certificateStatus === 'VOIDED'} onClick={() => voidCertificate(c)}>Void</button>}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <div className="pagination">
              <span className="pagination__meta">{pageInfo.totalElements} certificates</span>
              <button className="btn btn-secondary btn-sm" disabled={pageInfo.number <= 0} onClick={() => loadCertificates(pageInfo.number - 1)}>Previous</button>
              <span>Page {pageInfo.number + 1} of {Math.max(pageInfo.totalPages, 1)}</span>
              <button className="btn btn-secondary btn-sm" disabled={pageInfo.number + 1 >= pageInfo.totalPages} onClick={() => loadCertificates(pageInfo.number + 1)}>Next</button>
            </div>
          </div>
        </>
      )}

      {activeTab === 'create' && (
        <div className="card">
          <div className="card-title">Create Certificate</div>
          <div className="form-grid">
            <div className="form-group">
              <label>Patient *</label>
              <select value={form.patientId} onChange={e => setForm(f => ({ ...f, patientId: e.target.value, appointmentId: '' }))}>
                <option value="">Select patient</option>
                {patients.map(p => <option key={p.id} value={p.id}>{p.fullName} - {p.phone}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Doctor *</label>
              <select value={form.doctorId} onChange={e => setForm(f => ({ ...f, doctorId: e.target.value, appointmentId: '' }))}>
                <option value="">Select doctor</option>
                {doctors.map(d => <option key={d.id} value={d.id}>{d.fullName} - {d.department?.name || d.specialization}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Appointment</label>
              <select value={form.appointmentId} onChange={setField('appointmentId')} disabled={!form.patientId}>
                <option value="">No linked appointment</option>
                {visibleAppointments.map(a => <option key={a.id} value={a.id}>{a.appointmentDate} {a.appointmentTime} - {a.status}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Certificate Type *</label>
              <select value={form.certificateType} onChange={e => setForm(f => ({ ...f, certificateType: e.target.value, dynamicFields: {} }))}>
                {certificateTypes.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </div>
            <div className="form-group"><label>Issue Date</label><input type="date" value={form.issueDate} onChange={setField('issueDate')} /></div>
            <div className="form-group"><label>Valid From</label><input type="date" value={form.validFrom} onChange={setField('validFrom')} /></div>
            <div className="form-group"><label>Valid Until</label><input type="date" value={form.validUntil} onChange={setField('validUntil')} /></div>
            <div className="form-group"><label>Issued By</label><input value={form.issuedByName} onChange={setField('issuedByName')} /></div>
          </div>

          <div style={{ marginTop: 20 }}>
            <div className="card-title">Certificate Details</div>
            <div className="form-grid">
              {(fieldSchemas[form.certificateType] || []).map(([key, label, type, required]) => (
                <div className="form-group" key={key}>
                  <label>{label}{required ? ' *' : ''}</label>
                  <input type={type} value={form.dynamicFields[key] || ''} onChange={setDynamicField(key)} />
                </div>
              ))}
              <div className="form-group">
                <label>Diagnosis / Reason</label>
                <textarea value={form.diagnosisOrReason} onChange={setField('diagnosisOrReason')} rows={3} />
              </div>
              <div className="form-group">
                <label>Remarks</label>
                <textarea value={form.remarks} onChange={setField('remarks')} rows={3} />
              </div>
            </div>
          </div>

          {(selectedPatient || selectedDoctor) && (
            <div className="alert alert-info" style={{ marginTop: 14 }}>
              {selectedPatient?.fullName || 'Selected patient'} {selectedDoctor ? `with ${selectedDoctor.fullName}` : ''}
            </div>
          )}

          <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 20 }}>
            <button className="btn btn-secondary" disabled={saving} onClick={() => setActiveTab('list')}>Cancel</button>
            <button className="btn btn-secondary" disabled={saving} onClick={() => createCertificate(false)}>Save</button>
            <button className="btn btn-primary" disabled={saving} onClick={() => createCertificate(true)}>{saving ? 'Saving...' : 'Save & Print'}</button>
          </div>
        </div>
      )}

      {viewCertificate && (
        <Modal title={viewCertificate.certificateNumber} onClose={() => setViewCertificate(null)}>
          <div style={{ display: 'grid', gap: 10 }}>
            <div><strong>Type:</strong> {typeLabels[viewCertificate.certificateType] || viewCertificate.certificateType}</div>
            <div><strong>Patient:</strong> {viewCertificate.patientName}</div>
            <div><strong>Doctor:</strong> {viewCertificate.doctorName}</div>
            <div><strong>Issue Date:</strong> {formatDate(viewCertificate.issueDate)}</div>
            <div><strong>Validity:</strong> {formatDate(viewCertificate.validFrom)} to {formatDate(viewCertificate.validUntil)}</div>
            <div><strong>Status:</strong> {viewCertificate.certificateStatus}</div>
            {viewCertificate.diagnosisOrReason && <div><strong>Reason:</strong> {viewCertificate.diagnosisOrReason}</div>}
            {viewCertificate.remarks && <div><strong>Remarks:</strong> {viewCertificate.remarks}</div>}
            {Object.keys(viewCertificate.dynamicFields || {}).length > 0 && (
              <table>
                <tbody>
                  {Object.entries(viewCertificate.dynamicFields).map(([key, value]) => (
                    <tr key={key}><td>{key}</td><td>{String(value || '')}</td></tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
};

export default MedicalCertificates;
