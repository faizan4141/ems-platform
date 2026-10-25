import { NavLink } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { LogOut } from 'lucide-react';
import { BRAND } from '../brand';
import { navFor } from '../app/nav';
import { useAuth } from '../features/auth/useAuth';
import { closeSidebar } from '../features/ui/uiSlice';
import { useGetTenantQuery } from '../services/tenantsApi';
import { Avatar } from './ui';

export default function Sidebar() {
  const dispatch = useDispatch();
  const open = useSelector((state) => state.ui.sidebarOpen);
  const { roles, tenantId, name, logout } = useAuth();
  const { data: tenant } = useGetTenantQuery(tenantId, { skip: !tenantId });

  return (
    <>
      <div className={`scrim ${open ? 'show' : ''}`} onClick={() => dispatch(closeSidebar())} />
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Main">
        <div className="brand"><span className="brand-mark">{BRAND.name[0]}</span>{BRAND.name}</div>
        <div className="school">{tenantId ? tenant?.name ?? '…' : 'Platform administration'}</div>

        <nav className="nav">
          {navFor(roles).map((item) => {
            const Icon = item.icon;
            if (item.soon) {
              return (
                <span key={item.label} className="nav-item disabled" aria-disabled="true">
                  <Icon size={18} />{item.label}<span className="chip chip-neutral">Soon</span>
                </span>
              );
            }
            return (
              <NavLink key={item.to} to={item.to} end={item.end}
                       className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                       onClick={() => dispatch(closeSidebar())}>
                <Icon size={18} />{item.label}
              </NavLink>
            );
          })}
        </nav>

        <div className="user-box">
          <Avatar name={name} />
          <span className="who">{name}</span>
          <button className="btn btn-secondary" aria-label="Sign out" onClick={logout}><LogOut size={16} /></button>
        </div>
      </aside>
    </>
  );
}