import { useState } from 'react';
import { useAuth } from '../auth/useAuth';

export default function AuthTestPage() {
  const {
    accessToken,
    refreshToken,
    user,
    loading,
    error,
    authenticated,
    register,
    login,
    refresh,
    logout,
    loadCurrentUser,
  } = useAuth();

  const [registerUsername, setRegisterUsername] = useState('');
  const [registerEmail, setRegisterEmail] = useState('');
  const [registerPassword, setRegisterPassword] = useState('');
  const [loginUsername, setLoginUsername] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [lastResponse, setLastResponse] = useState(null);
  const [lastStatus, setLastStatus] = useState(null);

  const handleRegister = async (e) => {
    e.preventDefault();
    setLastResponse(null);
    setLastStatus(null);
    const result = await register(registerUsername, registerEmail, registerPassword);
    setLastStatus(result.status);
    setLastResponse(result.data);
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setLastResponse(null);
    setLastStatus(null);
    const result = await login(loginUsername, loginPassword);
    setLastStatus(result.status);
    setLastResponse(result.data);
  };

  const handleRefresh = async () => {
    setLastResponse(null);
    setLastStatus(null);
    const result = await refresh();
    setLastStatus(result.status);
    setLastResponse(result.data);
  };

  const handleGetMe = async () => {
    setLastResponse(null);
    setLastStatus(null);
    await loadCurrentUser();
    setLastStatus(200);
    setLastResponse({ message: 'Current user loaded', user });
  };

  const handleLogout = async () => {
    setLastResponse(null);
    setLastStatus(null);
    await logout();
    setLastStatus(200);
    setLastResponse({ message: 'Logged out' });
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px', fontFamily: 'monospace' }}>
      <h1>Auth Test Page</h1>

      <section style={{ marginBottom: '30px' }}>
        <h2>Register</h2>
        <form onSubmit={handleRegister}>
          <div style={{ marginBottom: '10px' }}>
            <label>Username: </label>
            <input
              type="text"
              value={registerUsername}
              onChange={(e) => setRegisterUsername(e.target.value)}
              required
              minLength={3}
              maxLength={50}
              style={{ width: '300px', marginLeft: '10px' }}
            />
          </div>
          <div style={{ marginBottom: '10px' }}>
            <label>Email: </label>
            <input
              type="email"
              value={registerEmail}
              onChange={(e) => setRegisterEmail(e.target.value)}
              required
              style={{ width: '300px', marginLeft: '10px' }}
            />
          </div>
          <div style={{ marginBottom: '10px' }}>
            <label>Password: </label>
            <input
              type="password"
              value={registerPassword}
              onChange={(e) => setRegisterPassword(e.target.value)}
              required
              minLength={8}
              style={{ width: '300px', marginLeft: '10px' }}
            />
          </div>
          <button type="submit" disabled={loading}>Register</button>
        </form>
      </section>

      <section style={{ marginBottom: '30px' }}>
        <h2>Login</h2>
        <form onSubmit={handleLogin}>
          <div style={{ marginBottom: '10px' }}>
            <label>Username: </label>
            <input
              type="text"
              value={loginUsername}
              onChange={(e) => setLoginUsername(e.target.value)}
              required
              style={{ width: '300px', marginLeft: '10px' }}
            />
          </div>
          <div style={{ marginBottom: '10px' }}>
            <label>Password: </label>
            <input
              type="password"
              value={loginPassword}
              onChange={(e) => setLoginPassword(e.target.value)}
              required
              minLength={8}
              style={{ width: '300px', marginLeft: '10px' }}
            />
          </div>
          <button type="submit" disabled={loading}>Login</button>
        </form>
      </section>

      <section style={{ marginBottom: '30px' }}>
        <h2>Authentication Controls</h2>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button onClick={handleRefresh} disabled={loading || !refreshToken}>Refresh Token</button>
          <button onClick={handleGetMe} disabled={loading || !authenticated}>Get Current User</button>
          <button onClick={handleLogout} disabled={loading || !authenticated}>Logout</button>
        </div>
      </section>

      <section style={{ marginBottom: '30px' }}>
        <h2>Authentication State</h2>
        <pre style={{ background: '#f5f5f5', padding: '10px', borderRadius: '4px' }}>
{JSON.stringify(
  {
    authenticated,
    loading,
    error,
    accessToken: accessToken ? `${accessToken.slice(0, 20)}...` : null,
    refreshToken: refreshToken ? `${refreshToken.slice(0, 20)}...` : null,
    user,
  },
  null,
  2
)}
        </pre>
      </section>

      <section style={{ marginBottom: '30px' }}>
        <h2>Last Response</h2>
        <div style={{ marginBottom: '10px' }}>
          <strong>Status: </strong>
          <span style={{ color: lastStatus >= 200 && lastStatus < 300 ? 'green' : 'red' }}>
            {lastStatus ?? 'none'}
          </span>
        </div>
        <pre style={{ background: '#f5f5f5', padding: '10px', borderRadius: '4px', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
          {lastResponse ? JSON.stringify(lastResponse, null, 2) : 'No response yet'}
        </pre>
      </section>
    </div>
  );
}
