import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

export default function Overview() {
  const [summary, setSummary] = useState(null);
  const [me, setMe] = useState(null);
  const [accounts, setAccounts] = useState([]);

  useEffect(() => {
    api.get("/api/analytics/summary").then(setSummary).catch(() => {});
    api.get("/api/me").then(setMe).catch(() => {});
    api.get("/api/instagram/accounts").then(setAccounts).catch(() => {});
  }, []);

  return (
    <div>
      <h1>Overview</h1>
      <div className="metrics">
        <div className="metric"><div className="v">{summary?.dmsSent30d ?? "—"}</div><div className="l">DMs sent (30d)</div></div>
        <div className="metric"><div className="v">{summary?.dmsReceived30d ?? "—"}</div><div className="l">Messages received (30d)</div></div>
        <div className="metric"><div className="v">{summary?.totalLeads ?? "—"}</div><div className="l">Total leads</div></div>
        <div className="metric"><div className="v">{me?.planCode ?? "—"}</div><div className="l">Current plan</div></div>
      </div>

      {accounts.length === 0 && (
        <div className="card" style={{ marginBottom: 24 }}>
          <h3>Connect your Instagram account</h3>
          <p>
            Link a Business or Creator Instagram account (connected to a Facebook Page) to start
            automating. Set this up in <Link to="/app/settings" style={{ color: "var(--accent)" }}>Settings</Link>.
          </p>
        </div>
      )}

      <div className="row-flex" style={{ justifyContent: "space-between" }}>
        <h2 style={{ margin: 0 }}>Get started</h2>
        <Link to="/app/automations/new" className="btn btn-primary btn-sm">+ New automation</Link>
      </div>
      <p style={{ color: "var(--muted)" }}>
        Create a keyword-triggered automation that DMs your followers a link, gates it behind a
        follow, and asks questions to capture their details.
      </p>
    </div>
  );
}
