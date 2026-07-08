import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useActiveAccount } from "../context/ActiveAccountContext";
import UpgradeBanner from "../components/UpgradeBanner";

function relTime(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  const diff = Date.now() - d.getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return "Just now";
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  const days = Math.floor(h / 24);
  if (days < 7) return `${days}d ago`;
  return d.toLocaleDateString();
}

function ImageGlyph() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#b6b0c6" strokeWidth="1.8">
      <rect x="3" y="4" width="18" height="16" rx="2.5" />
      <circle cx="8.5" cy="9.5" r="1.6" />
      <path d="M4 18l5-4 4 3 3-2 4 3" />
    </svg>
  );
}

function RowMenu({ onEdit, onToggle, onDelete, active }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    const h = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, []);
  return (
    <div className="row-menu" ref={ref}>
      <button className="icon-btn" onClick={() => onEdit()} title="Edit">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round"><path d="M4 20h4L18 10l-4-4L4 16z" /><path d="M13.5 6.5l4 4" /></svg>
      </button>
      <button className="icon-btn" onClick={() => setOpen((v) => !v)} title="More">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><circle cx="5" cy="12" r="1.8" /><circle cx="12" cy="12" r="1.8" /><circle cx="19" cy="12" r="1.8" /></svg>
      </button>
      {open && (
        <div className="row-menu-pop">
          <button onClick={() => { setOpen(false); onToggle(); }}>{active ? "Pause" : "Activate"}</button>
          <button onClick={() => { setOpen(false); onEdit(); }}>Edit</button>
          <button className="danger" onClick={() => { setOpen(false); onDelete(); }}>Delete</button>
        </div>
      )}
    </div>
  );
}

export default function Automations() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [perPage, setPerPage] = useState(10);
  const navigate = useNavigate();
  const { activeAccountId, activeAccount } = useActiveAccount();

  const load = () =>
    api.get("/api/automations").then((d) => {
      setItems(Array.isArray(d) ? d : []);
      setLoading(false);
    });

  useEffect(() => {
    load().catch(() => setLoading(false));
  }, []);

  useEffect(() => {
    setPage(1);
  }, [activeAccountId]);

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

  const scoped = activeAccountId
    ? items.filter((a) => a.igAccountId === activeAccountId)
    : items;
  const totalPages = Math.max(1, Math.ceil(scoped.length / perPage));
  const pageItems = scoped.slice((page - 1) * perPage, page * perPage);

  return (
    <div>
      <UpgradeBanner />

      <div className="page-head">
        <div>
          <h1>Automations</h1>
          <p className="page-sub">
            {activeAccount
              ? <>Showing automations for <strong>@{activeAccount.igUsername || activeAccount.igUserId}</strong>.</>
              : "Turn comments and DMs into automated conversations."}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate("/app/automations/new")}>+ Create</button>
      </div>

      {loading ? (
        <p>Loading…</p>
      ) : scoped.length === 0 ? (
        <div className="empty-card">
          <div className="empty-icon"><ImageGlyph /></div>
          <h3>No automations yet</h3>
          <p>Create your first automation to turn comments and DMs into conversations.</p>
          <button className="btn btn-primary" onClick={() => navigate("/app/automations/new")}>+ Create automation</button>
        </div>
      ) : (
        <div className="table-card">
          <table className="auto-table">
            <thead>
              <tr>
                <th style={{ width: 64 }}>Image</th>
                <th>Name</th>
                <th>Status</th>
                <th>Created</th>
                <th>Last modified</th>
                <th style={{ textAlign: "right" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((a) => {
                const active = a.status === "ACTIVE";
                return (
                  <tr key={a.id}>
                    <td><div className="auto-thumb"><ImageGlyph /></div></td>
                    <td>
                      <a className="auto-name" onClick={() => navigate(`/app/automations/${a.id}`)}>
                        {a.name || "Untitled"}
                      </a>
                    </td>
                    <td>
                      <span className={`status-dot ${active ? "on" : "off"}`}>
                        <i /> {active ? "Active" : a.status === "PAUSED" ? "Paused" : "Inactive"}
                      </span>
                    </td>
                    <td className="muted-cell">{relTime(a.createdAt)}</td>
                    <td className="muted-cell">{relTime(a.updatedAt)}</td>
                    <td>
                      <div className="row-flex" style={{ justifyContent: "flex-end", gap: 8 }}>
                        {a.steps?.some((s) => s.stepType === "QUESTION") && (
                          <span className="data-tag">Leads Data</span>
                        )}
                        <RowMenu
                          active={active}
                          onEdit={() => navigate(`/app/automations/${a.id}`)}
                          onToggle={() => toggle(a)}
                          onDelete={() => remove(a.id)}
                        />
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          <div className="table-foot">
            <div className="rows-per">
              Rows per page:
              <select value={perPage} onChange={(e) => { setPerPage(Number(e.target.value)); setPage(1); }}>
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
              </select>
            </div>
            <div className="pager">
              <button className="pager-btn" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>‹</button>
              <span className="pager-num">{page}</span>
              <button className="pager-btn" disabled={page >= totalPages} onClick={() => setPage((p) => p + 1)}>›</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
