import { Link } from "react-router-dom";
import { Logo, LogoMark } from "../components/Logo";

const CREATORS = [
  { handle: "@elementec", followers: "1.2M+ Followers", tag: "Travel", img: "https://picsum.photos/seed/dmme-travel/300/420" },
  { handle: "@ezsnippet", followers: "3.3M+ Followers", tag: "Education", img: "https://picsum.photos/seed/dmme-edu/300/420" },
  { handle: "@fit.saswati", followers: "35K+ Followers", tag: "Yoga", img: "https://picsum.photos/seed/dmme-yoga/300/420" },
  { handle: "@glowbyrhea", followers: "670K+ Followers", tag: "Beauty", img: "https://picsum.photos/seed/dmme-beauty/300/420" },
  { handle: "@coach.dev", followers: "95K+ Followers", tag: "Startup", img: "https://picsum.photos/seed/dmme-startup/300/420" },
  { handle: "@thefoodloop", followers: "820K+ Followers", tag: "Local Business", img: "https://picsum.photos/seed/dmme-local/300/420" },
  { handle: "@hardikpandyaa93", followers: "1M+ Followers", tag: "Lifestyle", img: "https://picsum.photos/seed/dmme-lifestyle/300/420" },
  { handle: "@aria.beats", followers: "2.1M+ Followers", tag: "Music", img: "https://picsum.photos/seed/dmme-music/300/420" },
];

const FEATURES = [
  {
    title: "Comment Automation",
    desc: "Reply to comments and send a DM to engage your followers.",
    mock: "comment",
  },
  {
    title: "Story Automation",
    desc: "Auto respond to story replies and reactions.",
    mock: "story",
  },
  {
    title: "Live Automation",
    desc: "Send a message to followers who are active during lives.",
    mock: "live",
  },
  {
    title: "DM Automation",
    desc: "Automatically reply to the followers who messages you.",
    mock: "dm",
  },
  {
    title: "Ask For Follow",
    desc: "Ask users to follow you before sending the message.",
    mock: "follow",
  },
  {
    title: "Re-trigger",
    desc: "Re-trigger automations for old posts and never loose customers.",
    mock: "retrigger",
  },
  {
    title: "Collect User Data",
    desc: "Create your email list to re target audience.",
    mock: "data",
  },
  {
    title: "dmme AI",
    desc: "Convert more users with the help of AI ✨",
    mock: "ai",
    comingSoon: true,
  },
];

const STEPS = [
  {
    n: "STEP 1",
    title: "Choose Trigger",
    desc: "Choose which keywords activate your automation.",
    icon: "cursor",
  },
  {
    n: "STEP 2",
    title: "Automate Response",
    desc: "Setup custom responses with links and offers to share.",
    icon: "envelope",
  },
  {
    n: "STEP 3",
    title: "Go Viral 🚀",
    desc: "Let automations do the magic while you focus on creating.",
    icon: "rocket",
  },
];

const REVIEWS = [
  {
    name: "Kushank",
    handle: "@khushankmathurcuet",
    text: "I gained 6000+ followers in just 20 days using dmme. The ask-for-follow automation is a game-changer for Reels.",
    img: "https://picsum.photos/seed/dmme-kushank/80/80",
  },
  {
    name: "Manoj",
    handle: "@missionudyog",
    text: "A must-have for any creator. Comment-to-DM flows convert way better than link-in-bio. Genuinely simple setup.",
    img: "https://picsum.photos/seed/dmme-manoj/80/80",
  },
  {
    name: "Shruti",
    handle: "@desi.potatoo",
    text: "Love the easy UI and multiple features. Story and comment automations run 24/7 while I focus on content.",
    img: "https://picsum.photos/seed/dmme-shruti/80/80",
  },
  {
    name: "Aditya",
    handle: "@aditya.creates",
    text: "Setup took five minutes and my Reels now convert comments into email signups automatically.",
    img: "https://picsum.photos/seed/dmme-aditya/80/80",
  },
];

