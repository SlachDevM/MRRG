import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import '../styles/NotificationPage.css';

const API_BASE = 'http://localhost:4000';

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
    fetchNotifications();
    refreshUnreadCount();
  }, [auth?.token, navigate, refreshUnreadCount]);

  const fetchNotifications = async () => {
    try {
      const response = await fetch(`${API_BASE}/api/notifications`, {
        headers: { Authorization: `Bearer ${auth.token}` },
      });
      const data = await response.json();
      setNotifications(data);
    } catch (err) {
      console.error('Failed to fetch notifications:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (notificationId) => {
    try {
      const response = await fetch(`${API_BASE}/api/notifications/${notificationId}/read`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${auth.token}` },
      });

      if (response.ok) {
        const wasUnread = notifications.some((n) => n.id === notificationId && !n.isRead);
        setNotifications((prev) =>
          prev.map((n) => (n.id === notificationId ? { ...n, isRead: true } : n))
        );
        if (wasUnread) {
          decrementUnread();
        }
        return true;
      }
    } catch (err) {
      console.error('Failed to mark notification as read:', err);
    }
    return false;
  };

  const handleNotificationClick = async (notif) => {
    if (!notif.isRead) {
      await handleMarkAsRead(notif.id);
    }

    if (NAVIGABLE_TYPES.includes(notif.type) && notif.jobId) {
      navigate(`/?jobId=${notif.jobId}`);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      const response = await fetch(`${API_BASE}/api/notifications/read-all`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${auth.token}` },
      });

      if (response.ok) {
        clearUnread();
        fetchNotifications();
      }
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
