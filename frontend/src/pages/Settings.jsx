import { useEffect, useState } from "react";
import { api } from "../lib/api";

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

  const loadAccounts = () => api.get("/api/instagram/accounts").then(setAccounts).catch(() => {});

  useEffect(() => {
    api.get("/api/me").then((m) => {
      setMe(m);
      setFullName(m.fullName || "");
    });
    loadAccounts();
  }, []);

  async function saveProfile() {
    setMsg("");
    await api.put("/api/me", { fullName });
    setMsg("Profile saved.");
  }

  async function connectAccount() {
    setMsg("");
    try {
      await api.post("/api/instagram/accounts/connect", connect);
      setConnect({ igUserId: "", igUsername: "", facebookPageId: "", pageAccessToken: "" });
      loadAccounts();
      setMsg("Instagram account connected.");
    } catch (e) {
      setMsg(e.message);
    }
  }

  async function disconnect(id) {
    await api.del(`/api/instagram/accounts/${id}`);
    loadAccounts();
  }

  return (
    <div style={{ maxWidth: 680 }}>
      <h1>Settings</h1>
      {msg && <div className="card" style={{ marginBottom: 16 }}>{msg}</div>}

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
        <h3>Connected Instagram accounts</h3>
        {accounts.length === 0 ? (
          <p style={{ color: "var(--muted)" }}>No accounts connected yet.</p>
        ) : (
          <table>
            <tbody>
              {accounts.map((a) => (
                <tr key={a.id}>
                  <td>@{a.igUsername || a.igUserId}</td>
                  <td>{a.isActive ? "Active" : "Inactive"}</td>
                  <td><button className="btn btn-ghost btn-sm" onClick={() => disconnect(a.id)}>Disconnect</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h3>Connect an account</h3>
        <p style={{ color: "var(--muted)", fontSize: 14, marginTop: 0 }}>
          Run the "Login with Instagram" flow (see docs/SETUP.md), then paste the resulting
          Instagram business account details and long-lived Page access token here.
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
          <label>Facebook Page ID</label>
          <input value={connect.facebookPageId} onChange={(e) => setConnect({ ...connect, facebookPageId: e.target.value })} />
        </div>
        <div className="field">
          <label>Page access token</label>
          <input value={connect.pageAccessToken} onChange={(e) => setConnect({ ...connect, pageAccessToken: e.target.value })} />
        </div>
        <button className="btn btn-primary btn-sm" onClick={connectAccount}>Connect</button>
      </div>
    </div>
  );
}
