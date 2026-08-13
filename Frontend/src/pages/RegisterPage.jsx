import { Link } from 'react-router-dom';

export default function RegisterPage() {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: '#f5f5f5',
      padding: '20px',
    }}>
      <div style={{
        width: '100%',
        maxWidth: '400px',
        backgroundColor: '#fff',
        padding: '40px',
        borderRadius: '8px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        textAlign: 'center',
      }}>
        <h1 style={{
          marginBottom: '8px',
          fontSize: '24px',
          fontWeight: 'bold',
          color: '#333',
        }}>
          THE SYSTEM
        </h1>
        <p style={{
          marginBottom: '24px',
          color: '#666',
          fontSize: '14px',
        }}>
          Registration is not yet available.
        </p>
        <p style={{ marginBottom: '24px', color: '#666', fontSize: '14px' }}>
          Please contact an administrator to create an account.
        </p>
        <Link
          to="/"
          style={{
            display: 'inline-block',
            padding: '10px 20px',
            backgroundColor: '#333',
            color: '#fff',
            textDecoration: 'none',
            borderRadius: '4px',
            fontSize: '14px',
          }}
        >
          Back to Login
        </Link>
      </div>
    </div>
  );
}
