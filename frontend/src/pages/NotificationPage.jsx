import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import apiClient from '../services/apiClient';
import { API_ENDPOINTS } from '../constants/jobConfig';
import '../styles/NotificationPage.css';

const NOTIFICATION_TYPE_LABELS = {
  JOB_ASSIGNED: 'Assigned Job',
  JOB_RESCHEDULED: 'Rescheduled Job',
  JOB_READY_FOR_CONFIRMATION: 'Job Ready for Confirmation',
  JOB_CONFIRMED: 'Job Confirmed',
};

const NAVIGABLE_TYPES = ['JOB_ASSIGNED', 'JOB_READY_FOR_CONFIRMATION'];

function getActionHint(type) {
  if (type === 'JOB_ASSIGNED') return 'Click to view job details';
  if (type === 'JOB_READY_FOR_CONFIRMATION') return 'Click to review and confirm';
  return null;
}

export default function NotificationPage() {
  const navigate = useNavigate();
  const { auth, logout } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const { decrementUnread, clearUnread, refreshUnreadCount } = useNotifications();

  useEffect(() => {
    if (!auth?.token) {
      navigate('/login');
      return;
    }
    apiClient.setToken(auth.token);
    fetchNotifications();
    refreshUnreadCount();
  }, [auth?.token, navigate, refreshUnreadCount]);

  const fetchNotifications = async () => {
    try {
      const data = await apiClient.get(API_ENDPOINTS.NOTIFICATIONS);
      setNotifications(data);
    } catch (err) {
      console.error('Failed to fetch notifications:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (notificationId) => {
    try {
      await apiClient.put(`${API_ENDPOINTS.NOTIFICATIONS}/${notificationId}/read`, {});
      const wasUnread = notifications.some((n) => n.id === notificationId && !n.isRead);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notificationId ? { ...n, isRead: true } : n))
      );
      if (wasUnread) {
        decrementUnread();
      }
      return true;
    } catch (err) {
      console.error('Failed to mark notification as read:', err);
      return false;
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await apiClient.put(`${API_ENDPOINTS.NOTIFICATIONS}/read-all`, {});
      // Immediately update local state without refetching
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true }))
      );
      clearUnread();
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (loading) {
    return <div className="loading">Loading notifications...</div>;
  }

  function formatNotificationType(type) {
    return NOTIFICATION_TYPE_LABELS[type] || type.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
  }

  return (
    <div className="notification-page">
      <header className="notification-header">
        <button className="back-btn" onClick={() => navigate('/')}>
          ← Back to Dashboard
        </button>
        <h1>Notifications</h1>
        {notifications.length > 0 && (
          <button className="mark-all-btn" onClick={handleMarkAllAsRead}>
            Mark All as Read
          </button>
        )}
        <button className="logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </header>

      <main className="notification-content">
        {notifications.length === 0 ? (
          <div className="no-notifications">
            <p>You have no notifications.</p>
          </div>
        ) : (
          <div className="notification-list">
            {notifications.map((notif) => {
              const actionHint = getActionHint(notif.type);

              return (
                <div
                  key={notif.id}
                  className={`notification-item ${notif.isRead ? 'read' : 'unread'} clickable`}
                  onClick={() => handleNotificationClick(notif)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      handleNotificationClick(notif);
                    }
                  }}
                  role="button"
                  tabIndex={0}
                >
                  <div className="notification-content">
                    <div className="notification-header-row">
                      <span className="notification-type">{formatNotificationType(notif.type)}</span>
                      <span className="notification-time">
                        {new Date(notif.createdAt).toLocaleString()}
                      </span>
                    </div>
                    <p className="notification-message">{notif.message}</p>
                    {actionHint && (
                      <p className="notification-action-hint">{actionHint}</p>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
