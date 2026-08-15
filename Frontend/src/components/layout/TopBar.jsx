import { useAuth } from '../../auth/useAuth';
import { Link, useNavigate } from 'react-router-dom';
import { MenuIcon, LogoutIcon } from './icons';

export default function TopBar({ title, onToggleSidebar }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const displayName = user?.displayName || user?.username || 'Operator';
  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate('/', { replace: true });
    }
  };

  return (
    <header className="app-topbar">
      <button
        type="button"
        className="app-menu-btn"
        onClick={onToggleSidebar}
        aria-label="Toggle navigation"
      >
        <MenuIcon />
      </button>
      <div className="app-topbar-title">{title}</div>
      <div className="app-topbar-spacer" />
      <div className="app-topbar-right">
        <Link to="/profile" className="app-user">
          <div className="app-user-avatar">
            {displayName.charAt(0)}
          </div>
          <div className="app-user-meta">
            <span className="app-user-name">{displayName}</span>
            {user?.username && (
              <span className="app-user-id">@{user.username}</span>
            )}
          </div>
        </Link>
        <button type="button" className="app-logout-btn" onClick={handleLogout}>
          <LogoutIcon width={15} height={15} />
          <span>Logout</span>
        </button>
      </div>
    </header>
  );
}
