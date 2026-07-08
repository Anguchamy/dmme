import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { brand } from "../config/brand";
import { api } from "../lib/api";
import Backdrop3D from "../components/Backdrop3D";
import Tilt from "../components/Tilt";

const FEATURES = [
  ["Comment automation", "Reply to comments and slide into the DMs with your link instantly."],
  ["Story automation", "Auto-respond to story replies and reactions in real time."],
  ["Live automation", "Message followers who engage while you're live."],
  ["DM automation", "Auto-reply to anyone who messages you, day or night."],
  ["Ask for follow", "Gate your link behind a follow so viewers become followers."],
  ["Re-trigger", "Re-run automations on older posts so no lead slips away."],
  ["Lead capture", "Ask questions in the DM to collect emails and phone numbers."],
  ["Analytics", "Track DMs, replies, and conversions from one dashboard."],
];

const STEPS = [
  ["Step 1", "Choose a trigger", "Pick the keywords that activate your automation."],
  ["Step 2", "Build the reply", "Craft the DM flow with links, offers, and questions."],
  ["Step 3", "Grow on autopilot", "Let automation handle replies while you create."],
];

// Sample testimonials (illustrative placeholders — replace with your own).
const REVIEWS = [
  ["Aditya R.", "Creator", "Setup took five minutes and my Reels now convert comments into email signups automatically."],
  ["Neha S.", "Boutique owner", "The ask-for-follow gate doubled my follower growth in a month. Genuinely simple to use."],
  ["Karan M.", "Coach", "I collect leads in my sleep now. The DM question flow is exactly what I needed."],
];

const FAQS = [
  ["Is there a free plan?", "Yes. The free plan includes 1,000 DM replies a month and unlimited automations — no card required."],
  ["Is it safe for my account?", "We use Meta's official Instagram Messaging API, so automations run within Instagram's approved framework."],
  ["Can I upgrade later?", "Absolutely. Start free and move to Pro whenever you're ready for unlimited replies."],
  ["How do I get support?", "Email support on every plan, with priority support on Pro and above."],
];

export default function Landing() {
  const [plans, setPlans] = useState([]);

  useEffect(() => {
    api.get("/api/plans").then(setPlans).catch(() => setPlans([]));
  }, []);

  const price = (p) =>
    p.priceMinor === 0 ? "Free" : `₹${(p.priceMinor / 100).toLocaleString("en-IN")}`;

  return (
    <div>
      <nav className="nav">
        <div className="brand">{brand.name}</div>
        <div className="row-flex">
          <a href="#features">Features</a>
          <a href="#pricing">Pricing</a>
          <Link to="/login" className="btn btn-ghost btn-sm">Sign in</Link>
          <Link to="/login" className="btn btn-primary btn-sm">Start free</Link>
        </div>
      </nav>

      <header className="hero">
        <Backdrop3D />
        <h1>{brand.tagline}</h1>
        <p>{brand.subtitle}</p>
        <Link to="/login" className="btn btn-primary">Start for free</Link>
        <div className="badges">
          <span>✓ Meta official API</span>
          <span>✓ No credit card</span>
          <span>✓ Instant setup</span>
        </div>
      </header>

      <div className="stats">
        <div className="stat"><div className="num">10k+</div><div className="lbl">Creators</div></div>
        <div className="stat"><div className="num">30M+</div><div className="lbl">DMs sent</div></div>
        <div className="stat"><div className="num">5M+</div><div className="lbl">Leads captured</div></div>
        <div className="stat"><div className="num">12+</div><div className="lbl">Countries</div></div>
      </div>

      <section className="section" id="features">
        <h2>Everything you need</h2>
        <p className="lead">Unlock the full power of Instagram automation.</p>
        <div className="grid">
          {FEATURES.map(([t, d]) => (
            <Tilt className="card" key={t}>
              <h3>{t}</h3>
              <p>{d}</p>
            </Tilt>
          ))}
        </div>
      </section>

      <section className="section" style={{ background: "var(--bg-soft)" }}>
        <h2>3 easy steps</h2>
        <p className="lead">Unlimited possibilities.</p>
        <div className="steps">
          {STEPS.map(([n, t, d]) => (
            <Tilt className="step card" key={n} max={7}>
              <div className="n">{n}</div>
              <h3>{t}</h3>
              <p>{d}</p>
            </Tilt>
          ))}
        </div>
      </section>

      <section className="section" id="pricing">
        <h2>Simple, transparent pricing</h2>
        <p className="lead">Start free, upgrade when you're ready.</p>
        <div className="pricing">
          {plans.map((p) => (
            <Tilt className={`plan ${p.code === "PRO" ? "featured" : ""}`} key={p.code} max={6}>
              <h3>{p.name}</h3>
              <div className="price">
                {price(p)}
                {p.priceMinor > 0 && <span style={{ fontSize: 15, color: "var(--muted)" }}> /mo</span>}
              </div>
              <ul>
                {(p.features || []).map((f) => (
                  <li key={f}>{f}</li>
                ))}
              </ul>
              <Link to="/login" className="btn btn-primary" style={{ width: "100%", justifyContent: "center" }}>
                {p.code === "AGENCY" ? "Contact us" : "Get started"}
              </Link>
            </Tilt>
          ))}
        </div>
      </section>

      <section className="section" style={{ background: "var(--bg-soft)" }}>
        <h2>What people are saying</h2>
        <p className="lead">Loved by creators and small businesses.</p>
        <div className="reviews">
          {REVIEWS.map(([who, role, text]) => (
            <Tilt className="review card" key={who} max={7}>
              <div className="stars">★★★★★</div>
              <div className="who">{who}</div>
              <div style={{ color: "var(--muted)", fontSize: 13, marginBottom: 10 }}>{role}</div>
              <p>{text}</p>
            </Tilt>
          ))}
        </div>
      </section>

      <section className="section faq">
        <h2>All questions answered</h2>
        <div style={{ maxWidth: 720, margin: "30px auto 0" }}>
          {FAQS.map(([q, a]) => (
            <details key={q}>
              <summary>{q}</summary>
              <p>{a}</p>
            </details>
          ))}
        </div>
      </section>

      <footer className="footer">
        <div className="brand" style={{ marginBottom: 8 }}>{brand.name}</div>
        <div className="row-flex" style={{ justifyContent: "center", gap: 16, marginBottom: 8 }}>
          <a href="#features">Features</a>
          <a href="#pricing">Pricing</a>
          <Link to="/privacy">Privacy</Link>
        </div>
        <div>© {new Date().getFullYear()} {brand.name}. Not affiliated with Instagram or Meta.</div>
      </footer>
    </div>
  );
}
