import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";
import { LogoMark } from "../components/Logo";
import {
  IconHome, IconBolt, IconUsers, IconCard, IconGear,
  IconLifebuoy, IconLogout, IconRocket, IconClose, IconMail,
} from "../components/DashIcons";

const SUPPORT_EMAIL = "support@dmme.app";

function UsageMeter({ label, used, limit }) {
  const unlimited = limit == null;
  const pct = unlimited ? 0 : Math.min(100, Math.round((used / Math.max(limit, 1)) * 100));
  const near = pct >= 80;
  return (
    <div className="usage-meter">
      <div className="usage-top">
        <span>{label}</span>
        <span>{used}/{unlimited ? "∞" : limit}</span>
      </div>
      <div className="usage-bar">
        <div className={`usage-fill${near ? " usage-fill--near" : ""}`} style={{ width: `${unlimited ? 4 : pct}%` }} />
      </div>
    </div>
  );
}

export default function DashboardLayout() {
  const { signOut } = useAuth();
  const navigate = useNavigate();
  const [me, setMe] = useState(null);
  const [summary, setSummary] = useState(null);
  const [plan, setPlan] = useState(null);
  const [handle, setHandle] = useState("");
  const [supportOpen, setSupportOpen] = useState(false);

  useEffect(() => {
    api.get("/api/me").then(setMe).catch(() => {});
    api.get("/api/analytics/summary").then(setSummary).catch(() => {});
    api.get("/api/instagram/accounts")
      .then((d) => Array.isArray(d) && d[0]?.igUsername && setHandle(d[0].igUsername))
      .catch(() => {});
  }, []);

  useEffect(() => {
    const code = me?.planCode || "FREE";
    api.get("/api/plans")
      .then((list) => setPlan((Array.isArray(list) ? list : []).find((p) => p.code === code) || null))
      .catch(() => {});
  }, [me]);

  async function logout() {
    await signOut();
    navigate("/", { replace: true });
  }

  const isFree = !me?.planCode || me.planCode === "FREE";
  const email = me?.email || "";
  const displayHandle = handle ? `@${handle}` : (email ? email.split("@")[0] : "your account");

  return (
    <div className="dash">
      <aside className="sidebar">
        <div className="side-profile">
          <div className="side-avatar"><LogoMark size={34} /></div>
          <div className="side-profile-text">
            <div className="side-handle">{displayHandle}</div>
            {email && <div className="side-email">{email}</div>}
          </div>
        </div>

        <div className="side-section-label">Workspace</div>
        <nav className="side-nav">
          <NavLink to="/app" end><IconHome /><span>Home</span></NavLink>
          <NavLink to="/app/automations"><IconBolt /><span>Automations</span></NavLink>
          <NavLink to="/app/leads"><IconUsers /><span>Contacts</span></NavLink>
          <NavLink to="/app/billing"><IconCard /><span>Billing</span></NavLink>
          <NavLink to="/app/settings"><IconGear /><span>Settings</span></NavLink>
        </nav>

        <div className="side-footer">
          <div className="side-meters">
            <UsageMeter label="DMs this month" used={summary?.dmsUsedThisMonth ?? 0} limit={plan?.dmLimit} />
            <UsageMeter label="Contacts" used={summary?.totalLeads ?? 0} limit={plan?.contactLimit} />
          </div>

          {isFree && (
            <button className="btn-upgrade" onClick={() => navigate("/app/billing")}>
              <IconRocket /> Upgrade
            </button>
          )}

          <div className="side-actions">
            <button className="side-btn" onClick={() => setSupportOpen(true)}><IconLifebuoy /> Support</button>
            <button className="side-btn" onClick={logout}><IconLogout /> Logout</button>
          </div>
        </div>
      </aside>

      <main className="main">
        <Outlet />
      </main>

      {supportOpen && (
        <div className="modal-backdrop" onClick={() => setSupportOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <button className="modal-close" onClick={() => setSupportOpen(false)} aria-label="Close"><IconClose /></button>
            <div className="support-icon"><IconLifebuoy /></div>
            <h3>Need a hand?</h3>
            <p>Our team usually replies within a few hours. Tell us what's up and we'll get you sorted.</p>
            <a className="btn btn-primary support-cta" href={`mailto:${SUPPORT_EMAIL}?subject=dmme%20support`}>
              <IconMail /> Email {SUPPORT_EMAIL}
            </a>
            <p className="support-hint">Tip: include your Instagram handle and a screenshot so we can help faster.</p>
          </div>
        </div>
      )}
    </div>
  );
}
