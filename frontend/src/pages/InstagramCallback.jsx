import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";

export default function InstagramCallback() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState("Connecting your Instagram account…");
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return; // guard against React StrictMode double-invoke
    ran.current = true;

    const code = params.get("code");
    const error = params.get("error_description") || params.get("error");

    if (error) {
      navigate(`/app/settings?ig_error=${encodeURIComponent(error)}`, { replace: true });
      return;
    }
    if (!code) {
      navigate("/app/settings?ig_error=Missing+authorization+code", { replace: true });
      return;
    }

    api
      .post("/api/instagram/oauth/callback", { code })
      .then((res) => {
        const name = res?.igUsername ? `@${res.igUsername}` : "your account";
        navigate(`/app/settings?ig_connected=${encodeURIComponent(name)}`, { replace: true });
      })
      .catch((e) => {
        navigate(`/app/settings?ig_error=${encodeURIComponent(e.message || "Connection failed")}`, {
          replace: true,
        });
      });
  }, [params, navigate]);

  return <div className="loading">{status}</div>;
}
