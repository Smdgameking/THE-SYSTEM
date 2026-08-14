import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export default function RegisterPage() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setIsLoading(true);

    const result = await register(username, email, password);
    setIsLoading(false);

    if (result.success) {
      navigate('/dashboard', { replace: true });
    } else {
      const message = result.data?.error?.message || result.data?.message || 'Registration failed';
      setError(message);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #0a0a0f 0%, #0d1117 50%, #0a0a1a 100%)',
      padding: '20px',
      position: 'relative',
      overflow: 'hidden',
    }}>
      <div style={{
        position: 'absolute',
        inset: 0,
        backgroundImage: 'radial-gradient(circle at 50% 30%, rgba(99, 102, 241, 0.1) 0%, transparent 50%)',
        pointerEvents: 'none',
      }} />

      <div style={{
        width: '100%',
        maxWidth: '420px',
        position: 'relative',
        zIndex: 1,
      }}>
        <div style={{
          textAlign: 'center',
          marginBottom: '40px',
        }}>
          <div style={{
            fontSize: '12px',
            letterSpacing: '6px',
            color: '#6366f1',
            marginBottom: '8px',
            textTransform: 'uppercase',
            fontWeight: 600,
          }}>
            SYSTEM INITIALIZATION
          </div>
          <h1 style={{
            fontSize: '32px',
            fontWeight: 700,
            color: '#e2e8f0',
            margin: '0 0 8px 0',
            letterSpacing: '-0.5px',
          }}>
            THE SYSTEM
          </h1>
          <div style={{
            width: '60px',
            height: '2px',
            background: 'linear-gradient(90deg, transparent, #6366f1, transparent)',
            margin: '12px auto 0',
          }} />
        </div>

        <div style={{
          background: 'rgba(15, 15, 25, 0.8)',
          border: '1px solid rgba(99, 102, 241, 0.2)',
          borderRadius: '12px',
          padding: '36px',
          backdropFilter: 'blur(20px)',
          boxShadow: '0 0 40px rgba(99, 102, 241, 0.1), inset 0 1px 0 rgba(255,255,255,0.05)',
        }}>
          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: '20px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontSize: '11px',
                fontWeight: 600,
                color: '#94a3b8',
                letterSpacing: '1.5px',
                textTransform: 'uppercase',
              }}>
                Username
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                minLength={3}
                maxLength={50}
                autoFocus
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  background: 'rgba(0, 0, 0, 0.3)',
                  border: '1px solid rgba(99, 102, 241, 0.2)',
                  borderRadius: '8px',
                  fontSize: '14px',
                  color: '#e2e8f0',
                  boxSizing: 'border-box',
                  outline: 'none',
                  transition: 'border-color 0.2s, box-shadow 0.2s',
                }}
                onFocus={(e) => {
                  e.target.style.borderColor = 'rgba(99, 102, 241, 0.5)';
                  e.target.style.boxShadow = '0 0 0 3px rgba(99, 102, 241, 0.1)';
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = 'rgba(99, 102, 241, 0.2)';
                  e.target.style.boxShadow = 'none';
                }}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontSize: '11px',
                fontWeight: 600,
                color: '#94a3b8',
                letterSpacing: '1.5px',
                textTransform: 'uppercase',
              }}>
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  background: 'rgba(0, 0, 0, 0.3)',
                  border: '1px solid rgba(99, 102, 241, 0.2)',
                  borderRadius: '8px',
                  fontSize: '14px',
                  color: '#e2e8f0',
                  boxSizing: 'border-box',
                  outline: 'none',
                  transition: 'border-color 0.2s, box-shadow 0.2s',
                }}
                onFocus={(e) => {
                  e.target.style.borderColor = 'rgba(99, 102, 241, 0.5)';
                  e.target.style.boxShadow = '0 0 0 3px rgba(99, 102, 241, 0.1)';
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = 'rgba(99, 102, 241, 0.2)';
                  e.target.style.boxShadow = 'none';
                }}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontSize: '11px',
                fontWeight: 600,
                color: '#94a3b8',
                letterSpacing: '1.5px',
                textTransform: 'uppercase',
              }}>
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={8}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  background: 'rgba(0, 0, 0, 0.3)',
                  border: '1px solid rgba(99, 102, 241, 0.2)',
                  borderRadius: '8px',
                  fontSize: '14px',
                  color: '#e2e8f0',
                  boxSizing: 'border-box',
                  outline: 'none',
                  transition: 'border-color 0.2s, box-shadow 0.2s',
                }}
                onFocus={(e) => {
                  e.target.style.borderColor = 'rgba(99, 102, 241, 0.5)';
                  e.target.style.boxShadow = '0 0 0 3px rgba(99, 102, 241, 0.1)';
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = 'rgba(99, 102, 241, 0.2)';
                  e.target.style.boxShadow = 'none';
                }}
              />
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{
                display: 'block',
                marginBottom: '8px',
                fontSize: '11px',
                fontWeight: 600,
                color: '#94a3b8',
                letterSpacing: '1.5px',
                textTransform: 'uppercase',
              }}>
                Confirm Password
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={8}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  background: 'rgba(0, 0, 0, 0.3)',
                  border: '1px solid rgba(99, 102, 241, 0.2)',
                  borderRadius: '8px',
                  fontSize: '14px',
                  color: '#e2e8f0',
                  boxSizing: 'border-box',
                  outline: 'none',
                  transition: 'border-color 0.2s, box-shadow 0.2s',
                }}
                onFocus={(e) => {
                  e.target.style.borderColor = 'rgba(99, 102, 241, 0.5)';
                  e.target.style.boxShadow = '0 0 0 3px rgba(99, 102, 241, 0.1)';
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = 'rgba(99, 102, 241, 0.2)';
                  e.target.style.boxShadow = 'none';
                }}
              />
            </div>

            {error && (
              <div style={{
                background: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#fca5a5',
                padding: '12px 16px',
                borderRadius: '8px',
                marginBottom: '24px',
                fontSize: '13px',
              }}>
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              style={{
                width: '100%',
                padding: '14px',
                background: isLoading
                  ? 'rgba(99, 102, 241, 0.3)'
                  : 'linear-gradient(135deg, #4f46e5 0%, #6366f1 100%)',
                color: '#fff',
                border: 'none',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: 600,
                letterSpacing: '1px',
                textTransform: 'uppercase',
                cursor: isLoading ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s',
                boxShadow: isLoading
                  ? 'none'
                  : '0 0 20px rgba(99, 102, 241, 0.3)',
              }}
            >
              {isLoading ? 'INITIALIZING...' : 'INITIALIZE ACCOUNT'}
            </button>
          </form>

          <div style={{
            textAlign: 'center',
            marginTop: '24px',
            paddingTop: '24px',
            borderTop: '1px solid rgba(99, 102, 241, 0.1)',
          }}>
            <p style={{
              fontSize: '13px',
              color: '#64748b',
              margin: '0 0 12px 0',
            }}>
              Already have an account?
            </p>
            <Link
              to="/"
              style={{
                display: 'inline-block',
                padding: '10px 24px',
                color: '#6366f1',
                textDecoration: 'none',
                fontSize: '13px',
                fontWeight: 600,
                letterSpacing: '0.5px',
                border: '1px solid rgba(99, 102, 241, 0.3)',
                borderRadius: '8px',
                transition: 'all 0.2s',
              }}
            >
              ACCESS SYSTEM
            </Link>
          </div>
        </div>

        <div style={{
          textAlign: 'center',
          marginTop: '24px',
          fontSize: '11px',
          color: '#475569',
          letterSpacing: '1px',
        }}>
          THE SYSTEM v0.7.2
        </div>
      </div>
    </div>
  );
}
