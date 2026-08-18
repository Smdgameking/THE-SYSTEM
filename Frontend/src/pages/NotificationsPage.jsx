import { useEffect, useState } from 'react';
import { getNotifications, markNotificationRead, markAllNotificationsRead, deleteNotification } from '../api/notificationApi';
import './notifications.css';

function formatDate(iso) {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function formatDateTime(iso) {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

function typeLabel(type) {
  switch (type) {
    case 'TASK_COMPLETED':
      return 'Task completed';
    case 'GOAL_COMPLETED':
      return 'Goal completed';
    case 'ACHIEVEMENT_UNLOCKED':
      return 'Achievement unlocked';
    case 'LEVEL_UP':
      return 'Level up';
    case 'STREAK_MILESTONE':
      return 'Streak milestone';
    default:
      return type;
  }
}

export default function NotificationsPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [items, setItems] = useState([]);
  const [actionId, setActionId] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    const result = await getNotifications();
    if (result.status === 401) {
      setError('Unauthorized');
    } else if (!result.ok || result.data?.success === false) {
      setError(result.data?.error?.message || 'Failed to load notifications');
    } else {
      setItems(result.data?.data || []);
    }
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const onMarkRead = async (id) => {
    setActionId(id);
    const result = await markNotificationRead(id);
    if (result.ok && result.data?.success !== false) {
      setItems((prev) =>
        prev.map((item) =>
          item.id === id ? { ...item, read: true, readAt: new Date().toISOString() } : item
        )
      );
    }
    setActionId(null);
  };

  const onMarkAllRead = async () => {
    setLoading(true);
    const result = await markAllNotificationsRead();
    if (result.ok && result.data?.success !== false) {
      setItems((prev) =>
        prev.map((item) => ({ ...item, read: true, readAt: new Date().toISOString() }))
      );
    }
    setLoading(false);
  };

  const onDelete = async (id) => {
    setActionId(id);
    const result = await deleteNotification(id);
    if (result.ok && result.data?.success !== false) {
      setItems((prev) => prev.filter((item) => item.id !== id));
    }
    setActionId(null);
  };

  const unreadCount = items.filter((item) => !item.read).length;

  return (
    <div className="notifications-page">
      <div className="notifications-header">
        <div>
          <h1 className="notifications-title">Notifications</h1>
          <p className="notifications-subtitle">
            {unreadCount > 0 ? `${unreadCount} unread` : 'All caught up'}
          </p>
        </div>
        {unreadCount > 0 && (
          <button className="notification-btn-primary" onClick={onMarkAllRead} disabled={loading}>
            Mark all as read
          </button>
        )}
      </div>

      {error && <p className="notifications-error">{error}</p>}

      {loading ? (
        <p className="notifications-empty">Loading...</p>
      ) : items.length === 0 ? (
        <div className="notifications-empty">
          <p>No notifications yet.</p>
          <p className="notifications-empty-sub">Complete tasks, goals, and achievements to see updates here.</p>
        </div>
      ) : (
        <ul className="notification-list">
          {items.map((item) => (
            <li key={item.id} className={`notification-item${item.read ? ' is-read' : ''}`}>
              <div className="notification-main">
                <div className="notification-meta">
                  <span className="notification-type">{typeLabel(item.type)}</span>
                  <span className="notification-date">{formatDateTime(item.createdAt)}</span>
                </div>
                <p className="notification-title">{item.title}</p>
                <p className="notification-message">{item.message}</p>
              </div>
              <div className="notification-actions">
                {!item.read && (
                  <button
                    className="notification-btn"
                    onClick={() => onMarkRead(item.id)}
                    disabled={actionId === item.id}
                  >
                    Mark read
                  </button>
                )}
                <button
                  className="notification-btn"
                  onClick={() => onDelete(item.id)}
                  disabled={actionId === item.id}
                >
                  Dismiss
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
