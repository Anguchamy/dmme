import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Logo, LogoMark } from "../components/Logo";
import { api } from "../lib/api";

/** Supabase email confirm / recovery links land on Site URL with a hash. Forward to /login. */
function useForwardAuthHash() {
  useEffect(() => {
    const hash = window.location.hash || "";
    if (
      hash.includes("error=") ||
      hash.includes("access_token") ||
      hash.includes("type=signup") ||
      hash.includes("type=recovery") ||
      hash.includes("type=email")
    ) {
      window.location.replace(`/login${hash}`);
    }
  }, []);
}

const CREATORS = [
  { handle: "@elementec", tag: "Travel", img: "https://picsum.photos/seed/dmme-travel/300/420" },
  { handle: "@ezsnippet", tag: "Education", img: "https://picsum.photos/seed/dmme-edu/300/420" },
  { handle: "@fit.saswati", tag: "Yoga", img: "https://picsum.photos/seed/dmme-yoga/300/420" },
  { handle: "@glowbyrhea", tag: "Beauty", img: "https://picsum.photos/seed/dmme-beauty/300/420" },
  { handle: "@coach.dev", tag: "Startup", img: "https://picsum.photos/seed/dmme-startup/300/420" },
  { handle: "@thefoodloop", tag: "Local", img: "https://picsum.photos/seed/dmme-local/300/420" },
];

const FEATURES = [
  { title: "Comment → DM", desc: "Reply in comments and send the DM automatically." },
  { title: "Story replies", desc: "Catch story reactions and replies 24/7." },
  { title: "Live + DMs", desc: "Engage viewers during lives and inbox chats." },
  { title: "Ask for follow", desc: "Gate links behind a real follow." },
  { title: "Collect data", desc: "Build email lists inside the conversation." },
  { title: "Re-trigger", desc: "Revive old posts without starting over." },
];

const STEPS = [
  { n: "01", title: "Choose trigger", desc: "Keywords on comments, stories, lives, or DMs." },
  { n: "02", title: "Write the reply", desc: "Add your offer, link, or follow ask." },
  { n: "03", title: "Go live", desc: "Automations run while you create." },
];

const FAQS = [
  ["Is dmme free?", "Yes. Start free with monthly DM replies and unlimited automations — no card required."],
  ["Is dmme safe?", "We use Meta's official Instagram Messaging API within approved platform rules."],
  ["How do I get support?", "Email contact@dmme.co.in anytime. Priority support is on paid plans."],
];

const STATS = [
  ["30M+", "DMs sent"],
  ["5M+", "Followers gained"],
  ["10M+", "Comments handled"],
  ["12+", "Countries"],
];

function CheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7.5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function formatPlanPrice(plan) {
  return plan.priceMinor === 0
    ? "Free"
    : `₹${(plan.priceMinor / 100).toLocaleString("en-IN")}/mo`;
}

