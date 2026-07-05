import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { supabase } from "../lib/supabase";
import UpgradeBanner from "../components/UpgradeBanner";

export default function Leads() {
  const [leads, setLeads] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get("/api/leads").then((d) => {
      setLeads(d);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  async function exportCsv() {
    const { data: { session } } = await supabase.auth.getSession();
    const res = await fetch(`${api.baseUrl}/api/leads/export`, {
      headers: { Authorization: `Bearer ${session?.access_token}` },
    });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "leads.csv";
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div>
      <UpgradeBanner />

      <div className="page-head">
        <div>
          <h1>Contacts</h1>
          <p className="page-sub">People captured through your automations.</p>
        </div>
        {leads.length > 0 && <button className="btn btn-ghost" onClick={exportCsv}>Export CSV</button>}
      </div>

      {loading ? (
        <p>Loading…</p>
      ) : leads.length === 0 ? (
        <div className="empty-card">
          <div className="empty-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#b6b0c6" strokeWidth="1.8"><circle cx="9" cy="8" r="3.2" /><path d="M3.5 20c0-3 2.5-5 5.5-5s5.5 2 5.5 5" /></svg>
          </div>
          <h3>No contacts yet</h3>
          <p>Add a Question step to an automation to start collecting emails, phones and names.</p>
        </div>
      ) : (
        <div className="table-card">
          <table className="auto-table">
            <thead>
              <tr>
                <th>Instagram</th>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Captured</th>
              </tr>
            </thead>
            <tbody>
              {leads.map((l) => (
                <tr key={l.id}>
                  <td className="auto-name">@{l.igUsername || l.igUserId}</td>
                  <td>{l.name || "—"}</td>
                  <td>{l.email || "—"}</td>
                  <td>{l.phone || "—"}</td>
                  <td className="muted-cell">{new Date(l.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
