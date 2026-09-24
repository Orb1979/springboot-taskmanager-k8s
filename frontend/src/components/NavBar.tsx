import { NavLink } from "react-router-dom";

export function NavBar() {
  return (
    <nav className="nav">
      <div className="nav-inner">
        <NavLink to="/tasks" className={({ isActive }) => (isActive ? "active" : undefined)}>
          Tasks
        </NavLink>
        <NavLink to="/jobs" className={({ isActive }) => (isActive ? "active" : undefined)}>
          Jobs
        </NavLink>
      </div>
    </nav>
  );
}
