import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopBar from './TopBar';
import './layout.css';

const SECTION_TITLES = {
  '/dashboard': 'Dashboard',
  '/profile': 'Profile',
  '/tasks': 'Tasks',
  '/goals': 'Goals',
  '/progress': 'Progress',
  '/settings': 'Settings',
};

export default function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();

  const title = SECTION_TITLES[location.pathname] ?? 'THE SYSTEM';

  return (
    <div className="app-shell">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="app-main">
        <TopBar
          title={title}
          onToggleSidebar={() => setSidebarOpen((open) => !open)}
        />
        <main className="app-content">
          <Outlet />
        </main>
      </div>
      <button
        type="button"
        className="app-backdrop"
        aria-label="Close navigation"
        onClick={() => setSidebarOpen(false)}
      />
    </div>
  );
}