const FAQS = [
  [
    "Is dmme free?",
    "Yes. Start free with generous monthly DM replies and unlimited automations — no credit card required.",
  ],
  [
    "Is dmme safe to use?",
    "We use Meta's official Instagram Messaging API, so automations run within Instagram's approved framework.",
  ],
  [
    "How do I contact customer support if I need help?",
    "Email us anytime at contact@dmme.co.in. We respond on every plan, with priority support on paid tiers.",
  ],
];

const LANDING_STATS = [
  ["30M+", "DMs Sent"],
  ["5M+", "Followers Gained"],
  ["10M+", "Comments Sent"],
  ["12+", "Countries"],
];

function MetaLogo() {
  return (
    <svg
      className="landing-meta-logo"
      width="20"
      height="13"
      viewBox="0 0 287 191"
      fill="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="metaLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#0064E0" />
          <stop offset="100%" stopColor="#0082FB" />
        </linearGradient>
      </defs>
      <path
        d="M31.06 126.2c0 10.9 2.39 19.27 5.52 24.33 4.1 6.64 10.21 9.45 16.45 9.45 8.05 0 15.41-2 29.59-21.61 11.36-15.72 24.75-37.79 33.75-51.62l15.25-23.42c10.59-16.27 22.84-34.36 36.9-46.63C179.94 6.42 192.53 0 205.19 0c21.26 0 41.51 12.32 57 35.42 16.95 25.3 25.18 57.16 25.18 90.04 0 19.55-3.86 33.91-10.42 45.26-6.34 10.98-18.71 21.95-39.5 21.95v-31.34c17.81 0 22.26-16.36 22.26-35.09 0-26.7-6.23-56.33-19.96-77.5-9.74-15.02-22.36-24.19-36.25-24.19-15.02 0-27.11 11.33-40.7 31.52-7.22 10.73-14.63 23.8-22.95 38.55l-9 15.94c-18.08 32.04-22.66 39.34-31.71 51.41C63.55 174.2 50.2 191 33.51 191 12.04 191 0 166.17 0 129.83l31.06-3.63Z"
        fill="url(#metaLogoGradient)"
      />
      <path
        d="M24.49 37.6C38.85 15.47 59.57 0 83.34 0 97.1 0 110.78 4.07 125.07 15.73c15.63 12.75 32.29 33.74 53.09 68.28l7.46 12.42c18 30 28.24 45.43 34.24 52.71 7.71 9.35 13.11 12.13 20.13 12.13 17.81 0 22.26-16.36 22.26-35.09l27.4-.86c0 19.55-3.86 33.91-10.42 45.26-6.34 10.98-18.71 21.95-39.5 21.95-12.93 0-24.39-2.81-37.06-14.76-9.74-9.17-21.13-25.45-29.89-40.11l-26.06-43.52c-13.08-21.85-25.08-38.12-32.03-45.5-7.48-7.93-17.09-17.51-32.42-17.51-12.41 0-22.95 8.71-31.77 22.04L24.49 37.6Z"
        fill="url(#metaLogoGradient)"
      />
      <path
        d="M83.94 30.98c-12.41 0-22.95 8.71-31.77 22.04-12.47 18.83-20.11 46.9-20.11 73.83 0 11.1 2.43 19.62 5.53 24.68L11.71 165.9C4.36 154.66 0 137.4 0 129.83 0 98.7 8.29 66.03 25.42 40.71 40.51 18.38 62.31 3.35 87.42 3.35l-3.48 27.63Z"
        fill="url(#metaLogoGradient)"
      />
    </svg>
  );
}

function IgIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="2.5" y="2.5" width="19" height="19" rx="5.5" stroke="currentColor" strokeWidth="2" />
      <circle cx="12" cy="12" r="4.2" stroke="currentColor" strokeWidth="2" />
      <circle cx="17.6" cy="6.4" r="1.4" fill="currentColor" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7.5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function TrustBadges({ variant = "hero" }) {
  const items = ["Meta Verified", "No Credit Card", "Instant Setup"];
  return (
    <div className={`landing-trust landing-trust--${variant}`}>
      {items.map((item) => (
        <span key={item}>
          <CheckIcon /> {item}
        </span>
      ))}
    </div>
  );
}

