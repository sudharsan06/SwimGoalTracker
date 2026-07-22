import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { auth, signOut } from '../firebase';

const navItems = [
  { path: '/', label: 'Dashboard', icon: '' },
  { path: '/users', label: 'Users', icon: '' },
  { path: '/swimmers', label: 'Swimmers', icon: '' },
  { path: '/events', label: 'Events', icon: '' },
  { path: '/dailyfeed', label: 'Daily Feed', icon: '' },
  { path: '/database', label: 'Data Browser', icon: '' },
  { path: '/settings', label: 'Settings', icon: '' },
];

export default function Layout() {
  const navigate = useNavigate();
  const user = auth.currentUser;

  const handleLogout = async () => {
    await signOut(auth);
    navigate('/');
  };

  const initial = user?.email?.[0]?.toUpperCase() || 'A';

  return (
    <div className="admin-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <h2>Swimmin<span>GO</span></h2>
          <small>Admin Portal</small>
        </div>

        <nav className="sidebar-nav">
          <div className="nav-section">Main</div>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) => isActive ? 'active' : ''}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">{initial}</div>
            <div>
              <div style={{ fontSize: 13, fontWeight: 600 }}>Admin</div>
              <div className="user-email">{user?.email}</div>
            </div>
          </div>
          <button className="logout-btn" onClick={handleLogout}>
            Sign Out
          </button>
        </div>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
