const STORAGE_KEY = "dmme_session";

function userFromAccessToken(token) {
  try {
    const payload = JSON.parse(
      atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))
    );
    return {
      id: payload.sub,
      email: payload.email,
      user_metadata: payload.user_metadata || {},
    };
  } catch {
    return null;
  }
}

/** Shape mirrors a Supabase session so existing callers keep working. */
export function loadSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    if (!session?.access_token) return null;
    return session;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  if (!session?.access_token) {
    clearSession();
    return null;
  }
  const normalized = {
    access_token: session.access_token,
    refresh_token: session.refresh_token || null,
    expires_in: session.expires_in,
    expires_at: session.expires_at
      || (session.expires_in
        ? Math.floor(Date.now() / 1000) + Number(session.expires_in)
        : null),
    token_type: session.token_type || "bearer",
    user: session.user || userFromAccessToken(session.access_token),
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
  return normalized;
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY);
}

export function getAccessToken() {
  return loadSession()?.access_token || null;
}

export function getRefreshToken() {
  return loadSession()?.refresh_token || null;
}

/**
 * Persist tokens returned in a URL hash after email confirm / recovery / OAuth.
 * Returns { session, type } where type is signup | recovery | etc.
 */
export function sessionFromHash(hash) {
  const raw = (hash || "").replace(/^#/, "");
  if (!raw) return null;
  const params = new URLSearchParams(raw);
  const access_token = params.get("access_token");
  if (!access_token) return null;
  const expires_in = params.get("expires_in");
  const session = saveSession({
    access_token,
    refresh_token: params.get("refresh_token"),
    expires_in: expires_in ? Number(expires_in) : undefined,
    token_type: params.get("token_type") || "bearer",
    user: null,
  });
  return { session, type: params.get("type") || null };
}

export function authErrorFromHash(hash) {
  const raw = (hash || "").replace(/^#/, "");
  if (!raw) return null;
  const params = new URLSearchParams(raw);
  const code = params.get("error_code");
  const desc = (params.get("error_description") || "").replace(/\+/g, " ");
  if (code === "otp_expired") {
    return "This email link is invalid or has expired. Request a new confirmation email, or sign up again.";
  }
  if (desc) return desc;
  if (params.get("error")) return "Authentication link failed. Please try again.";
  return null;
}