function CreatorCard({ c }) {
  return (
    <div className="creator-card">
      <img src={c.img} alt="" loading="lazy" />
      <div className="creator-meta">
        <span className="cat-chip">{c.tag}</span>
        <div className="creator-handle">{c.handle}</div>
        <div className="creator-followers">
          <IgIcon /> {c.followers}
        </div>
      </div>
    </div>
  );
}

function MarqueeRow({ items }) {
  const loop = [...items, ...items];
  return (
    <div className="marquee-row">
      <div className="marquee-track">
        {loop.map((c, i) => (
          <CreatorCard key={`${c.handle}-${i}`} c={c} />
        ))}
      </div>
    </div>
  );
}

function FeatureMock({ type }) {
  switch (type) {
    case "comment":
      return (
        <div className="landing-fmock landing-fmock--light">
          <div className="landing-fmock-head">Comments</div>
          <div className="landing-fmock-row">
            <span className="landing-fmock-av" />
            <div>
              <strong>Etienne</strong>
              <p>Do you ship in Italy?</p>
            </div>
          </div>
          <div className="landing-fmock-row muted">
            <span className="landing-fmock-av sm" />
            <div>
              <strong>muted_poetry</strong>
              <p>@etienne We ship in all Europe!</p>
            </div>
          </div>
        </div>
      );
    case "story":
      return (
        <div className="landing-fmock landing-fmock--dark">
          <p>React with 🔥 to get early access to tickets!</p>
          <div className="landing-fmock-bubble user">That&apos;s crazyyyy!!</div>
        </div>
      );
    case "live":
      return (
        <div className="landing-fmock landing-fmock--live">
          <span className="landing-live-badge">LIVE</span>
          <div className="landing-fmock-row">
            <span className="landing-fmock-av sm" />
            <div><strong>pierredemilly</strong> joined</div>
          </div>
          <div className="landing-fmock-row">
            <span className="landing-fmock-av sm" />
            <div><strong>katelin</strong> Welcome!</div>
          </div>
        </div>
      );
    case "dm":
      return (
        <div className="landing-fmock landing-fmock--light">
          <div className="landing-fmock-bubble user">Hey! I love your music!</div>
          <div className="landing-fmock-bubble bot">Thank you so much, it means a lot</div>
        </div>
      );
    case "follow":
      return (
        <div className="landing-fmock landing-fmock--light">
          <div className="landing-fmock-bubble bot">Please Follow me for the link</div>
          <div className="landing-fmock-bubble user">I Followed ✅</div>
          <div className="landing-fmock-bubble bot">
            Thanks! Here&apos;s the link 👇
            <span className="landing-fmock-btn">Click Here</span>
          </div>
        </div>
      );
    case "retrigger":
      return (
        <div className="landing-fmock landing-fmock--icon">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none" aria-hidden="true">
            <circle cx="32" cy="32" r="24" stroke="#2b2bff" strokeWidth="3" strokeDasharray="6 4" />
            <path d="M32 14v8M32 42v8M14 32h8M42 32h8" stroke="#2b2bff" strokeWidth="2" />
            <path d="M26 22l4 6 8-10" stroke="#2b2bff" strokeWidth="2.5" strokeLinecap="round" />
          </svg>
          <span>Re-Trigger</span>
        </div>
      );
    case "data":
      return (
        <div className="landing-fmock landing-fmock--light">
          <div className="landing-fmock-bubble bot">Please Share Your Email!</div>
          <div className="landing-fmock-bubble user">ig_user@gmail.com</div>
          <div className="landing-fmock-bubble bot">Sent on Mail 😊</div>
        </div>
      );
    case "ai":
      return (
        <div className="landing-fmock landing-fmock--icon">
          <span className="landing-ai-star">✦</span>
          <span>dmme AI</span>
        </div>
      );
    default:
      return null;
  }
}

