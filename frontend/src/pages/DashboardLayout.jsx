import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Logo } from "../components/Logo";

export default function DashboardLayout() {
  const { signOut } = useAuth();
  const navigate = useNavigate();

  async function logout() {
    await signOut();
    navigate("/", { replace: true });
  }

  return (
    <div className="dash">
      <aside className="sidebar">
        <div className="brand"><Logo size={26} color="#fff" /></div>
        <NavLink to="/app" end>Overview</NavLink>
        <NavLink to="/app/automations">Automations</NavLink>
        <NavLink to="/app/leads">Leads</NavLink>
        <NavLink to="/app/billing">Billing</NavLink>
        <NavLink to="/app/settings">Settings</NavLink>
        <a style={{ cursor: "pointer", marginTop: 24 }} onClick={logout}>Sign out</a>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
