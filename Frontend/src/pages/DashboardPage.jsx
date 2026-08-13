import { useAuth } from '../auth/useAuth';

export default function DashboardPage() {
  const { user, logout } = useAuth();

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '40px 20px' }}>
      <h1 style={{ fontSize: '28px', marginBottom: '16px', color: '#333' }}>
        Welcome to THE SYSTEM
      </h1>
      {user?.email && (
        <p style={{ fontSize: '16px', color: '#666', marginBottom: '24px' }}>
          Signed in as <strong>{user.email}</strong>
        </p>
      )}
      <button
        onClick={logout}
        style={{
          padding: '10px 20px',
          backgroundColor: '#333',
          color: '#fff',
          border: 'none',
          borderRadius: '4px',
          fontSize: '14px',
          cursor: 'pointer',
        }}
      >
        Logout
      </button>
    </div>
  );
}
