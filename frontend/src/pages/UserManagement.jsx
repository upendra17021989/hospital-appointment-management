import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { LoadingSpinner, EmptyState, Icon } from '../components/Common';
import RoleGuard from '../components/RoleGuard';
import { useAuth } from '../context/AuthContext';

const UserManagement = ({ onNavigate }) => {
  const { user } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [deleteUser, setDeleteUser] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState('');
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    role: 'STAFF',
    password: '',
  });

  useEffect(() => {
    loadUsers();
  }, []);

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3500); };

  const loadUsers = async () => {
    try {
      setLoading(true);
      const response = await api.get('/users');
      if (response && response.data && response.data.length) {
        setUsers(response.data);
      } else if (Array.isArray(response)) {
        setUsers(response);
      } else {
        console.error('Failed to load users:', response);
      }
    } catch (error) {
      console.error('Failed to load users:', error);
    } finally {
      setLoading(false);
    }
  };

  const filtered = users.filter(u => {
    const q = search.toLowerCase();
    return !search
      || u.firstName?.toLowerCase().includes(q)
      || u.lastName?.toLowerCase().includes(q)
      || u.email?.toLowerCase().includes(q)
      || u.role?.toLowerCase().includes(q)
      || u.hospital?.name?.toLowerCase().includes(q);
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const userData = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        role: formData.role,
        ...(formData.password && { password: formData.password }),
      };

      let response;
      if (editingUser) {
        response = await api.put(`/users/${editingUser.id}`, userData);
      } else {
        response = await api.post('/users', userData);
      }

      if (response.success) {
        setShowModal(false);
        setEditingUser(null);
        resetForm();
        loadUsers();
        showToast(editingUser ? 'User updated successfully.' : 'User created successfully.');
      } else {
        console.error('Failed to save user:', response.message);
        alert('Failed to save user: ' + response.message);
      }
    } catch (error) {
      console.error('Failed to save user:', error);
      alert('Failed to save user. Please try again.');
    }
  };

  const handleDeactivate = async (targetUser) => {
    const newStatus = !targetUser.isActive;
    const action = newStatus ? 'activate' : 'deactivate';
    if (!confirm(`Are you sure you want to ${action} this user?`)) return;
    try {
      await api.put(`/users/${targetUser.id}`, { isActive: newStatus });
      loadUsers();
      showToast(`User ${action}d successfully.`);
    } catch (error) {
      console.error(`Failed to ${action} user:`, error);
      alert(`Failed to ${action} user. Please try again.`);
    }
  };

  const handleDelete = async () => {
    if (!deleteUser) return;
    setDeleting(true);
    try {
      await api.delete(`/users/${deleteUser.id}`);
      setDeleteUser(null);
      loadUsers();
      showToast('User deleted successfully.');
    } catch (error) {
      console.error('Failed to delete user:', error);
      alert('Failed to delete user. Please try again.');
    } finally {
      setDeleting(false);
    }
  };

  const resetForm = () => {
    setFormData({
      firstName: '',
      lastName: '',
      email: '',
      role: 'STAFF',
      password: '',
    });
  };

  const openCreateModal = () => {
    resetForm();
    setEditingUser(null);
    setShowModal(true);
  };

  const openEditModal = (user) => {
    setFormData({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      password: '',
    });
    setEditingUser(user);
    setShowModal(true);
  };

  if (loading) return <LoadingSpinner />;

  return (
    <RoleGuard roles={['HOSPITAL_ADMIN', 'SUPER_ADMIN']}>
      <div>
        {/* Toast */}
        {toast && (
          <div className="dm-toast">✓ {toast}</div>
        )}

        <div className="page-header">
          <div>
            <h1 className="page-title">User Management</h1>
            <p className="page-subtitle">Manage hospital staff and administrators</p>
          </div>
          <button className="btn btn-primary" onClick={openCreateModal}>
            <Icon name="plus" /> Add User
          </button>
        </div>

        {/* Search */}
        <div className="search-bar" style={{ marginBottom: 20 }}>
          <div className="search-input-wrap" style={{ flex: 1 }}>
            <span className="search-icon">🔍</span>
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search by name, email, role or hospital..."
            />
          </div>
          {search && (
            <button className="btn btn-secondary btn-sm" onClick={() => setSearch('')}>
              Clear
            </button>
          )}
        </div>

        {filtered.length === 0 ? (
          <EmptyState icon="👤" title="No users found" subtitle="Try adjusting your search or add a new user." />
        ) : (
          <div className="table-wrap">
            {/* Desktop Table */}
            <table className="users-table-desktop">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Hospital</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(u => (
                  <tr key={u.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div className="dm-avatar">{u.firstName?.[0]}{u.lastName?.[0]}</div>
                        <div>
                          <div style={{ fontWeight: 700 }}>{u.firstName} {u.lastName}</div>
                        </div>
                      </div>
                    </td>
                    <td>{u.email}</td>
                    <td>
                      <span className={`role-badge role-${u.role.toLowerCase()}`}>
                        {u.role.replace('_', ' ')}
                      </span>
                    </td>
                    <td>{u.hospital?.name || '—'}</td>
                    <td>
                      <span className={`status-badge ${u.isActive ? 'active' : 'inactive'}`}>
                        {u.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className="btn btn-sm btn-secondary"
                          onClick={() => openEditModal(u)}
                        >
                          Edit
                        </button>
                        <button
                          className={`btn btn-sm ${u.isActive ? 'btn-danger' : 'btn-secondary'}`}
                          onClick={() => handleDeactivate(u)}
                        >
                          {u.isActive ? 'Deactivate' : 'Activate'}
                        </button>
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => setDeleteUser(u)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Mobile Cards */}
            <div className="users-mobile">
              {filtered.map(u => (
                <div className="user-card" key={u.id}>
                  <div className="user-card-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div className="dm-avatar">{u.firstName?.[0]}{u.lastName?.[0]}</div>
                      <div>
                        <div style={{ fontWeight: 700 }}>{u.firstName} {u.lastName}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{u.email}</div>
                      </div>
                    </div>
                    <span className={`status-badge ${u.isActive ? 'active' : 'inactive'}`}>
                      {u.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <div className="user-card-body">
                    <div className="user-card-row">
                      <span className="user-card-label">Role</span>
                      <span className="user-card-value">
                        <span className={`role-badge role-${u.role.toLowerCase()}`}>
                          {u.role.replace('_', ' ')}
                        </span>
                      </span>
                    </div>
                    <div className="user-card-row">
                      <span className="user-card-label">Hospital</span>
                      <span className="user-card-value">{u.hospital?.name || '—'}</span>
                    </div>
                  </div>
                  <div className="user-card-actions">
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => openEditModal(u)}
                    >
                      ✏️ Edit
                    </button>
                    <button
                      className={`btn btn-sm ${u.isActive ? 'btn-danger' : 'btn-secondary'}`}
                      onClick={() => handleDeactivate(u)}
                    >
                      {u.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => setDeleteUser(u)}
                    >
                      🗑
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* User Modal */}
        {showModal && (
          <div className="modal-overlay" onClick={() => setShowModal(false)}>
            <div className="modal" onClick={e => e.stopPropagation()}>
              <div className="modal-header">
                <h3>{editingUser ? 'Edit User' : 'Add New User'}</h3>
                <button className="modal-close" onClick={() => setShowModal(false)}>×</button>
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="form-row">
                    <div className="form-group">
                      <label>First Name *</label>
                      <input
                        type="text"
                        value={formData.firstName}
                        onChange={e => setFormData({...formData, firstName: e.target.value})}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Last Name *</label>
                      <input
                        type="text"
                        value={formData.lastName}
                        onChange={e => setFormData({...formData, lastName: e.target.value})}
                        required
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label>Email *</label>
                    <input
                      type="email"
                      value={formData.email}
                      onChange={e => setFormData({...formData, email: e.target.value})}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Role *</label>
                    <select
                      value={formData.role}
                      onChange={e => setFormData({...formData, role: e.target.value})}
                      required
                    >
                      <option value="STAFF">Staff</option>
                      <option value="RECEPTIONIST">Receptionist</option>
                      <option value="HOSPITAL_ADMIN">Hospital Admin</option>
                    </select>
                    <small className="form-help">
                      Note: SUPER_ADMIN roles must be assigned via database access for security reasons.
                    </small>
                  </div>

                  {!editingUser && (
                    <div className="form-group">
                      <label>Password *</label>
                      <input
                        type="password"
                        value={formData.password}
                        onChange={e => setFormData({...formData, password: e.target.value})}
                        required={!editingUser}
                        placeholder="Enter password"
                      />
                    </div>
                  )}
                </div>

                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary">
                    {editingUser ? 'Update User' : 'Create User'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Delete Confirm Modal */}
        {deleteUser && (
          <div className="modal-overlay" onClick={() => setDeleteUser(null)}>
            <div className="modal" onClick={e => e.stopPropagation()}>
              <div className="modal-header">
                <h3>Delete User</h3>
                <button className="modal-close" onClick={() => setDeleteUser(null)}>×</button>
              </div>
              <div style={{ textAlign: 'center', padding: '8px 0 24px' }}>
                <div style={{ fontSize: 52, marginBottom: 16 }}>⚠️</div>
                <p style={{ fontSize: 15, marginBottom: 8 }}>
                  Are you sure you want to delete <strong>{deleteUser.firstName} {deleteUser.lastName}</strong>?
                </p>
                <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
                  This will permanently remove the user. This action cannot be undone.
                </p>
              </div>
              <div style={{ display: 'flex', gap: 10, justifyContent: 'center', paddingBottom: 24 }}>
                <button className="btn btn-secondary" onClick={() => setDeleteUser(null)}>Cancel</button>
                <button className="btn btn-danger" disabled={deleting} onClick={handleDelete}>
                  {deleting ? 'Deleting...' : 'Yes, Delete'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </RoleGuard>
  );
};

export default UserManagement;

