import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../lib/api";

const emptyStep = () => ({
  stepType: "TEXT",
  body: "",
  buttons: [],
  collectField: "EMAIL",
  customKey: "",
});

function IgGlyph() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="url(#igg)" strokeWidth="2">
      <defs>
        <linearGradient id="igg" x1="0" y1="0" x2="24" y2="24">
          <stop stopColor="#f58529" /><stop offset="0.5" stopColor="#dd2a7b" /><stop offset="1" stopColor="#8134af" />
        </linearGradient>
      </defs>
      <rect x="2.5" y="2.5" width="19" height="19" rx="5.5" />
      <circle cx="12" cy="12" r="4.2" />
      <circle cx="17.6" cy="6.4" r="1.2" fill="#dd2a7b" stroke="none" />
    </svg>
  );
}
function ChatGlyph() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#6d28d9" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 5h16v11H9l-4 3v-3H4z" />
    </svg>
  );
}
function ShieldGlyph() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#16a34a" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6z" /><path d="M9 12l2 2 4-4" />
    </svg>
  );
}

const TRIGGERS = [
  { type: "COMMENT", label: "User comments on your post or reel", brand: true },
  { type: "DM", label: "User DMs to you", brand: false },
  { type: "LIVE", label: "User comments on your LIVE", brand: true },
  { type: "STORY_REPLY", label: "User replies to your stories", brand: true },
  { type: "MENTION", label: "User mentions you in story", brand: true, soon: true },
];

function Toggle({ on, onChange, disabled }) {
  return (
    <button
      type="button"
      className={`toggle${on ? " toggle--on" : ""}${disabled ? " toggle--disabled" : ""}`}
      onClick={() => !disabled && onChange(!on)}
      aria-pressed={on}
    >
      <span className="toggle-knob" />
    </button>
  );
}

