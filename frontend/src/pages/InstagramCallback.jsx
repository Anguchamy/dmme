import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { Logo } from "../components/Logo";

function ChatBoltIcon() {
  return (
    <svg width="34" height="34" viewBox="0 0 40 40" fill="none" aria-hidden="true">
      <rect x="7" y="9" width="26" height="17" rx="8" fill="#fff" />
      <path d="M13 24 L13 31 L20.5 25 Z" fill="#fff" />
      <path d="M21 12 L16 18 L19.5 18 L18.5 23 L24 16 L20.5 16 Z" fill="#2b2bff" />
    </svg>
  );
}

function CrossIcon() {
  return (
    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M7 7l10 10M17 7L7 17" stroke="#fff" strokeWidth="2.6" strokeLinecap="round" />
    </svg>
  );
}

export default function InstagramCallback() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  // state: "loading" | "success" | "error"
  const [state, setState] = useState("loading");
  const [account, setAccount] = useState("your account");
  const [errorMsg, setErrorMsg] = useState("");
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return; // guard against React StrictMode double-invoke
    ran.current = true;

    const code = params.get("code");
    const error = params.get("error_description") || params.get("error");

    if (error) {
      setErrorMsg(decodeURIComponent(error));
      setState("error");
      return;
    }
    if (!code) {
      setErrorMsg("Missing authorization code from Instagram.");
      setState("error");
      return;
    }

    api
      .post("/api/instagram/oauth/callback", { code })
      .then((res) => {
        setAccount(res?.igUsername ? `@${res.igUsername}` : "your account");
        setState("success");
      })
      .catch((e) => {
        setErrorMsg(e.message || "We couldn't connect your Instagram account.");
        setState("error");
      });
  }, [params]);

  return (
    <div className="ig-result-page">
      <div className="ig-result-card">
        <div className="ig-result-brand">
          <Logo size={34} />
        </div>

        {state === "loading" && (
          <>
            <div className="ig-result-icon ig-result-icon--brand ig-result-icon--pulse">
              <ChatBoltIcon />
            </div>
            <h2>Connecting…</h2>
            <p>Hang tight while we securely link your Instagram account.</p>
          </>
        )}

        {state === "success" && (
          <>
            <div className="ig-result-icon ig-result-icon--brand">
              <ChatBoltIcon />
            </div>
            <h2>Congratulations! 🎉</h2>
            <p>
              <strong>{account}</strong> is successfully connected!
            </p>
            <button className="btn btn-primary ig-result-btn" onClick={() => navigate("/app", { replace: true })}>
              Next
            </button>
          </>
        )}

        {state === "error" && (
          <>
            <div className="ig-result-icon ig-result-icon--error">
              <CrossIcon />
            </div>
            <h2>Connection failed</h2>
            <p>{errorMsg}</p>
            <button
              className="btn btn-primary ig-result-btn"
              onClick={() => navigate("/app/settings", { replace: true })}
            >
              Try again
            </button>
          </>
        )}
      </div>
    </div>
  );
}
