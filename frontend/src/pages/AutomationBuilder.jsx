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
    api.get("/api/instagram/accounts").then(setAccounts).catch(() => {});
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

  // ---- keywords ----
  const setKeyword = (i, v) =>
    set("keywords", form.keywords.map((k, idx) => (idx === i ? { ...k, keyword: v } : k)));
  const addKeyword = () => set("keywords", [...form.keywords, { keyword: "", matchType: "CONTAINS" }]);
  const removeKeyword = (i) => set("keywords", form.keywords.filter((_, idx) => idx !== i));

  // ---- steps ----
  const setStep = (i, patch) =>
    set("steps", form.steps.map((s, idx) => (idx === i ? { ...s, ...patch } : s)));
  const addStep = () => set("steps", [...form.steps, emptyStep()]);
  const removeStep = (i) => set("steps", form.steps.filter((_, idx) => idx !== i));

  async function save(activate) {
    setErr("");
    setBusy(true);
    const payload = {
      ...form,
      status: activate ? "ACTIVE" : form.status,
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

  return (
    <div style={{ maxWidth: 760 }}>
      <h1>{editing ? "Edit automation" : "New automation"}</h1>
      {err && <div className="err" style={{ color: "#d33" }}>{err}</div>}

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="field">
          <label>Name</label>
          <input value={form.name} onChange={(e) => set("name", e.target.value)} placeholder="Reel link giveaway" />
        </div>
        <div className="row-flex">
          <div className="field" style={{ flex: 1 }}>
            <label>Trigger channel</label>
            <select value={form.type} onChange={(e) => set("type", e.target.value)}>
              <option value="COMMENT">Comment on a post/reel</option>
              <option value="DM">Direct message</option>
              <option value="STORY_REPLY">Story reply</option>
              <option value="LIVE">Live</option>
            </select>
          </div>
          <div className="field" style={{ flex: 1 }}>
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
        </div>
        {form.type === "COMMENT" && (
          <div className="field">
            <label>Specific post/reel ID (optional — leave blank for all posts)</label>
            <input value={form.igMediaId || ""} onChange={(e) => set("igMediaId", e.target.value)} placeholder="17895..." />
          </div>
        )}
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <h3>Trigger</h3>
        <label className="row-flex" style={{ gap: 8 }}>
          <input type="checkbox" checked={form.matchAny} onChange={(e) => set("matchAny", e.target.checked)} />
          Trigger on any comment/message (ignore keywords)
        </label>
        {!form.matchAny && (
          <div style={{ marginTop: 12 }}>
            <label style={{ fontWeight: 600, fontSize: 14 }}>Keywords</label>
            {form.keywords.map((k, i) => (
              <div className="row-flex" key={i} style={{ marginTop: 8 }}>
                <input value={k.keyword} onChange={(e) => setKeyword(i, e.target.value)} placeholder="link" />
                {form.keywords.length > 1 && (
                  <button className="btn btn-ghost btn-sm" onClick={() => removeKeyword(i)}>✕</button>
                )}
              </div>
            ))}
            <button className="btn btn-ghost btn-sm" style={{ marginTop: 10 }} onClick={addKeyword}>+ Add keyword</button>
          </div>
        )}
        {form.type === "COMMENT" && (
          <div className="field" style={{ marginTop: 16 }}>
            <label>Public reply on the comment (optional)</label>
            <input value={form.publicReply || ""} onChange={(e) => set("publicReply", e.target.value)}
                   placeholder="Sent you a DM! 📩" />
          </div>
        )}
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <h3>Ask for follow</h3>
        <label className="row-flex" style={{ gap: 8 }}>
          <input type="checkbox" checked={form.askFollowEnabled}
                 onChange={(e) => set("askFollowEnabled", e.target.checked)} />
          Require the user to follow before sending the flow
        </label>
        {form.askFollowEnabled && (
          <div className="field" style={{ marginTop: 12 }}>
            <label>Ask-for-follow message</label>
            <input value={form.askFollowMessage || ""} onChange={(e) => set("askFollowMessage", e.target.value)}
                   placeholder="Follow us first, then reply here to unlock your link!" />
          </div>
        )}
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <h3>DM flow</h3>
        <p style={{ color: "var(--muted)", marginTop: 0, fontSize: 14 }}>
          Messages are sent in order. Use a Question step to collect the follower's details.
        </p>
        {form.steps.map((s, i) => (
          <div className="step-card" key={i}>
            <div className="row-flex" style={{ justifyContent: "space-between" }}>
              <select value={s.stepType} onChange={(e) => setStep(i, { stepType: e.target.value })}>
                <option value="TEXT">Text message</option>
                <option value="BUTTONS">Message with link buttons</option>
                <option value="QUESTION">Question (collect data)</option>
              </select>
              {form.steps.length > 1 && (
                <button className="btn btn-ghost btn-sm" onClick={() => removeStep(i)}>Remove</button>
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
                    <input value={s.customKey || ""} onChange={(e) => setStep(i, { customKey: e.target.value })}
                           placeholder="city" />
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
        <button className="btn btn-ghost btn-sm" onClick={addStep}>+ Add step</button>
      </div>

      <div className="row-flex">
        <button className="btn btn-ghost" disabled={busy} onClick={() => save(false)}>Save as draft</button>
        <button className="btn btn-primary" disabled={busy} onClick={() => save(true)}>Save & activate</button>
      </div>
    </div>
  );
}

function ButtonsEditor({ buttons, onChange }) {
  const set = (i, patch) => onChange(buttons.map((b, idx) => (idx === i ? { ...b, ...patch } : b)));
  const add = () => onChange([...buttons, { title: "", url: "" }]);
  const remove = (i) => onChange(buttons.filter((_, idx) => idx !== i));
  return (
    <div>
      <label style={{ fontWeight: 600, fontSize: 14 }}>Buttons</label>
      {buttons.map((b, i) => (
        <div className="row-flex" key={i} style={{ marginTop: 8 }}>
          <input placeholder="Button label" value={b.title || ""} onChange={(e) => set(i, { title: e.target.value })} />
          <input placeholder="https://your-link.com" value={b.url || ""} onChange={(e) => set(i, { url: e.target.value })} />
          <button className="btn btn-ghost btn-sm" onClick={() => remove(i)}>✕</button>
        </div>
      ))}
      <button className="btn btn-ghost btn-sm" style={{ marginTop: 10 }} onClick={add}>+ Add button</button>
    </div>
  );
}
