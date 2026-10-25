import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Navigate, Route, Routes } from 'react-router-dom';
import { initAuth } from './features/auth/initAuth';
import { useAuth } from './features/auth/useAuth';
import { homePathFor } from './auth/roles';
import Shell from './components/Shell';
import RequireRole from './components/RequireRole';
import DashboardPage from './pages/DashboardPage';
import TenantsPage from './pages/TenantsPage';
import UsersPage from './pages/UsersPage';
import NoAccess from './pages/NoAccess';

function AuthGate({ children }) {
  const dispatch = useDispatch();
  const status = useSelector((state) => state.auth.status);
  useEffect(() => { dispatch(initAuth()); }, [dispatch]);

  if (status === 'loading') return <p className="center muted">Signing you in…</p>;
  if (status === 'error') {
    return (
      <div className="center">
        <p>We couldn't reach the login server.</p>
        <button className="btn btn-primary" onClick={() => window.location.reload()}>Try again</button>
      </div>
    );
  }
  return children;
}

function Home() {
  const { roles } = useAuth();
  const path = homePathFor(roles);
  return path === '/' ? <DashboardPage /> : <Navigate to={path} replace />;
}

export default function App() {
  return (
    <AuthGate>
      <Routes>
        <Route element={<Shell />}>
          <Route index element={<Home />} />
          <Route path="tenants" element={<RequireRole roles={['PLATFORM_ADMIN']}><TenantsPage /></RequireRole>} />
          <Route path="users" element={<RequireRole roles={['TENANT_ADMIN', 'TEACHER']}><UsersPage /></RequireRole>} />
          <Route path="no-access" element={<NoAccess />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </AuthGate>
  );
}