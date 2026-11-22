import { clearSession, getAccessToken, getRefreshToken, loadSession, saveSession } from "./session";

const BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

let refreshPromise = null;

async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const res = await fetch(`${BASE}/api/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
        if (!res.ok) {
          clearSession();
          return null;
        }
        const data = await res.json();
        return saveSession(data);
      } catch {
        clearSession();
        return null;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

function authHeader() {
  const token = getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function parseError(res) {
  const text = await res.text();
  if (!text) return `Request failed: ${res.status}`;
  try {
    const json = JSON.parse(text);
    return json.message || json.error_description || json.msg || json.error || text;
  } catch {
    return text;
  }
}

async function request(method, path, body, { retry = true } = {}) {
  const headers = { "Content-Type": "application/json", ...authHeader() };
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 && retry && !path.startsWith("/api/auth/")) {
    const refreshed = await refreshAccessToken();
    if (refreshed?.access_token) {
      return request(method, path, body, { retry: false });
    }
  }

  if (!res.ok) {
    throw new Error(await parseError(res));
  }
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) return res.json();
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(
      `Non-JSON response from ${path} (got "${ct || "unknown"}"). Check VITE_API_BASE_URL — it should be the backend root with no /api suffix.`
    );
  }
}

/** Auth helpers that do not require an existing session. */
export const authApi = {
  signup: (email, password, emailRedirectTo) =>
    request("POST", "/api/auth/signup", { email, password, emailRedirectTo }, { retry: false }),
  login: (email, password) =>
    request("POST", "/api/auth/login", { email, password }, { retry: false }),
  forgotPassword: (email, redirectTo) =>
    request("POST", "/api/auth/forgot-password", { email, redirectTo }, { retry: false }),
  updatePassword: (password) =>
    request("POST", "/api/auth/update-password", { password }, { retry: false }),
  logout: () => request("POST", "/api/auth/logout", null, { retry: false }).catch(() => null),
  googleStart: (redirectTo, codeChallenge) =>
    request("POST", "/api/auth/oauth/google", { redirectTo, codeChallenge }, { retry: false }),
  oauthExchange: (code, codeVerifier) =>
    request("POST", "/api/auth/oauth/exchange", { code, codeVerifier }, { retry: false }),
};

export const api = {
  get: (p) => request("GET", p),
  post: (p, b) => request("POST", p, b),
  put: (p, b) => request("PUT", p, b),
  patch: (p, b) => request("PATCH", p, b),
  del: (p) => request("DELETE", p),
  baseUrl: BASE,
  getSession: loadSession,
  getAccessToken,
};
