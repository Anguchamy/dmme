import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import UpgradeBanner from "../components/UpgradeBanner";

function InstagramGlyph() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="2.5" y="2.5" width="19" height="19" rx="5.5" stroke="currentColor" strokeWidth="2" />
      <circle cx="12" cy="12" r="4.2" stroke="currentColor" strokeWidth="2" />
      <circle cx="17.6" cy="6.4" r="1.4" fill="currentColor" />
    </svg>
  );
}

function LockGlyph() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="4" y="10.5" width="16" height="10" rx="2.5" stroke="currentColor" strokeWidth="2" />
      <path d="M7.5 10.5V8a4.5 4.5 0 0 1 9 0v2.5" stroke="currentColor" strokeWidth="2" />
    </svg>
  );
}

function StarGlyph() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 3.5l2.6 5.27 5.82.85-4.21 4.1.99 5.79L12 16.77l-5.2 2.73.99-5.79-4.21-4.1 5.82-.85L12 3.5z" />
    </svg>
  );
}

export default function Settings() {
  const [me, setMe] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [fullName, setFullName] = useState("");
  const [connect, setConnect] = useState({
    igUserId: "",
    igUsername: "",
    facebookPageId: "",
    pageAccessToken: "",
  });
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");
  const [connecting, setConnecting] = useState(false);
  const [params, setParams] = useSearchParams();

  const loadAccounts = () =>
    api
      .get("/api/instagram/accounts")
      .then((d) => setAccounts(Array.isArray(d) ? d : []))
      .catch(() => {});

  useEffect(() => {
    api.get("/api/me").then((m) => {
      setMe(m);
      setFullName(m.fullName || "");
    });
    loadAccounts();
  }, []);

  // Surface the outcome of the Instagram OAuth redirect, then clean the URL.
  useEffect(() => {
    const connected = params.get("ig_connected");
    const igErr = params.get("ig_error");
    if (connected) {
      setMsg(`Instagram connected: ${connected}`);
      loadAccounts();
    }
    if (igErr) setErr(`Instagram connection failed: ${igErr}`);
    if (connected || igErr) {
      params.delete("ig_connected");
      params.delete("ig_error");
      setParams(params, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function saveProfile() {
    setMsg("");
    setErr("");
    await api.put("/api/me", { fullName });
    setMsg("Profile saved.");
  }

  async function connectInstagram() {
    setErr("");
    setMsg("");
    setConnecting(true);
    try {
      const { url } = await api.get("/api/instagram/oauth/url");
      window.location.assign(url);
    } catch (e) {
      setConnecting(false);
      setErr(e.message || "Could not start Instagram login.");
    }
  }

  async function connectManual() {
    setErr("");
    setMsg("");
    try {
      await api.post("/api/instagram/accounts/connect", connect);
      setConnect({ igUserId: "", igUsername: "", facebookPageId: "", pageAccessToken: "" });
      loadAccounts();
      setMsg("Instagram account connected.");
    } catch (e) {
      setErr(e.message);
    }
  }

  async function disconnect(id) {
    await api.del(`/api/instagram/accounts/${id}`);
    loadAccounts();
  }

  return (
    <div style={{ maxWidth: 680 }}>
      <UpgradeBanner />
      <div className="page-head">
        <div>
          <h1>Settings</h1>
          <p className="page-sub">Manage your profile and connected accounts.</p>
        </div>
      </div>
      {msg && <div className="banner banner-ok">{msg}</div>}
      {err && <div className="banner banner-err">{err}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <h3>Profile</h3>
        <div className="field">
          <label>Email</label>
          <input value={me?.email || ""} disabled />
        </div>
        <div className="field">
          <label>Full name</label>
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </div>
        <button className="btn btn-primary btn-sm" onClick={saveProfile}>Save profile</button>
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <div className="row-flex" style={{ justifyContent: "space-between" }}>
          <h3 style={{ margin: 0 }}>Instagram</h3>
          <button className="btn btn-ig btn-sm" onClick={connectInstagram} disabled={connecting}>
            <InstagramGlyph />
            {connecting ? "Redirecting…" : accounts.length ? "Connect another" : "Connect Instagram"}
          </button>
        </div>
        <p style={{ color: "var(--muted)", fontSize: 14 }}>
          Connect a Business or Creator Instagram account to start automating comments, story
          replies, and DMs. You'll be redirected to Instagram to authorize dmme.
        </p>

        <div className="trust-row">
          <span className="trust-item">
            <LockGlyph />
            Secure official Instagram login — we never see your password
          </span>
          <span className="trust-item">
            <StarGlyph />
            Trusted by <strong>1,000+ creators</strong>
          </span>
        </div>

        {accounts.length === 0 ? (
          <p style={{ color: "var(--muted)" }}>No accounts connected yet.</p>
        ) : (
          <table>
            <tbody>
              {accounts.map((a) => (
                <tr key={a.id}>
                  <td>@{a.igUsername || a.igUserId}</td>
                  <td>{a.isActive ? "Active" : "Inactive"}</td>
                  <td>
                    <button className="btn btn-ghost btn-sm" onClick={() => disconnect(a.id)}>
                      Disconnect
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <details className="card">
        <summary style={{ cursor: "pointer", fontWeight: 600 }}>
          Advanced: connect manually with a token
        </summary>
        <p style={{ color: "var(--muted)", fontSize: 14 }}>
          For development/testing only. Paste an Instagram user id and a long-lived access token
          obtained outside the app.
        </p>
        <div className="field">
          <label>Instagram user ID</label>
          <input value={connect.igUserId} onChange={(e) => setConnect({ ...connect, igUserId: e.target.value })} />
        </div>
        <div className="field">
          <label>Instagram username</label>
          <input value={connect.igUsername} onChange={(e) => setConnect({ ...connect, igUsername: e.target.value })} />
        </div>
        <div className="field">
          <label>Facebook Page ID (optional)</label>
          <input value={connect.facebookPageId} onChange={(e) => setConnect({ ...connect, facebookPageId: e.target.value })} />
        </div>
        <div className="field">
          <label>Access token</label>
          <input value={connect.pageAccessToken} onChange={(e) => setConnect({ ...connect, pageAccessToken: e.target.value })} />
        </div>
        <button className="btn btn-ghost btn-sm" onClick={connectManual}>Connect manually</button>
      </details>
    </div>
  );
}
