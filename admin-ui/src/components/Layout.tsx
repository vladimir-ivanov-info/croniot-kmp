import { NavLink, Outlet } from "react-router";
import { useAuth } from "../auth/useAuth";
import "./Layout.css";

// Header with navigation shared by every signed-in page.
export function Layout() {
  const { logout } = useAuth();
  return (
    <div className="layout">
      <header className="layout-header">
        <div className="layout-brand">
          <img src="/logo.png" alt="" className="layout-logo" />
          <span className="layout-title">Croniot Admin</span>
        </div>
        <nav className="layout-nav" aria-label="Main">
          <NavLink to="/logs">Logs</NavLink>
          <NavLink to="/flags">Feature flags</NavLink>
        </nav>
        <button type="button" className="layout-signout" onClick={logout}>
          Sign out
        </button>
      </header>
      <main className="layout-main">
        <Outlet />
      </main>
    </div>
  );
}
