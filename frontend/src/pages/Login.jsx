import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { supabase } from "../lib/supabase";
import { useAuth } from "../context/AuthContext";
import { Logo } from "../components/Logo";
import Backdrop3D from "../components/Backdrop3D";

const CREATORS = [
  { handle: "@hardikpandyaa93", followers: "1M+ Followers", tag: "Lifestyle", img: "https://picsum.photos/seed/dmme-lifestyle/300/420" },
  { handle: "@ezsnippet", followers: "3.3M+ Followers", tag: "Tech", img: "https://picsum.photos/seed/dmme-tech/300/420" },
  { handle: "@fit.saswati", followers: "35K+ Followers", tag: "Fitness", img: "https://picsum.photos/seed/dmme-fitness/300/420" },
  { handle: "@elementec", followers: "1.2M+ Followers", tag: "Travel", img: "https://picsum.photos/seed/dmme-travel/300/420" },
  { handle: "@studio.mira", followers: "540K+ Followers", tag: "Design", img: "https://picsum.photos/seed/dmme-design/300/420" },
  { handle: "@thefoodloop", followers: "820K+ Followers", tag: "Food", img: "https://picsum.photos/seed/dmme-food/300/420" },
  { handle: "@aria.beats", followers: "2.1M+ Followers", tag: "Music", img: "https://picsum.photos/seed/dmme-music/300/420" },
  { handle: "@wildframe", followers: "410K+ Followers", tag: "Photography", img: "https://picsum.photos/seed/dmme-photo/300/420" },
  { handle: "@glowbyrhea", followers: "670K+ Followers", tag: "Beauty", img: "https://picsum.photos/seed/dmme-beauty/300/420" },
  { handle: "@coach.dev", followers: "95K+ Followers", tag: "Coaching", img: "https://picsum.photos/seed/dmme-coach/300/420" },
  { handle: "@urban.threads", followers: "1.4M+ Followers", tag: "Fashion", img: "https://picsum.photos/seed/dmme-fashion/300/420" },
  { handle: "@ledger.io", followers: "230K+ Followers", tag: "Finance", img: "https://picsum.photos/seed/dmme-finance/300/420" },
];

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

function MarqueeRow({ items, reverse }) {
  // Duplicate the items so the track can loop seamlessly.
  const loop = [...items, ...items];
  return (
    <div className={`marquee-row${reverse ? " reverse" : ""}`}>
      <div className="marquee-track">
        {loop.map((c, i) => (
          <CreatorCard key={`${c.handle}-${i}`} c={c} />
        ))}
      </div>
    </div>
  );
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true">
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
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

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mode, setMode] = useState("signin"); // signin | signup
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const { session } = useAuth();

  useEffect(() => {
    if (session) navigate("/app", { replace: true });
  }, [session, navigate]);

  async function submit(e) {
    e.preventDefault();
    setErr("");
    setMsg("");
    setBusy(true);
    try {
      if (mode === "signup") {
        const { error } = await supabase.auth.signUp({ email, password });
        if (error) throw error;
        setMsg("Check your email to confirm your account, then sign in.");
        setMode("signin");
      } else {
        const { error } = await supabase.auth.signInWithPassword({ email, password });
        if (error) throw error;
        navigate("/app", { replace: true });
      }
    } catch (e2) {
      setErr(e2.message);
    } finally {
      setBusy(false);
    }
  }

  async function google() {
    await supabase.auth.signInWithOAuth({
      provider: "google",
      options: { redirectTo: `${window.location.origin}/app` },
    });
  }

  async function forgotPassword() {
    setErr("");
    setMsg("");
    if (!email) {
      setErr("Enter your email above first, then click Forgot password.");
      return;
    }
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/login`,
    });
    if (error) setErr(error.message);
    else setMsg("Password reset link sent — check your email.");
  }

  return (
    <div className="login">
      <div className="login-form-col">
        <form className="login-form" onSubmit={submit}>
          <div className="brand-row">
            <Logo size={32} />
          </div>

          <h2>{mode === "signin" ? "Welcome back" : "Create your account"}</h2>
          <p className="login-sub">
            {mode === "signin" ? "Log in to your account" : "Start automating in minutes"}
          </p>

          <button type="button" className="google-btn" onClick={google}>
            <GoogleIcon />
            <span>Sign in with Google</span>
          </button>

          <div className="divider"><span>OR</span></div>

          <input
            type="email"
            className="login-input"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            type="password"
            className="login-input"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          {mode === "signin" && (
            <div className="forgot">
              <a onClick={forgotPassword}>Forgot password?</a>
            </div>
          )}

          {err && <div className="login-err">{err}</div>}
          {msg && <div className="login-ok">{msg}</div>}

          <button className="signin-btn" disabled={busy}>
            {busy ? "…" : mode === "signin" ? "Sign In" : "Sign Up"}
          </button>

          <p className="login-switch">
            {mode === "signin" ? "Don't have an account? " : "Already have an account? "}
            <a onClick={() => { setErr(""); setMsg(""); setMode(mode === "signin" ? "signup" : "signin"); }}>
              {mode === "signin" ? "Sign up for free" : "Sign in"}
            </a>
          </p>
        </form>
      </div>

      <div className="login-showcase">
        <Backdrop3D />
        <h2 className="showcase-head">
          Join <span>25k+</span> Creators
        </h2>
        <div className="marquee-stack">
          <MarqueeRow items={CREATORS.slice(0, 6)} />
          <MarqueeRow items={CREATORS.slice(6, 12)} reverse />
          <MarqueeRow items={[...CREATORS.slice(3, 9)]} />
        </div>
      </div>
    </div>
  );
}
