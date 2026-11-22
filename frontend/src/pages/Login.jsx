import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { useLoading, usePageLoading } from "../context/LoadingContext";
import { Logo } from "../components/Logo";
import Backdrop3D from "../components/Backdrop3D";
import { authErrorFromHash } from "../lib/session";
import { createCodeChallenge, createCodeVerifier, storeCodeVerifier } from "../lib/pkce";

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

function EyeIcon({ open }) {
  if (open) {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M3 3l18 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        <path d="M10.6 10.7a2 2 0 0 0 2.8 2.8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        <path d="M9.9 5.2A10.5 10.5 0 0 1 12 5c5 0 9.3 3.1 11 7.5a12.3 12.3 0 0 1-4.2 5.1" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        <path d="M6.7 6.7A12.3 12.3 0 0 0 1 12.5C2.7 16.9 7 20 12 20c1.4 0 2.7-.2 4-.7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
    );
  }
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M1 12.5C2.7 8.1 7 5 12 5s9.3 3.1 11 7.5c-1.7 4.4-6 7.5-11 7.5S2.7 16.9 1 12.5z" stroke="currentColor" strokeWidth="2" />
      <circle cx="12" cy="12.5" r="3.2" stroke="currentColor" strokeWidth="2" />
    </svg>
  );
}

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [mode, setMode] = useState("signin"); // signin | signup
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const { session, setSession, passwordRecovery, clearPasswordRecovery } = useAuth();
  const { runWithLoader } = useLoading();
  usePageLoading(false);

  useEffect(() => {
    // Recovery email leaves a temporary session — stay on login to set a new password.
    if (session && !passwordRecovery) navigate("/app", { replace: true });
  }, [session, passwordRecovery, navigate]);

  useEffect(() => {
    const message = authErrorFromHash(window.location.hash);
    if (message) {
      setErr(message);
      window.history.replaceState(null, "", "/login");
    }
  }, []);

  async function submit(e) {
    e.preventDefault();
    setErr("");
    setMsg("");
    setBusy(true);
    const label = passwordRecovery
      ? "Updating password…"
      : mode === "signup"
        ? "Creating account…"
        : "Signing in…";
    try {
      await runWithLoader(async () => {
        if (passwordRecovery) {
          if (password.length < 6) {
            throw new Error("Password must be at least 6 characters.");
          }
          if (password !== confirmPassword) {
            throw new Error("Passwords do not match.");
          }
          await authApi.updatePassword(password);
          clearPasswordRecovery();
          setMsg("Password updated. You're signed in.");
          navigate("/app", { replace: true });
          return;
        }
        if (mode === "signup") {
          await authApi.signup(email, password, `${window.location.origin}/login`);
          setMsg("Check your email to confirm your account, then sign in.");
          setMode("signin");
        } else {
          const data = await authApi.login(email, password);
          setSession(data);
          navigate("/app", { replace: true });
        }
      }, label);
    } catch (e2) {
      setErr(e2.message);
    } finally {
      setBusy(false);
    }
  }

  async function google() {
    setErr("");
    setBusy(true);
    try {
      await runWithLoader(async () => {
        const verifier = createCodeVerifier();
        storeCodeVerifier(verifier);
        const challenge = await createCodeChallenge(verifier);
        const { url } = await authApi.googleStart(`${window.location.origin}/login`, challenge);
        window.location.assign(url);
      }, "Connecting Google…");
    } catch (e2) {
      setBusy(false);
      setErr(e2.message || "Could not start Google sign-in.");
    }
  }

  async function forgotPassword() {
    setErr("");
    setMsg("");
    if (!email) {
      setErr("Enter your email above first, then click Forgot password.");
      return;
    }
    try {
      await runWithLoader(
        () => authApi.forgotPassword(email, `${window.location.origin}/login`),
        "Sending reset email…"
      );
      setMsg("Password reset link sent — check your email.");
    } catch (e2) {
      setErr(e2.message);
    }
  }

  return (
    <div className="login">
      <div className="login-form-col">
        <form className="login-form" onSubmit={submit}>
          <div className="brand-row">
            <Logo size={32} />
          </div>

          <h2>
            {passwordRecovery
              ? "Set a new password"
              : mode === "signin"
                ? "Welcome back"
                : "Create your account"}
          </h2>
          <p className="login-sub">
            {passwordRecovery
              ? "Choose a new password for your account"
              : mode === "signin"
                ? "Log in to your account"
                : "Start automating in minutes"}
          </p>

          {!passwordRecovery && (
            <>
              <button type="button" className="google-btn" onClick={google} disabled={busy}>
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
                autoComplete="email"
                required
              />
            </>
          )}

          <div className="password-field">
            <input
              type={showPassword ? "text" : "password"}
              className="login-input"
              placeholder={passwordRecovery ? "New password" : "Password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={passwordRecovery || mode === "signup" ? "new-password" : "current-password"}
              required
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? "Hide password" : "Show password"}
              tabIndex={-1}
            >
              <EyeIcon open={showPassword} />
            </button>
          </div>

          {passwordRecovery && (
            <div className="password-field">
              <input
                type={showPassword ? "text" : "password"}
                className="login-input"
                placeholder="Confirm new password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                autoComplete="new-password"
                required
              />
            </div>
          )}

          {!passwordRecovery && mode === "signin" && (
            <div className="forgot">
              <a onClick={forgotPassword}>Forgot password?</a>
            </div>
          )}

          {err && <div className="login-err">{err}</div>}
          {msg && <div className="login-ok">{msg}</div>}

          <button className="signin-btn" disabled={busy}>
            {busy
              ? "…"
              : passwordRecovery
                ? "Update password"
                : mode === "signin"
                  ? "Sign In"
                  : "Sign Up"}
          </button>

          {!passwordRecovery && (
            <p className="login-switch">
              {mode === "signin" ? "Don't have an account? " : "Already have an account? "}
              <a onClick={() => { setErr(""); setMsg(""); setMode(mode === "signin" ? "signup" : "signin"); }}>
                {mode === "signin" ? "Sign up for free" : "Sign in"}
              </a>
            </p>
          )}
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
