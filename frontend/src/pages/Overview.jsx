import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { useActiveAccount } from "../context/ActiveAccountContext";
import { IconComment, IconReply, IconFunnel, IconGrowth, IconCheck } from "../components/DashIcons";
import UpgradeBanner from "../components/UpgradeBanner";
import Tilt from "../components/Tilt";

function LockIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="4" y="10" width="16" height="10" rx="2.5" fill="#fff" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" stroke="#fff" strokeWidth="2" strokeLinecap="round" />
      <circle cx="12" cy="15" r="1.6" fill="#2b2bff" />
    </svg>
  );
}

const QUICK_ACTIONS = [
  { key: "comment", title: "Auto DM from Comments", desc: "Send DMs to users who comment on your posts.", Icon: IconComment, badge: "POPULAR", badgeClass: "qa-badge--pop" },
  { key: "growth", title: "Grow Followers", desc: "Boost engagement with automated replies.", Icon: IconGrowth, badge: "TRENDING", badgeClass: "qa-badge--trend" },
  { key: "leads", title: "Generate Leads", desc: "Capture emails & phones right inside the DM.", Icon: IconFunnel, badge: "PRO", badgeClass: "qa-badge--pro" },
  { key: "reply", title: "Auto-reply DMs", desc: "Never miss a message with instant responses.", Icon: IconReply, badge: "PRO", badgeClass: "qa-badge--pro" },
];

export default function Overview() {
  const [summary, setSummary] = useState(null);
  const [me, setMe] = useState(null);
  const [automationCount, setAutomationCount] = useState(0);
  const navigate = useNavigate();
  const { user } = useAuth();
  const { accounts, activeAccountId, loading: accountsLoading } = useActiveAccount();

  useEffect(() => {
    api.get("/api/analytics/summary").then(setSummary).catch(() => {});
    api.get("/api/me").then(setMe).catch(() => {});
  }, []);

  useEffect(() => {
    api.get("/api/automations")
      .then((d) => {
        const list = Array.isArray(d) ? d : [];
        const scoped = activeAccountId ? list.filter((a) => a.igAccountId === activeAccountId) : list;
        setAutomationCount(scoped.length);
      })
      .catch(() => {});
  }, [activeAccountId]);

  const notConnected = !accountsLoading && accounts.length === 0;
  const displayName =
    me?.fullName || user?.user_metadata?.full_name || user?.user_metadata?.name ||
    me?.email || user?.email || "there";
  const firstName = displayName.split(/[ @]/)[0];

  const steps = [
    { label: "Connect Instagram", done: accounts.length > 0, go: "/app/settings" },
    { label: "Set up your profile", done: !!me?.fullName, go: "/app/settings" },
    { label: "Create your first automation", done: automationCount > 0, go: "/app/automations/new" },
    { label: "Send your first auto DM", done: (summary?.dmsSent30d ?? 0) > 0, go: "/app/automations" },
  ];
  const doneCount = steps.filter((s) => s.done).length;
  const progressPct = Math.round((doneCount / steps.length) * 100);

  return (
    <div className="overview">
      <h1 style={{ marginTop: 0 }}>Welcome back, {firstName} 👋</h1>

      <UpgradeBanner />

      <div className="overview-body">
        <div className={notConnected ? "gate-behind" : ""}>
          <div className="dash-columns">
            <div className="getstarted card">
              <div className="row-flex" style={{ justifyContent: "space-between" }}>
                <h3 style={{ margin: 0 }}>Get started</h3>
                <span className="gs-pct">{progressPct}% complete</span>
              </div>
              <div className="gs-bar"><div className="gs-fill" style={{ width: `${progressPct}%` }} /></div>
              <ul className="gs-list">
                {steps.map((st) => (
                  <li key={st.label} className={st.done ? "gs-done" : ""} onClick={() => !st.done && navigate(st.go)}>
                    <span className={`gs-check${st.done ? " gs-check--on" : ""}`}>{st.done && <IconCheck />}</span>
                    <span className="gs-label">{st.label}</span>
                    {!st.done && <span className="gs-cta">Do it →</span>}
                  </li>
                ))}
              </ul>
            </div>

            <div className="quick-actions">
              <div className="row-flex" style={{ justifyContent: "space-between", marginBottom: 12 }}>
                <h3 style={{ margin: 0 }}>Quick actions</h3>
                <button className="btn btn-primary btn-sm" onClick={() => navigate("/app/automations/new")}>+ Create new</button>
              </div>
              <div className="qa-grid">
                {QUICK_ACTIONS.map(({ key, title, desc, Icon, badge, badgeClass }) => (
                  <Tilt className="qa-card" key={key} role="button" tabIndex={0} onClick={() => navigate("/app/automations/new")}>
                    <span className="qa-icon"><Icon /></span>
                    <span className={`qa-badge ${badgeClass}`}>{badge}</span>
                    <span className="qa-title">{title}</span>
                    <span className="qa-desc">{desc}</span>
                  </Tilt>
                ))}
              </div>
            </div>
          </div>

          <h3 className="metrics-head">Metrics <span>Last 30 days</span></h3>
          <div className="metrics">
            <div className="metric"><div className="v">{summary?.dmsSent30d ?? "—"}</div><div className="l">Messages sent</div></div>
            <div className="metric"><div className="v">{summary?.dmsReceived30d ?? "—"}</div><div className="l">Messages received</div></div>
            <div className="metric"><div className="v">{summary?.totalLeads ?? "—"}</div><div className="l">Total leads</div></div>
            <div className="metric"><div className="v">{me?.planCode ?? "—"}</div><div className="l">Current plan</div></div>
          </div>
        </div>

        {notConnected && (
          <div className="ig-gate">
            <div className="ig-gate-card">
              <div className="ig-lock"><LockIcon /></div>
              <h3>Connect Instagram to unlock your dashboard</h3>
              <p>
                Connect an Instagram Business or Creator account once to activate live metrics,
                quick actions, and automations. You can keep exploring the other tabs meanwhile.
              </p>
              <button className="btn btn-primary" onClick={() => navigate("/app/settings")}>
                Connect Instagram
              </button>
              <p className="ig-gate-trust">
                Secure official Instagram login · Trusted by <strong>1,000+ creators</strong>
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