export default function Landing() {
  const [plans, setPlans] = useState([]);
  useForwardAuthHash();

  useEffect(() => {
    api.get("/api/plans").then(setPlans).catch(() => {});
  }, []);

  return (
    <div className="bento-landing">
      <div className="wall">
        {/* Hero */}
        <section className="tile tile--paper bento-hero">
          <div className="bento-bar">
            <Link to="/" aria-label="dmme home">
              <Logo size={30} color="var(--ink)" />
            </Link>
            <div className="bento-nav-links">
              <a href="#features">Features</a>
              <a href="#pricing">Pricing</a>
              <a href="#faq">FAQ</a>
            </div>
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <Link to="/login" className="pill-btn pill-btn--ghost pill-btn--sm" style={{ color: "var(--ink)" }}>
                Login
              </Link>
              <Link to="/login" className="pill-btn pill-btn--ink pill-btn--sm">
                Join free
              </Link>
            </div>
          </div>

          <div style={{ display: "inline-flex", alignItems: "center", gap: 8, fontWeight: 700, fontSize: 14, color: "#51525c" }}>
            <LogoMark size={22} />
            dmme × Meta
          </div>

          <h1>
            Go viral on IG with{" "}
            <span className="mark mark--brand">DM</span>{" "}
            <span className="mark mark--orange">automation</span>
          </h1>
          <p className="bento-hero-lead">
            Auto-respond to comments, stories, and DMs — keep the algorithm happy and convert every reply.
          </p>
          <div className="bento-hero-actions">
            <Link to="/login" className="pill-btn pill-btn--ink pill-btn--lg">
              Start for free
            </Link>
            <a href="#how" className="pill-btn pill-btn--ghost" style={{ color: "var(--ink)" }}>
              See how it works
            </a>
          </div>
          <div className="bento-trust">
            <span><CheckIcon /> Meta Verified API</span>
            <span><CheckIcon /> No credit card</span>
            <span><CheckIcon /> Instant setup</span>
          </div>
        </section>

        {/* Creators + stats */}
        <section className="tile tile--orange tile--wide">
          <h2 className="bento-tile-title">Creators already shipping with dmme</h2>
          <p className="bento-tile-body">10k+ accounts automate comment → DM flows every day.</p>
          <div className="bento-creators">
            {CREATORS.map((c) => (
              <div className="bento-creator" key={c.handle}>
                <img src={c.img} alt="" loading="lazy" />
                <div className="bento-creator-meta">
                  {c.handle}
                  <div style={{ opacity: 0.7, fontWeight: 600 }}>{c.tag}</div>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="tile tile--purple tile--narrow">
          <h2 className="bento-tile-title">By the numbers</h2>
          <div className="bento-stats">
            {STATS.map(([num, lbl]) => (
              <div key={lbl}>
                <div className="bento-stat-num">{num}</div>
                <div className="bento-stat-lbl">{lbl}</div>
              </div>
            ))}
          </div>
        </section>

        {/* Features */}
        <section className="tile tile--green" id="features">
          <h2 className="bento-tile-title">Everything you need to convert Instagram traffic</h2>
          <p className="bento-tile-body">One workspace for comments, stories, lives, and inbox — keyword triggers included.</p>
          <ul className="bento-feature-list" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 10 }}>
            {FEATURES.map((f) => (
              <li key={f.title}>
                <strong>{f.title}</strong>
                <span>{f.desc}</span>
              </li>
            ))}
          </ul>
        </section>

        {/* How it works */}
        <section className="tile tile--sky" id="how">
          <h2 className="bento-tile-title">
            Three steps. <span className="mark mark--brand">Unlimited</span> replies.
          </h2>
          <div className="bento-steps">
            {STEPS.map((s) => (
              <div className="bento-step" key={s.n}>
                <div className="bento-step-n">STEP {s.n}</div>
                <h3>{s.title}</h3>
                <p>{s.desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Pricing */}
        {plans.length > 0 && (
          <section className="tile tile--paper" id="pricing">
            <h2 className="bento-tile-title" style={{ color: "var(--ink)" }}>
              Simple pricing
            </h2>
            <p className="bento-tile-body" style={{ color: "#51525c" }}>
              Start free. Upgrade when volume grows.
            </p>
            <div className="bento-pricing-grid">
              {plans.map((plan) => {
                const featured = plan.code === "PRO";
                return (
                  <article className={`bento-plan${featured ? " bento-plan--featured" : ""}`} key={plan.code}>
                    <h3>{plan.name}</h3>
                    <div className="bento-plan-price">{formatPlanPrice(plan)}</div>
                    <ul>
                      {(plan.features || []).slice(0, 5).map((feature) => (
                        <li key={feature}>{feature}</li>
                      ))}
                    </ul>
                    <Link
                      to="/login"
                      className={`pill-btn pill-btn--sm ${featured ? "pill-btn--paper" : "pill-btn--ink"}`}
                      style={{ marginTop: "auto", alignSelf: "flex-start" }}
                    >
                      {plan.code === "AGENCY" ? "Contact us" : "Get started"}
                    </Link>
                  </article>
                );
              })}
            </div>
          </section>
        )}

        {/* Reviews */}
        <section className="tile tile--surface tile--two-thirds">
          <h2 className="bento-tile-title">What people are saying</h2>
          <div className="bento-reviews">
            <article className="bento-review">
              <strong>Kushank</strong>
              <div className="handle">@khushankmathurcuet</div>
              <p>Gained 6000+ followers in 20 days. Ask-for-follow on Reels is a game-changer.</p>
            </article>
            <article className="bento-review">
              <strong>Shruti</strong>
              <div className="handle">@desi.potatoo</div>
              <p>Story and comment automations run 24/7 while I focus on content.</p>
            </article>
            <article className="bento-review">
              <strong>Aditya</strong>
              <div className="handle">@aditya.creates</div>
              <p>Five-minute setup. Comments become email signups automatically.</p>
            </article>
          </div>
        </section>

        {/* CTA */}
        <section className="tile tile--brand tile--third" style={{ justifyContent: "center", gap: 16 }}>
          <h2 className="bento-tile-title">Your Circle starts with a yes</h2>
          <p className="bento-tile-body" style={{ opacity: 0.9, color: "#fff" }}>
            Soft-launch today. Full public comment→DM after Meta approval.
          </p>
          <Link to="/login" className="pill-btn pill-btn--paper pill-btn--lg" style={{ alignSelf: "flex-start" }}>
            Start for free
          </Link>
        </section>

        {/* FAQ */}
        <section className="tile tile--paper" id="faq">
          <h2 className="bento-tile-title" style={{ color: "var(--ink)" }}>
            Questions, <span className="mark mark--sky">answered</span>
          </h2>
          <div className="bento-faq">
            {FAQS.map(([q, a]) => (
              <details key={q}>
                <summary>{q}</summary>
                <p>{a}</p>
              </details>
            ))}
          </div>
        </section>

        {/* Footer */}
        <footer className="tile tile--surface">
          <div className="bento-bar" style={{ flexWrap: "wrap" }}>
            <Logo size={28} color="var(--paper)" />
            <div className="bento-footer-links">
              <a href="#pricing">Pricing</a>
              <Link to="/terms">Terms</Link>
              <Link to="/privacy">Privacy</Link>
              <Link to="/contact">Contact</Link>
              <Link to="/refund">Refunds</Link>
            </div>
          </div>
          <p className="muted" style={{ fontSize: 14 }}>
            Automate Instagram DMs and grow with 24/7 engagement. © 2026 dmme.
          </p>
        </footer>
      </div>
    </div>
  );
}
