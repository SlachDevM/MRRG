import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../services/apiClient';
import userApi from '../services/userApi';
import NotificationButton from '../components/NotificationButton';
import InviteUserModal from '../components/InviteUserModal';
import EditUserModal from '../components/EditUserModal';
import ConfirmDialog from '../components/ConfirmDialog';
import '../styles/UserManagementPage.css';

const STATUS_LABELS = {
  ACTIVE: 'Active',
  PENDING_ACTIVATION: 'Pending',
  DISABLED: 'Disabled',
};

const STATUS_COLORS = {
  ACTIVE: 'status-active',
  PENDING_ACTIVATION: 'status-pending',
  DISABLED: 'status-disabled',
};

export default function UserManagementPage() {
  const navigate = useNavigate();
  const { auth, logout } = useAuth();

  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Search and filter
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');

  // Modals
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);

  // Confirm dialogs
  const [confirmAction, setConfirmAction] = useState(null);
  const [confirmingUserId, setConfirmingUserId] = useState(null);

  const [actionLoading, setActionLoading] = useState(false);

  // Check authorization
  useEffect(() => {
    if (!auth || (auth.user.role !== 'MANAGER' && auth.user.role !== 'ADMIN')) {
      navigate('/');
      return;
    }
    apiClient.setToken(auth.token);
    fetchUsers();
  }, [auth, navigate]);

  // Filter and search users
  useEffect(() => {
    let result = users;

    // Apply status filter
    if (statusFilter !== 'all') {
      result = result.filter((u) => u.status === statusFilter);
    }

    // Apply search
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      result = result.filter(
        (u) =>
          u.name.toLowerCase().includes(query) ||
          u.email.toLowerCase().includes(query)
      );
    }

    setFilteredUsers(result);
  }, [users, searchQuery, statusFilter]);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await userApi.listUsers();
      setUsers(data);
    } catch (err) {
      setError(`Failed to load users: ${err.message}`);
      console.error('Failed to fetch users:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleInviteSuccess = (newUser) => {
    setUsers((prev) => [newUser, ...prev]);
    setShowInviteModal(false);
  };

  const handleEditUser = (user) => {
    setSelectedUser(user);
    setShowEditModal(true);
  };

  const handleEditSuccess = (updatedUser) => {
    setUsers((prev) =>
      prev.map((u) => (u.id === updatedUser.id ? updatedUser : u))
    );
    setShowEditModal(false);
    setSelectedUser(null);
  };

  const handleDeactivateClick = (user) => {
    setConfirmAction('deactivate');
    setConfirmingUserId(user.id);
  };

  const handleReactivateClick = (user) => {
    setConfirmAction('reactivate');
    setConfirmingUserId(user.id);
  };

  const handleResendActivationClick = (user) => {
    setConfirmAction('resend');
    setConfirmingUserId(user.id);
  };

  const executeConfirmAction = async () => {
    try {
      setActionLoading(true);

      if (confirmAction === 'deactivate') {
        await userApi.deactivateUser(confirmingUserId);
        setUsers((prev) =>
          prev.map((u) =>
            u.id === confirmingUserId ? { ...u, status: 'DISABLED' } : u
          )
        );
      } else if (confirmAction === 'reactivate') {
        await userApi.reactivateUser(confirmingUserId);
        await fetchUsers();
      } else if (confirmAction === 'resend') {
        await userApi.resendActivation(confirmingUserId);
        setUsers((prev) =>
          prev.map((u) =>
            u.id === confirmingUserId ? { ...u, status: 'PENDING_ACTIVATION' } : u
          )
        );
        setError(null);
      }

      setConfirmAction(null);
      setConfirmingUserId(null);
    } catch (err) {
      setError(`Failed to ${confirmAction} user: ${err.message}`);
      console.error(`Failed to ${confirmAction} user:`, err);
    } finally {
      setActionLoading(false);
    }
  };

  const cancelConfirm = () => {
    setConfirmAction(null);
    setConfirmingUserId(null);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isAdmin = auth?.user.role === 'ADMIN';
  const getConfirmMessage = () => {
    if (confirmAction === 'deactivate') {
      return 'Are you sure you want to deactivate this user? They will not be able to log in.';
    } else if (confirmAction === 'reactivate') {
      return 'Reactivate this user?';
    } else if (confirmAction === 'resend') {
      return 'Send a new activation link to this user?';
    }
    return '';
  };

  if (loading) {
    return <div className="loading">Loading users...</div>;
  }

  return (
    <div className="user-management-page">
      <header 
        className="user-header"
        style={{
          background: 'linear-gradient(135deg, #1a472a 0%, #2d6b4d 100%)',
          color: 'white'
        }}
      >
        <button
          type="button"
          className="back-btn"
          onClick={() => navigate('/')}
        >
          ← Back to Dashboard
        </button>

        <h1>User Management</h1>

        <div className="user-header-actions">
          <NotificationButton />

          {isAdmin && (
            <button
              type="button"
              className="invite-btn"
              onClick={() => setShowInviteModal(true)}
            >
              + Invite User
            </button>
          )}

          <button type="button" className="logout-btn" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      {error && <div className="error-message">{error}</div>}

      <main className="user-content">
        <div className="user-controls">
          <input
            type="text"
            placeholder="Search by name or email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="status-filter"
          >
            <option value="all">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="PENDING_ACTIVATION">Pending Activation</option>
            <option value="DISABLED">Disabled</option>
          </select>
        </div>

        <div className="users-table-container">
          {filteredUsers.length === 0 ? (
            <p className="no-users">
              {users.length === 0
                ? 'No users yet.'
                : 'No users matching your search.'}
            </p>
          ) : (
            <table className="users-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Created</th>
                  {isAdmin && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td>{user.name}</td>
                    <td>{user.email}</td>
                    <td>
                      <span className="role-badge">
                        {user.role}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`status-badge ${STATUS_COLORS[user.status]}`}
                      >
                        {STATUS_LABELS[user.status]}
                      </span>
                    </td>
                    <td>
                      {user.createdAt
                        ? new Date(user.createdAt).toLocaleDateString()
                        : '-'}
                    </td>
                    {isAdmin && (
                      <td className="actions-cell">
                        <button
                          type="button"
                          className="action-btn edit-btn"
                          onClick={() => handleEditUser(user)}
                          title="Edit user"
                        >
                          Edit
                        </button>

                        {user.status === 'PENDING_ACTIVATION' && (
                          <>
                            <button
                              type="button"
                              className="action-btn resend-btn"
                              onClick={() => handleResendActivationClick(user)}
                              title="Resend activation link"
                            >
                              Resend
                            </button>
                            <button
                              type="button"
                              className="action-btn deactivate-btn"
                              onClick={() => handleDeactivateClick(user)}
                              title="Deactivate user"
                            >
                              Deactivate
                            </button>
                          </>
                        )}

                        {user.status === 'ACTIVE' && (
                          <button
                            type="button"
                            className="action-btn deactivate-btn"
                            onClick={() => handleDeactivateClick(user)}
                            title="Deactivate user"
                          >
                            Deactivate
                          </button>
                        )}

                        {user.status === 'DISABLED' && (
                          <button
                            type="button"
                            className="action-btn reactivate-btn"
                            onClick={() => handleReactivateClick(user)}
                            title="Reactivate user"
                          >
                            Reactivate
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>

      {showInviteModal && (
        <InviteUserModal
          isOpen={showInviteModal}
          onClose={() => setShowInviteModal(false)}
          onSuccess={handleInviteSuccess}
        />
      )}

      {showEditModal && selectedUser && (
        <EditUserModal
          isOpen={showEditModal}
          onClose={() => {
            setShowEditModal(false);
            setSelectedUser(null);
          }}
          onSuccess={handleEditSuccess}
          user={selectedUser}
        />
      )}

      {confirmAction && (
        <ConfirmDialog
          title={
            confirmAction === 'deactivate'
              ? 'Deactivate User'
              : confirmAction === 'reactivate'
              ? 'Reactivate User'
              : 'Resend Activation'
          }
          message={getConfirmMessage()}
          confirmLabel={
            confirmAction === 'deactivate'
              ? 'Deactivate'
              : confirmAction === 'reactivate'
              ? 'Reactivate'
              : 'Resend'
          }
          onConfirm={executeConfirmAction}
          onCancel={cancelConfirm}
          isLoading={actionLoading}
          isDangerous={confirmAction === 'deactivate'}
        />
      )}
    </div>
  );
}
