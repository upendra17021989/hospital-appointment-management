import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { LoadingSpinner, EmptyState, Icon } from '../components/Common';
import RoleGuard from '../components/RoleGuard';
import { useAuth } from '../context/AuthContext';

const UserManagement = ({ onNavigate }) => {
  const { user } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
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

  const loadUsers = async () => {
    try {
      setLoading(true);
      const response = await api.get('/users');
      if (response && response.length) {
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const userData = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        role: formData.role,
        ...(formData.password && { password: formData.password }), // Only include password if provided
      };

      let response;
      if (editingUser) {
        // Update existing user
        response = await api.put(`/users/${editingUser.id}`, userData);
      } else {
        // Create new user
        response = await api.post('/users', userData);
      }

      if (response.success) {
        setShowModal(false);
        setEditingUser(null);
        resetForm();
        loadUsers();
      } else {
        console.error('Failed to save user:', response.message);
        alert('Failed to save user: ' + response.message);
      }
    } catch (error) {
      console.error('Failed to save user:', error);
      alert('Failed to save user. Please try again.');
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
      password: '', // Don't populate password for security
    });
    setEditingUser(user);
    setShowModal(true);
  };

  if (loading) return <LoadingSpinner />;

  return (
    <RoleGuard roles={['HOSPITAL_ADMIN', 'SUPER_ADMIN']}>
      <div>
        <div className="page-header">
          <div>
            <h1 className="page-title">User Management</h1>
            <p className="page-subtitle">Manage hospital staff and administrators</p>
          </div>
          <button className="btn btn-primary" onClick={openCreateModal}>
            <Icon name="plus" /> Add User
          </button>
        </div>

        <div className="card">
          <div className="table-container">
            <table className="data-table">
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
                {users.map(user => (
                  <tr key={user.id}>
                    <td>{user.firstName} {user.lastName}</td>
                    <td>{user.email}</td>
                    <td>
                      <span className={`role-badge role-${user.role.toLowerCase()}`}>
                        {user.role.replace('_', ' ')}
                      </span>
                    </td>
                    <td>{user.hospital?.name}</td>
                    <td>
                      <span className={`status-badge ${user.isActive ? 'active' : 'inactive'}`}>
                        {user.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className="btn btn-sm btn-secondary"
                          onClick={() => openEditModal(user)}
                        >
                          Edit
                        </button>
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => {
                            if (confirm('Are you sure you want to deactivate this user?')) {
                              // Handle deactivation
                            }
                          }}
                        >
                          Deactivate
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

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
      </div>
    </RoleGuard>
  );
};

export default UserManagement;