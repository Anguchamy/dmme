import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import { ActiveAccountProvider } from "./context/ActiveAccountContext";
import Landing from "./pages/Landing";
import Login from "./pages/Login";
import Privacy from "./pages/Privacy";
import DashboardLayout from "./pages/DashboardLayout";
import Overview from "./pages/Overview";
import Automations from "./pages/Automations";
import AutomationBuilder from "./pages/AutomationBuilder";
import Leads from "./pages/Leads";
import Billing from "./pages/Billing";
import Settings from "./pages/Settings";
import InstagramCallback from "./pages/InstagramCallback";

function Protected({ children }) {
  const { session, loading } = useAuth();
  if (loading) return <div className="loading">Loading…</div>;
  return session ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/privacy" element={<Privacy />} />
      <Route
        path="/instagram/callback"
        element={
          <Protected>
            <InstagramCallback />
          </Protected>
        }
      />
      <Route
        path="/app"
        element={
          <Protected>
            <ActiveAccountProvider>
              <DashboardLayout />
            </ActiveAccountProvider>
          </Protected>
        }
      >
        <Route index element={<Overview />} />
        <Route path="automations" element={<Automations />} />
        <Route path="automations/new" element={<AutomationBuilder />} />
        <Route path="automations/:id" element={<AutomationBuilder />} />
        <Route path="leads" element={<Leads />} />
        <Route path="billing" element={<Billing />} />
        <Route path="settings" element={<Settings />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