function StepIcon({ type }) {
  if (type === "cursor") {
    return (
      <svg width="48" height="48" viewBox="0 0 48 48" fill="none" aria-hidden="true">
        <circle cx="24" cy="24" r="16" stroke="#2b2bff" strokeWidth="2" strokeDasharray="4 3" />
        <path d="M18 30l-4 8 8-4 14-14-6-6-12 12z" fill="#1a1a2e" />
      </svg>
    );
  }
  if (type === "envelope") {
    return (
      <svg width="48" height="48" viewBox="0 0 48 48" fill="none" aria-hidden="true">
        <rect x="8" y="14" width="32" height="22" rx="3" stroke="#1a1a2e" strokeWidth="2" />
        <path d="M8 16l16 12 16-12" stroke="#1a1a2e" strokeWidth="2" />
      </svg>
    );
  }
  return (
    <svg width="48" height="48" viewBox="0 0 48 48" fill="none" aria-hidden="true">
      <path d="M24 8l4 12h12l-10 8 4 12-10-8-10 8 4-12-10-8h12l4-12z" fill="#2b2bff" opacity="0.2" />
      <path d="M24 12v24M16 28h16" stroke="#2b2bff" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

function SocialIcon({ type }) {
  if (type === "instagram") {
    return (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <rect x="2" y="2" width="20" height="20" rx="5" stroke="currentColor" strokeWidth="2" />
        <circle cx="12" cy="12" r="4.5" stroke="currentColor" strokeWidth="2" />
        <circle cx="17.5" cy="6.5" r="1.2" fill="currentColor" />
      </svg>
    );
  }
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M23 7.5a3 3 0 00-2-2C19.3 5 12 5 12 5s-7.3 0-9 0.5a3 3 0 00-2 2A31 31 0 001 12a31 31 0 00.5 4.5 3 3 0 002 2C4.7 19 12 19 12 19s7.3 0 9-.5a3 3 0 002-2A31 31 0 0023 12a31 31 0 00-.5-4.5zM10 15.5v-7l6 3.5-6 3.5z" />
    </svg>
  );
}

function PhoneMockup() {
  return (
    <div className="landing-phone-wrap">
      <div className="landing-float landing-float--followers">
        <span>👤</span>
        <div>
          <strong>1K</strong>
          <small>2X Followers</small>
        </div>
      </div>
      <div className="landing-float landing-float--sales">
        <span>💸</span>
        <small>+50% Sales</small>
      </div>
      <div className="landing-float landing-float--viral">
        <span>🚀</span>
        <small>Go Viral</small>
      </div>

      <div className="landing-phone">
        <div className="landing-phone-notch" />
        <div className="landing-phone-screen">
          <div className="landing-phone-grid" />
          <div className="landing-phone-comment">
            <img src="https://picsum.photos/seed/dmme-sofia/32/32" alt="" />
            <div>
              <strong>Sofia Bennett</strong>
              <span>LINK please! ❤️</span>
            </div>
          </div>
          <div className="landing-phone-dm">
            <p>Here&apos;s the link you asked for. Enjoy!</p>
            <button type="button">Open</button>
          </div>
          <div className="landing-phone-nav">
            <span>⌂</span><span>🔍</span><span>＋</span><span>▶</span><span>👤</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function ChatIllustration({ variant = "convert" }) {
  const photo =
    variant === "convert"
      ? "https://picsum.photos/seed/dmme-convert/520/520"
      : "https://picsum.photos/seed/dmme-boost/520/520";

  if (variant === "convert") {
    return (
      <div className={`landing-illus landing-illus--${variant}`}>
        <img src={photo} alt="" className="landing-illus-photo" />
        <div className="landing-chat landing-chat--1">
          <img src="https://picsum.photos/seed/dmme-creator1/28/28" alt="" />
          <div>
            <p>Wait! Don&apos;t miss out! Follow me first and click on I&apos;m following to grab the link</p>
            <span className="landing-chat-btn">I&apos;m following</span>
          </div>
        </div>
        <div className="landing-chat landing-chat--user">I&apos;m following</div>
        <div className="landing-chat landing-chat--2">
          <img src="https://picsum.photos/seed/dmme-creator1/28/28" alt="" />
          <div>
            <p>Amazing! You did it! Here&apos;s the link and thanks for the follow!</p>
            <span className="landing-chat-btn">Get Guide</span>
          </div>
        </div>
        <div className="landing-illus-badge">👤 1K</div>
      </div>
    );
  }

  return (
    <div className={`landing-illus landing-illus--${variant}`}>
      <img src={photo} alt="" className="landing-illus-photo" />
      <div className="landing-chat landing-chat--1">
        <img src="https://picsum.photos/seed/dmme-creator2/28/28" alt="" />
        <div>
          <p>Hey there! Thanks so much for your interest. Click below and I&apos;ll send you the link in just a sec</p>
          <span className="landing-chat-btn">Send me the link</span>
        </div>
      </div>
      <div className="landing-chat landing-chat--user">Send me the link</div>
      <div className="landing-chat landing-chat--2">
        <img src="https://picsum.photos/seed/dmme-creator2/28/28" alt="" />
        <div>
          <p>Hey! Here&apos;s your link! Enjoy</p>
          <span className="landing-chat-btn">Get Link</span>
        </div>
      </div>
      <div className="landing-illus-badge heart">❤️ 9994</div>
    </div>
  );
}

function SplitSection({ reverse, title, text, variant }) {
  return (
    <section className={`landing-split${reverse ? " landing-split--reverse" : ""}`}>
      <div className="landing-split-visual">
        <ChatIllustration variant={variant} />
      </div>
      <div className="landing-split-copy">
        <h2>{title}</h2>
        <p>{text}</p>
        <Link to="/login" className="landing-btn landing-btn--blue">Start For Free</Link>
        <TrustBadges variant="light" />
      </div>
    </section>
  );
}

export default function Landing() {
  return (
    <div className="landing">
      <nav className="landing-nav">
        <Link to="/" className="landing-nav-logo">
          <Logo size={28} />
        </Link>
        <div className="landing-nav-links">
          <Link to="/login">Pricing</Link>
          <a href="#faq">Help Centre</a>
        </div>
        <div className="landing-nav-actions">
          <Link to="/login" className="landing-nav-login">Login</Link>
          <Link to="/login" className="landing-btn landing-btn--blue landing-btn--sm">Start for free</Link>
        </div>
      </nav>

      <header className="landing-hero">
        <div className="landing-hero-inner">
          <div className="landing-hero-copy">
            <div className="landing-meta-badge">
              <span className="landing-brand-label">
                <LogoMark size={18} />
                dmme
              </span>
              <span className="landing-meta-x">×</span>
              <span className="landing-meta-label">
                <MetaLogo /> Meta
              </span>
            </div>
            <h1>Go Viral On IG with DM Automation</h1>
            <p>
              Keep your audience and the IG algorithm happy by auto-responding to every comment in a DM.
            </p>
            <Link to="/login" className="landing-btn landing-btn--green">Start For Free</Link>
            <TrustBadges variant="hero" />
          </div>
          <div className="landing-hero-visual">
            <PhoneMockup />
          </div>
        </div>
      </header>

      <section className="landing-creators" id="creators">
        <h2>
          <span>10k+</span> Creators Trust dmme 🚀
        </h2>
        <div className="landing-marquee marquee-stack">
          <MarqueeRow items={CREATORS} />
        </div>
        <div className="landing-stats-row">
          {LANDING_STATS.map(([num, lbl]) => (
            <div key={lbl} className="landing-stat">
              <div className="landing-stat-num">{num}</div>
              <div className="landing-stat-lbl">{lbl}</div>
            </div>
          ))}
        </div>
      </section>

      <SplitSection
        variant="convert"
        title="Convert More Followers!"
        text="Ensure your discounts, offers, or exclusives are going to only real, actual Instagram followers with Ask for a Follow automation"
      />

      <SplitSection
        reverse
        variant="boost"
        title="Boost Engagement!"
        text="Auto-respond to every Instagram comment in a DM. Keep your audience (and the algorithm) happy — and watch your revenue grow"
      />

      <section className="landing-features" id="features">
        <p className="landing-overline">ALL THE FEATURES YOU NEED</p>
        <h2>Unlock the full Power of Instagram</h2>
        <div className="landing-features-grid">
          {FEATURES.map((f) => (
            <article className="landing-feature-card" key={f.title}>
              <div className="landing-feature-mock">
                <FeatureMock type={f.mock} />
              </div>
              <div className="landing-feature-body">
                <h3>
                  {f.title}
                  {f.comingSoon && <span className="landing-soon">Coming Soon</span>}
                </h3>
                <p>{f.desc}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-how">
        <p className="landing-overline landing-overline--blue">HOW IT WORKS</p>
        <h2>
          3 Easy Steps, <span>Unlimited</span> Possibilities
        </h2>
        <div className="landing-steps">
          {STEPS.map((s, i) => (
            <div className="landing-step-wrap" key={s.n}>
              <div className="landing-step">
                <span className="landing-step-badge">{s.n}</span>
                <StepIcon type={s.icon} />
                <h3>{s.title}</h3>
                <p>{s.desc}</p>
              </div>
              {i < STEPS.length - 1 && <span className="landing-step-arrow" aria-hidden="true">→</span>}
            </div>
          ))}
        </div>
      </section>

      <section className="landing-reviews">
        <h2>See What People Are Saying 👀</h2>
        <div className="landing-reviews-track">
          {REVIEWS.map((r) => (
            <article className="landing-review-card" key={r.handle}>
              <div className="landing-review-head">
                <img src={r.img} alt="" />
                <div>
                  <strong>{r.name}</strong>
                  <span>{r.handle}</span>
                </div>
              </div>
              <div className="landing-stars">★★★★★</div>
              <p>{r.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-cta-band">
        <Link to="/login" className="landing-btn landing-btn--blue landing-btn--lg">Start For Free</Link>
        <TrustBadges variant="cta" />
      </section>

      <section className="landing-faq" id="faq">
        <div className="landing-faq-head">
          <span className="landing-faq-badge">FAQS</span>
          <h2>
            All Questions <span>Answered</span>
          </h2>
        </div>
        <div className="landing-faq-list">
          {FAQS.map(([q, a]) => (
            <details key={q} className="landing-faq-item">
              <summary>{q}</summary>
              <p>{a}</p>
            </details>
          ))}
        </div>
      </section>

      <footer className="landing-footer">
        <div className="landing-footer-grid">
          <div className="landing-footer-brand">
            <Logo size={28} />
            <p>Automate your Instagram DMs and grow your business with 24/7 engagement.</p>
            <div className="landing-footer-social">
              <a href="https://instagram.com" target="_blank" rel="noreferrer" aria-label="Instagram">
                <SocialIcon type="instagram" />
              </a>
              <a href="https://youtube.com" target="_blank" rel="noreferrer" aria-label="YouTube">
                <SocialIcon type="youtube" />
              </a>
            </div>
            <p className="landing-footer-copy">© 2026 dmme. All rights reserved.</p>
            <div className="landing-security-badge">
              <span className="landing-security-icon" aria-hidden="true" />
              SECURITY PARTNER
            </div>
          </div>
          <div className="landing-footer-col">
            <h4>Company</h4>
            <Link to="/login">Pricing</Link>
            <Link to="/login">Terms &amp; Conditions</Link>
            <Link to="/privacy">Privacy Policy</Link>
            <Link to="/login">Contact Us</Link>
            <Link to="/login">Refund Policy</Link>
          </div>
          <div className="landing-footer-col">
            <h4>Compare</h4>
            <Link to="/login">Vs Manychat</Link>
            <Link to="/login">Vs LinkDM</Link>
          </div>
          <div className="landing-footer-col landing-footer-col--muted">
            <h4>
              Solutions <span className="landing-soon landing-soon--sm">Coming Soon</span>
            </h4>
            <span>Creators</span>
            <span>Coaches &amp; Experts</span>
            <span>Artists &amp; Labels</span>
            <span>Agencies</span>
          </div>
        </div>
        <div className="landing-watermark" aria-hidden="true">dmme</div>
      </footer>
    </div>
  );
}
