import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../context/NotificationContext';
import '../styles/Dashboard.css';

export default function NotificationButton() {
  const navigate = useNavigate();
  const { unreadCount, refreshUnreadCount } = useNotifications();

  useEffect(() => {
    refreshUnreadCount();
  }, [refreshUnreadCount]);

  return (
    <button
      type="button"
      className="notification-btn"
      onClick={() => navigate('/notifications')}
    >
      🔔 Notifications
      {unreadCount > 0 && (
        <span className="notification-badge">
          {unreadCount > 99 ? '99+' : unreadCount}
        </span>
      )}
    </button>
  );
}
