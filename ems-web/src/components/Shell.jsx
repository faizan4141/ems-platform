import { Outlet } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Menu } from 'lucide-react';
import { BRAND } from '../brand';
import { toggleSidebar } from '../features/ui/uiSlice';
import Sidebar from './Sidebar';
import Toaster from './Toaster';

export default function Shell() {
  const dispatch = useDispatch();
  return (
    <div className="shell">
      <Sidebar />
      <div className="main">
        <header className="topbar">
          <button className="btn btn-secondary" aria-label="Open menu" onClick={() => dispatch(toggleSidebar())}>
            <Menu size={18} />
          </button>
          <strong>{BRAND.name}</strong>
        </header>
        <main className="content"><Outlet /></main>
      </div>
      <Toaster />
    </div>
  );
}