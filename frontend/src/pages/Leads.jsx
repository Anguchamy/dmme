import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { supabase } from "../lib/supabase";

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
      <div className="row-flex" style={{ justifyContent: "space-between" }}>
        <h1 style={{ margin: 0 }}>Leads</h1>
        <button className="btn btn-ghost btn-sm" onClick={exportCsv}>Export CSV</button>
      </div>

      {loading ? (
        <p>Loading…</p>
      ) : leads.length === 0 ? (
        <div className="card" style={{ marginTop: 20 }}>
          <p>No leads captured yet. Add a Question step to an automation to start collecting details.</p>
        </div>
      ) : (
        <table style={{ marginTop: 20 }}>
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
                <td>@{l.igUsername || l.igUserId}</td>
                <td>{l.name || "—"}</td>
                <td>{l.email || "—"}</td>
                <td>{l.phone || "—"}</td>
                <td>{new Date(l.createdAt).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
