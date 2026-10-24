import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';

export default function Layout() {
  const auth = useAuth();
  const isPlatformAdmin = auth.roles.includes('PLATFORM_ADMIN');
  const canSeeUsers = auth.roles.some((r) => r === 'TENANT_ADMIN' || r === 'TEACHER');

  return (
    <>
      <header className="topbar">
        <strong className="brand">EMS Portal</strong>
        <nav>
          {isPlatformAdmin && <NavLink to="/tenants">Schools</NavLink>}
          {canSeeUsers && <NavLink to="/users">Users</NavLink>}
        </nav>
        <div className="who">
          <span>{auth.name}</span>
          <button className="secondary" onClick={auth.logout}>Sign out</button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}