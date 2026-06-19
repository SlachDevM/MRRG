import { useState } from 'react';
import userApi from '../services/userApi';

export default function EditUserModal({ isOpen, onClose, onSuccess, user }) {
  const [formData, setFormData] = useState({
    name: user?.name || '',
    email: user?.email || '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  if (!isOpen || !user) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      setError(null);
      setMessage(null);
      const result = await userApi.updateUser(user.id, formData);

      // Show additional message if email changed and user is pending
      if (formData.email !== user.email && user.status === 'PENDING_ACTIVATION') {
        setMessage(
          'User updated successfully. A new activation link has been sent to the new email address.'
        );
      } else if (formData.email !== user.email) {
        setMessage(
          'User updated. The new email address has been notified of this change.'
        );
      }

      setTimeout(() => onSuccess(result), 1500);
    } catch (err) {
      setError(err.message || 'Failed to update user');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>Edit User</h2>
        {error && <div className="error-message">{error}</div>}
        {message && <div className="success-message">{message}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Name</label>
            <input
              id="name"
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group readonly">
            <label>Role</label>
            <input
              type="text"
              value={user.role}
              disabled
              className="readonly-field"
            />
            <small>Role cannot be changed here</small>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn-cancel"
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={loading}
            >
              {loading ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
