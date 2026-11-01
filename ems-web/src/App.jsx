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
import SetupPage from './pages/SetupPage';
import StudentsPage from './pages/StudentsPage';
import StudentDetailPage from './pages/StudentDetailPage';
import AttendancePage from './pages/AttendancePage';
import ParentHomePage from './pages/ParentHomePage';
import NoticesPage from './pages/NoticesPage';
import FeesPage from './pages/FeesPage';
import FeeSetupPage from './pages/FeeSetupPage';
import StudentFeesPage from './pages/StudentFeesPage';
import OnlinePaymentsPage from './pages/OnlinePaymentsPage';
import SimulatedCheckoutPage from './pages/SimulatedCheckoutPage';

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
          <Route path="setup" element={<RequireRole roles={['TENANT_ADMIN']}><SetupPage /></RequireRole>} />
          <Route path="students" element={<RequireRole roles={['TENANT_ADMIN', 'TEACHER']}><StudentsPage /></RequireRole>} />
          <Route path="students/:id" element={<RequireRole roles={['TENANT_ADMIN', 'TEACHER']}><StudentDetailPage /></RequireRole>} />
          <Route path="attendance" element={<RequireRole roles={['TENANT_ADMIN', 'TEACHER']}><AttendancePage /></RequireRole>} />
          <Route path="parent" element={<RequireRole roles={['PARENT']}><ParentHomePage /></RequireRole>} />
          <Route path="notices" element={<RequireRole roles={['TENANT_ADMIN', 'TEACHER', 'PARENT']}><NoticesPage /></RequireRole>} />
          <Route path="fees" element={<RequireRole roles={['TENANT_ADMIN']}><FeesPage /></RequireRole>} />
          <Route path="fees/setup" element={<RequireRole roles={['TENANT_ADMIN']}><FeeSetupPage /></RequireRole>} />
          <Route path="fees/students/:id" element={<RequireRole roles={['TENANT_ADMIN']}><StudentFeesPage /></RequireRole>} />
          <Route path="fees/online" element={<RequireRole roles={['TENANT_ADMIN']}><OnlinePaymentsPage /></RequireRole>} />
          <Route path="pay/simulated/:providerRef" element={<RequireRole roles={['PARENT']}><SimulatedCheckoutPage /></RequireRole>} />
        </Route>
      </Routes>
    </AuthGate>
  );
}