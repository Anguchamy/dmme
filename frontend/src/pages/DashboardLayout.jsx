import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useActiveAccount } from "../context/ActiveAccountContext";
import { api } from "../lib/api";
import { LogoMark } from "../components/Logo";
import {
  IconHome, IconBolt, IconUsers, IconCard, IconGear,
  IconLifebuoy, IconLogout, IconRocket, IconClose, IconMail,
} from "../components/DashIcons";

const SUPPORT_EMAIL = "support@dmme.app";

function AccountSwitcher() {
  const { accounts, activeAccount, setActiveAccountId } = useActiveAccount();
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    const h = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, []);

  const email = user?.email || "";
  const handle = activeAccount
    ? `@${activeAccount.igUsername || activeAccount.igUserId}`
    : "Connect Instagram";

  return (
    <div className="acct-switch" ref={ref}>
      <button className="acct-trigger" onClick={() => setOpen((v) => !v)}>
        <div className="side-avatar"><LogoMark size={34} /></div>
        <div className="side-profile-text">
          <div className="side-handle">{handle}</div>
          {email && <div className="side-email">{email}</div>}
        </div>
        <svg className={`acct-caret${open ? " open" : ""}`} width="16" height="16" viewBox="0 0 24 24"
             fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>

      {open && (
        <div className="acct-menu">
          <div className="acct-menu-label">Your accounts</div>
          {accounts.length === 0 && (
            <div className="acct-empty">No Instagram account connected yet.</div>
          )}
          {accounts.map((a) => {
            const active = a.id === activeAccount?.id;
            return (
              <button
                key={a.id}
                className={`acct-item${active ? " active" : ""}`}
                onClick={() => { setActiveAccountId(a.id); setOpen(false); }}
              >
                <span className="acct-item-dot" />
                <span className="acct-item-name">@{a.igUsername || a.igUserId}</span>
                {active && (
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20 6L9 17l-5-5" />
                  </svg>
                )}
              </button>
            );
          })}
          <button
            className="acct-connect"
            onClick={() => { setOpen(false); navigate("/app/settings"); }}
          >
            + Connect another account
          </button>
        </div>
      )}
    </div>
  );
}

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
  const [supportOpen, setSupportOpen] = useState(false);

  useEffect(() => {
    api.get("/api/me").then(setMe).catch(() => {});
    api.get("/api/analytics/summary").then(setSummary).catch(() => {});
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

  return (
    <div className="dash">
      <aside className="sidebar">
        <AccountSwitcher />

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
