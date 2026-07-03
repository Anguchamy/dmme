import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../lib/api";

function LockIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="4" y="10" width="16" height="10" rx="2.5" fill="#fff" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" stroke="#fff" strokeWidth="2" strokeLinecap="round" />
      <circle cx="12" cy="15" r="1.6" fill="#2563eb" />
    </svg>
  );
}

const QUICK_ACTIONS = [
  { title: "Grow Followers", desc: "Increase followers with automation-driven engagement." },
  { title: "Auto-reply DMs", desc: "Send instant replies to every incoming message." },
  { title: "Capture Leads", desc: "Ask questions in the DM and collect emails & phones." },
  { title: "Comment triggers", desc: "Reply to comments and slide into the DMs instantly." },
];

export default function Overview() {
  const [summary, setSummary] = useState(null);
  const [me, setMe] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    api.get("/api/analytics/summary").then(setSummary).catch(() => {});
    api.get("/api/me").then(setMe).catch(() => {});
    api
      .get("/api/instagram/accounts")
      .then((d) => setAccounts(Array.isArray(d) ? d : []))
      .catch(() => {})
      .finally(() => setLoaded(true));
  }, []);

  const notConnected = loaded && accounts.length === 0;
  const firstName = (me?.fullName || me?.email || "there").split(/[ @]/)[0];

  return (
    <div className="overview">
      <h1 style={{ marginTop: 0 }}>Welcome back, {firstName}</h1>

      <div className="overview-body">
        <div className={notConnected ? "gate-behind" : ""}>
          <div className="metrics">
            <div className="metric"><div className="v">{summary?.dmsSent30d ?? "—"}</div><div className="l">DMs sent (30d)</div></div>
            <div className="metric"><div className="v">{summary?.dmsReceived30d ?? "—"}</div><div className="l">Messages received (30d)</div></div>
            <div className="metric"><div className="v">{summary?.totalLeads ?? "—"}</div><div className="l">Total leads</div></div>
            <div className="metric"><div className="v">{me?.planCode ?? "—"}</div><div className="l">Current plan</div></div>
          </div>

          <div className="row-flex" style={{ justifyContent: "space-between", marginBottom: 12 }}>
            <h2 style={{ margin: 0 }}>Quick actions</h2>
            <Link to="/app/automations/new" className="btn btn-primary btn-sm">+ New automation</Link>
          </div>
          <div className="grid">
            {QUICK_ACTIONS.map((q) => (
              <div className="card" key={q.title}>
                <h3>{q.title}</h3>
                <p>{q.desc}</p>
              </div>
            ))}
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
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
