import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthProvider';
import { homePathFor } from './auth/roles';
import Layout from './components/Layout';
import RequireRole from './components/RequireRole';
import TenantsPage from './pages/TenantsPage';
import UsersPage from './pages/UsersPage';
import NoAccess from './pages/NoAccess';

function Home() {
  const { roles } = useAuth();
  return <Navigate to={homePathFor(roles)} replace />;
}

function AuthGate({ children }) {
  const { status } = useAuth();
  if (status === 'loading') {
    return <p className="center">Signing you in…</p>;
  }
  if (status === 'error') {
    return (
      <div className="center">
        <p>We couldn't reach the login server.</p>
        <button onClick={() => window.location.reload()}>Try again</button>
      </div>
    );
  }
  return children;
}

export default function App() {
  return (
    <AuthGate>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="tenants" element={
            <RequireRole roles={['PLATFORM_ADMIN']}><TenantsPage /></RequireRole>
          } />
          <Route path="users" element={
            <RequireRole roles={['TENANT_ADMIN', 'TEACHER']}><UsersPage /></RequireRole>
          } />
          <Route path="no-access" element={<NoAccess />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </AuthGate>
  );
}