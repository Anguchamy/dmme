import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { supabase } from "../lib/supabase";
import { brand } from "../config/brand";
import { useAuth } from "../context/AuthContext";
import { useEffect } from "react";

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

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <h1>{brand.name}</h1>
        <p style={{ color: "var(--muted)", marginTop: 0 }}>
          {mode === "signin" ? "Welcome back" : "Create your account"}
        </p>
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {err && <div className="err">{err}</div>}
        {msg && <div style={{ color: "#1a8f4b", fontSize: 14 }}>{msg}</div>}
        <button className="btn btn-primary" style={{ width: "100%", marginTop: 8 }} disabled={busy}>
          {busy ? "…" : mode === "signin" ? "Sign in" : "Sign up"}
        </button>
        <button
          type="button"
          className="btn btn-ghost"
          style={{ width: "100%", marginTop: 10 }}
          onClick={google}
        >
          Continue with Google
        </button>
        <p style={{ marginTop: 16, fontSize: 14 }}>
          {mode === "signin" ? "New here? " : "Have an account? "}
          <a
            style={{ color: "var(--accent)", cursor: "pointer" }}
            onClick={() => setMode(mode === "signin" ? "signup" : "signin")}
          >
            {mode === "signin" ? "Create an account" : "Sign in"}
          </a>
        </p>
      </form>
    </div>
  );
}
