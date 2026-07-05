import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { IconClose } from "./DashIcons";

/**
 * Modal that lists the connected account's recent posts/reels and lets the
 * creator pick one. Calls onSelect({ id, thumbnail, caption }) on choose.
 */
export default function PostPicker({ accountId, selectedId, onSelect, onClose }) {
  const [media, setMedia] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    if (!accountId) return;
    setLoading(true);
    api.get(`/api/instagram/accounts/${accountId}/media`)
      .then((d) => setMedia(Array.isArray(d) ? d : []))
      .catch((e) => setErr(e.message || "Could not load your posts."))
      .finally(() => setLoading(false));
  }, [accountId]);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card picker-modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose} aria-label="Close"><IconClose /></button>
        <h3 style={{ marginTop: 0 }}>Select a post or reel</h3>
        <p style={{ marginBottom: 18 }}>Pick the post whose comments should trigger this automation.</p>

        {loading ? (
          <p>Loading your posts…</p>
        ) : err ? (
          <div className="banner banner-err">{err}</div>
        ) : media.length === 0 ? (
          <p>No posts found on this account yet.</p>
        ) : (
          <div className="picker-grid">
            {media.map((m) => (
              <button
                key={m.id}
                className={`picker-item${selectedId === m.id ? " selected" : ""}`}
                onClick={() => onSelect(m)}
                title={m.caption || m.id}
              >
                {m.thumbnail
                  ? <img src={m.thumbnail} alt="" loading="lazy" />
                  : <span className="picker-noimg">{m.mediaType || "POST"}</span>}
                {m.mediaType === "VIDEO" && <span className="picker-badge">Reel</span>}
                {selectedId === m.id && <span className="picker-tick">✓</span>}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
