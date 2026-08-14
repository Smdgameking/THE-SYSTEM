import { NavLink } from 'react-router-dom';
import {
  GridIcon,
  ListIcon,
  TargetIcon,
  TrendingUpIcon,
  SettingsIcon,
  UserIcon,
} from './icons';

const NAV_SECTIONS = [
  {
    label: 'Overview',
    items: [
      { to: '/dashboard', label: 'Dashboard', icon: GridIcon, end: true },
    ],
  },
  {
    label: 'Operations',
    items: [
      { to: '/tasks', label: 'Tasks', icon: ListIcon },
      { to: '/goals', label: 'Goals', icon: TargetIcon },
      { to: '/progress', label: 'Progress', icon: TrendingUpIcon },
    ],
  },
  {
    label: 'System',
    items: [
      { to: '/settings', label: 'Settings', icon: SettingsIcon },
    ],
  },
];

export default function Sidebar({ open, onClose }) {
  return (
    <aside className={`app-sidebar${open ? ' is-open' : ''}`} aria-label="Main navigation">
      <div className="app-sidebar-brand">
        <div className="app-sidebar-brand-mark">
          <UserIcon width={16} height={16} />
        </div>
        <div>
          <div className="app-sidebar-brand-name">THE SYSTEM</div>
          <div className="app-sidebar-brand-sub">Command Deck</div>
        </div>
      </div>

      <nav className="app-sidebar-nav">
        {NAV_SECTIONS.map((section) => (
          <div key={section.label}>
            <div className="app-sidebar-section">{section.label}</div>
            {section.items.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    `app-nav-link${isActive ? ' active' : ''}`
                  }
                  onClick={onClose}
                >
                  <Icon className="icon" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </div>
        ))}
      </nav>

      <div className="app-sidebar-footer">v0.7.2</div>
    </aside>
  );
}
