import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { authApi } from "../lib/api";
import { clearSession, loadSession, saveSession, sessionFromHash } from "../lib/session";
import { takeCodeVerifier } from "../lib/pkce";

const AuthContext = createContext(null);
const RECOVERY_FLAG = "dmme_password_recovery";

function notifyAuth(session) {
  window.dispatchEvent(new CustomEvent("dmme-auth", { detail: session }));
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [passwordRecovery, setPasswordRecovery] = useState(
    () => sessionStorage.getItem(RECOVERY_FLAG) === "1"
  );

  const applySession = useCallback((next) => {
    const saved = next ? saveSession(next) : (clearSession(), null);
    setSession(saved);
    notifyAuth(saved);
    return saved;
  }, []);

  const clearPasswordRecovery = useCallback(() => {
    sessionStorage.removeItem(RECOVERY_FLAG);
    setPasswordRecovery(false);
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function boot() {
      // Email-confirm / recovery / implicit OAuth tokens arrive in the hash.
      const fromHash = sessionFromHash(window.location.hash);
      if (fromHash?.session) {
        window.history.replaceState(null, "", window.location.pathname + window.location.search);
        if (fromHash.type === "recovery") {
          sessionStorage.setItem(RECOVERY_FLAG, "1");
          if (!cancelled) setPasswordRecovery(true);
        }
        if (!cancelled) {
          setSession(fromHash.session);
          notifyAuth(fromHash.session);
        }
      }

      // Google PKCE returns ?code= on redirect_to.
      const params = new URLSearchParams(window.location.search);
      const code = params.get("code");
      if (code) {
        const verifier = takeCodeVerifier();
        params.delete("code");
        const qs = params.toString();
        window.history.replaceState(null, "", window.location.pathname + (qs ? `?${qs}` : ""));
        if (verifier) {
          try {
            const data = await authApi.oauthExchange(code, verifier);
            if (!cancelled) applySession(data);
          } catch {
            // Leave session empty; Login can show a generic error if needed.
          }
        }
      }

      if (!cancelled && !fromHash?.session) {
        setSession(loadSession());
      }
      if (!cancelled) setLoading(false);
    }

    boot();

    const onAuth = (e) => setSession(e.detail || null);
    window.addEventListener("dmme-auth", onAuth);
    return () => {
      cancelled = true;
      window.removeEventListener("dmme-auth", onAuth);
    };
  }, [applySession]);

  const value = {
    session,
    user: session?.user ?? null,
    loading,
    passwordRecovery,
    clearPasswordRecovery,
    setSession: applySession,
    signOut: async () => {
      clearPasswordRecovery();
      await authApi.logout();
      applySession(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
