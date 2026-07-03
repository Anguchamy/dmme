import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../lib/api";

export default function Automations() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const load = () =>
    api.get("/api/automations").then((d) => {
      setItems(d);
      setLoading(false);
    });

  useEffect(() => {
    load().catch(() => setLoading(false));
  }, []);

  async function toggle(a) {
    const next = a.status === "ACTIVE" ? "PAUSED" : "ACTIVE";
    await api.patch(`/api/automations/${a.id}/status`, { status: next });
    load();
  }

  async function remove(id) {
    if (!confirm("Delete this automation?")) return;
    await api.del(`/api/automations/${id}`);
    load();
  }

  return (
    <div>
      <div className="row-flex" style={{ justifyContent: "space-between" }}>
        <h1 style={{ margin: 0 }}>Automations</h1>
        <Link to="/app/automations/new" className="btn btn-primary btn-sm">+ New automation</Link>
      </div>

      {loading ? (
        <p>Loading…</p>
      ) : items.length === 0 ? (
        <div className="card" style={{ marginTop: 20 }}>
          <p>No automations yet. Create your first one to start converting comments into DMs.</p>
        </div>
      ) : (
        <table style={{ marginTop: 20 }}>
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th>Keywords</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.map((a) => (
              <tr key={a.id}>
                <td>
                  <a style={{ color: "var(--accent)", cursor: "pointer" }}
                     onClick={() => navigate(`/app/automations/${a.id}`)}>{a.name}</a>
                </td>
                <td>{a.type}</td>
                <td>{a.matchAny ? "Any comment" : (a.keywords || []).map((k) => k.keyword).join(", ")}</td>
                <td><span className={`pill ${a.status.toLowerCase()}`}>{a.status}</span></td>
                <td className="row-flex">
                  <button className="btn btn-ghost btn-sm" onClick={() => toggle(a)}>
                    {a.status === "ACTIVE" ? "Pause" : "Activate"}
                  </button>
                  <button className="btn btn-ghost btn-sm" onClick={() => remove(a.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