export default function AutomationBuilder() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();

  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState({
    name: "",
    type: "COMMENT",
    status: "DRAFT",
    igAccountId: null,
    igMediaId: "",
    matchAny: false,
    publicReply: "",
    askFollowEnabled: false,
    askFollowMessage: "",
    keywords: [{ keyword: "", matchType: "CONTAINS" }],
    steps: [{ ...emptyStep(), body: "Here's the link you asked for!" }],
  });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");

  useEffect(() => {
    api.get("/api/instagram/accounts").then((d) => setAccounts(Array.isArray(d) ? d : [])).catch(() => {});
    if (editing) {
      api.get(`/api/automations/${id}`).then((a) =>
        setForm({
          ...a,
          keywords: a.keywords?.length ? a.keywords : [{ keyword: "", matchType: "CONTAINS" }],
          steps: a.steps?.length ? a.steps : [emptyStep()],
        })
      );
    }
  }, [id, editing]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const setKeyword = (i, v) =>
    set("keywords", form.keywords.map((k, idx) => (idx === i ? { ...k, keyword: v } : k)));
  const addKeyword = () => set("keywords", [...form.keywords, { keyword: "", matchType: "CONTAINS" }]);
  const removeKeyword = (i) => set("keywords", form.keywords.filter((_, idx) => idx !== i));

  const setStep = (i, patch) =>
    set("steps", form.steps.map((s, idx) => (idx === i ? { ...s, ...patch } : s)));
  const addStep = () => set("steps", [...form.steps, emptyStep()]);
  const removeStep = (i) => set("steps", form.steps.filter((_, idx) => idx !== i));

  async function save() {
    setErr("");
    setBusy(true);
    const payload = {
      ...form,
      keywords: form.matchAny ? [] : form.keywords.filter((k) => k.keyword.trim()),
      steps: form.steps.map((s, idx) => ({ ...s, stepOrder: idx })),
    };
    try {
      if (editing) await api.put(`/api/automations/${id}`, payload);
      else await api.post("/api/automations", payload);
      navigate("/app/automations");
    } catch (e) {
      setErr(e.message);
    } finally {
      setBusy(false);
    }
  }

  const isActive = form.status === "ACTIVE";
  const isComment = form.type === "COMMENT" || form.type === "LIVE";

  return (
    <div className="builder">
      {/* Top bar */}
      <div className="builder-bar">
        <button className="back-link" onClick={() => navigate("/app/automations")}>← Back</button>
        <input
          className="builder-title"
          value={form.name}
          onChange={(e) => set("name", e.target.value)}
          placeholder="Untitled"
        />
        <div className="builder-bar-right">
          <label className="status-toggle-label">
            <span>Status</span>
            <Toggle on={isActive} onChange={(v) => set("status", v ? "ACTIVE" : "PAUSED")} />
          </label>
          <button className="btn btn-primary" disabled={busy} onClick={save}>
            {busy ? "Saving…" : "Save Changes"}
          </button>
        </div>
      </div>

      {err && <div className="banner banner-err">{err}</div>}

      {/* Trigger */}
      <div className="flow-card">
        <div className="flow-head">
          <span className="flow-head-icon flow-head-icon--amber">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="#fff"><path d="M13 2 4 14h7l-1 8 9-12h-7z" /></svg>
          </span>
          <div>
            <div className="flow-head-sub">Select a Trigger</div>
            <div className="flow-head-title">When to run automation</div>
          </div>
        </div>

        <div className="trigger-list">
          {TRIGGERS.map((t) => (
            <button
              key={t.type}
              type="button"
              className={`trigger-row${form.type === t.type ? " selected" : ""}${t.soon ? " disabled" : ""}`}
              onClick={() => !t.soon && set("type", t.type)}
            >
              <span className="trigger-ic">{t.brand ? <IgGlyph /> : <ChatGlyph />}</span>
              <span className="trigger-label">{t.label}</span>
              {t.soon && <span className="soon-badge">Coming Soon</span>}
              {!t.soon && <span className={`radio${form.type === t.type ? " on" : ""}`} />}
            </button>
          ))}
        </div>

        {/* Trigger configuration */}
        <div className="trigger-config">
          <div className="field">
            <label>Instagram account</label>
            <select
              value={form.igAccountId ?? ""}
              onChange={(e) => set("igAccountId", e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Select…</option>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>@{a.igUsername || a.igUserId}</option>
              ))}
            </select>
          </div>

          {isComment && (
            <div className="field">
              <label>Specific post/reel ID (optional — leave blank for all posts)</label>
              <input value={form.igMediaId || ""} onChange={(e) => set("igMediaId", e.target.value)} placeholder="17895…" />
            </div>
          )}

          <label className="inline-check">
            <input type="checkbox" checked={form.matchAny} onChange={(e) => set("matchAny", e.target.checked)} />
            Trigger on any {form.type === "DM" ? "message" : "comment"} (ignore keywords)
          </label>

          {!form.matchAny && (
            <div className="field" style={{ marginTop: 12 }}>
              <label>Keywords</label>
              <div className="kw-list">
                {form.keywords.map((k, i) => (
                  <div className="kw-row" key={i}>
                    <input value={k.keyword} onChange={(e) => setKeyword(i, e.target.value)} placeholder="e.g. link" />
                    {form.keywords.length > 1 && (
                      <button className="icon-btn" onClick={() => removeKeyword(i)}>✕</button>
                    )}
                  </div>
                ))}
              </div>
              <button className="link-btn" onClick={addKeyword}>+ Add keyword</button>
            </div>
          )}

          {isComment && (
            <div className="field" style={{ marginTop: 12 }}>
              <label>Public reply on the comment (optional)</label>
              <input value={form.publicReply || ""} onChange={(e) => set("publicReply", e.target.value)} placeholder="Sent you a DM! 📩" />
            </div>
          )}
        </div>
      </div>

      {/* Response Flow */}
      <h2 className="flow-section-title">Response Flow</h2>

      {/* Bot protection (premium placeholder) */}
      <div className="flow-card locked-card">
        <div className="locked-row">
          <span className="flow-head-icon flow-head-icon--green"><ShieldGlyph /></span>
          <div className="locked-text">
            <div className="locked-title">
              <span className="mini-badge mini-badge--safe">ACCOUNT SAFETY</span>
              <span className="mini-badge mini-badge--max">MAX</span>
            </div>
            <div className="locked-name">Bot Protection</div>
            <div className="locked-desc">
              Protect your Instagram account by sending random approved variants and reducing
              repeated DM patterns Instagram may flag as bot-like.
            </div>
          </div>
          <div className="locked-toggle"><span className="paused-label">PRO</span><Toggle on={false} disabled /></div>
        </div>
      </div>

      {/* Ask to follow (maps to real askFollow fields) */}
      <div className="flow-card">
        <div className="opening-row">
          <span className="flow-head-icon flow-head-icon--muted"><ChatGlyph /></span>
          <div className="locked-text">
            <div className="locked-name">Ask to follow first {!form.askFollowEnabled && <span className="off-tag">OFF</span>}</div>
            <div className="locked-desc">Require the user to follow you before the response flow runs.</div>
          </div>
          <Toggle on={form.askFollowEnabled} onChange={(v) => set("askFollowEnabled", v)} />
        </div>
        {form.askFollowEnabled && (
          <div className="field" style={{ marginTop: 14 }}>
            <label>Ask-for-follow message</label>
            <input value={form.askFollowMessage || ""} onChange={(e) => set("askFollowMessage", e.target.value)}
                   placeholder="Follow us first, then reply here to unlock your link!" />
          </div>
        )}
      </div>

      {/* Response steps */}
      {form.steps.map((s, i) => (
        <div className="flow-card response-card" key={i}>
          <div className="response-head">
            <span className="response-index">{i + 1}</span>
            <select value={s.stepType} onChange={(e) => setStep(i, { stepType: e.target.value })}>
              <option value="TEXT">Text message</option>
              <option value="BUTTONS">Message with link buttons</option>
              <option value="QUESTION">Question (collect data)</option>
            </select>
            {form.steps.length > 1 && (
              <button className="icon-btn danger" onClick={() => removeStep(i)} title="Remove">✕</button>
            )}
          </div>

          <div className="field" style={{ marginTop: 12 }}>
            <label>{s.stepType === "QUESTION" ? "Question text" : "Message"}</label>
            <textarea rows={2} value={s.body || ""} onChange={(e) => setStep(i, { body: e.target.value })}
                      placeholder={s.stepType === "QUESTION" ? "What's your best email?" : "Here's your link!"} />
          </div>

          {s.stepType === "BUTTONS" && (
            <ButtonsEditor buttons={s.buttons || []} onChange={(b) => setStep(i, { buttons: b })} />
          )}

          {s.stepType === "QUESTION" && (
            <div className="row-flex">
              <div className="field" style={{ flex: 1 }}>
                <label>Store answer as</label>
                <select value={s.collectField} onChange={(e) => setStep(i, { collectField: e.target.value })}>
                  <option value="EMAIL">Email</option>
                  <option value="PHONE">Phone</option>
                  <option value="NAME">Name</option>
                  <option value="CUSTOM">Custom field</option>
                </select>
              </div>
              {s.collectField === "CUSTOM" && (
                <div className="field" style={{ flex: 1 }}>
                  <label>Custom field key</label>
                  <input value={s.customKey || ""} onChange={(e) => setStep(i, { customKey: e.target.value })} placeholder="city" />
                </div>
              )}
            </div>
          )}
        </div>
      ))}

      <button className="add-response-btn" onClick={addStep}>+ Add Response</button>

      {/* Follow-up (premium placeholder) */}
      <div className="flow-card locked-card">
        <div className="opening-row">
          <span className="flow-head-icon flow-head-icon--muted"><ChatGlyph /></span>
          <div className="locked-text">
            <div className="locked-name">Follow-up Message <span className="mini-badge mini-badge--pro">PRO</span></div>
            <div className="locked-desc">Send a nudge after the automation completes (delay: 1 min to 23 hr 30 min).</div>
          </div>
          <Toggle on={false} disabled />
        </div>
      </div>
    </div>
  );
}

function ButtonsEditor({ buttons, onChange }) {
  const set = (i, patch) => onChange(buttons.map((b, idx) => (idx === i ? { ...b, ...patch } : b)));
  const add = () => onChange([...buttons, { title: "", url: "" }]);
  const remove = (i) => onChange(buttons.filter((_, idx) => idx !== i));
  return (
    <div style={{ marginTop: 4 }}>
      <label style={{ fontWeight: 600, fontSize: 14 }}>Buttons</label>
      {buttons.map((b, i) => (
        <div className="row-flex" key={i} style={{ marginTop: 8 }}>
          <input placeholder="Button label" value={b.title || ""} onChange={(e) => set(i, { title: e.target.value })} />
          <input placeholder="https://your-link.com" value={b.url || ""} onChange={(e) => set(i, { url: e.target.value })} />
          <button className="icon-btn" onClick={() => remove(i)}>✕</button>
        </div>
      ))}
      <button className="link-btn" style={{ marginTop: 10 }} onClick={add}>+ Add button</button>
    </div>
  );
}
