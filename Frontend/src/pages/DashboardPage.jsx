import { useAuth } from '../auth/useAuth';

export default function DashboardPage() {
  const { user } = useAuth();
  const displayName = user?.displayName || user?.username || 'Operator';

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Welcome back, {displayName}</h1>
          <p className="page-subtitle">
            The system is online. Your operational modules are standing by.
          </p>
        </div>
      </header>

      <div className="page-grid">
        <section className="page-card">
          <p className="page-card-label">Operator</p>
          <p className="page-card-value">{user?.username ?? '—'}</p>
          <p className="page-card-note">
            {user?.accountStatus ? `${user.accountStatus}` : 'Active operator'}
          </p>
        </section>
        <section className="page-card">
          <p className="page-card-label">System Status</p>
          <p className="page-card-value">Online</p>
          <p className="page-card-note">
            Authentication verified for this session.
          </p>
        </section>
        <section className="page-card">
          <p className="page-card-label">Modules</p>
          <p className="page-card-value">5</p>
          <p className="page-card-note">
            Dashboard, Tasks, Goals, Progress, Settings.
          </p>
        </section>
      </div>
    </div>
  );
}
