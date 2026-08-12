import { AuthProvider } from './auth/AuthContext';
import AuthTestPage from './components/AuthTestPage';

function App() {
  return (
    <AuthProvider>
      <AuthTestPage />
    </AuthProvider>
  );
}

export default App
