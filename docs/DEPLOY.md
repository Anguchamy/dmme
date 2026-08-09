# dmme — Deployment

Backend on **Render** (Docker), frontend on **Cloudflare Pages** (static), database + auth on **Supabase**.

## Backend → Render (Docker)

Files: [`backend/Dockerfile`](../backend/Dockerfile), [`backend/.dockerignore`](../backend/.dockerignore), [`render.yaml`](../render.yaml).

Two ways to deploy:

1. Blueprint (recommended): Render dashboard -> New -> Blueprint -> select this repo. It reads `render.yaml`, builds `backend/Dockerfile`, and creates the service `dmme-backend`.
2. Manual: New -> Web Service -> connect repo -> Runtime "Docker" -> Root Directory `backend` -> Dockerfile path `Dockerfile`.

Set these env vars in Render (all are `sync:false` in the blueprint, so add them in the dashboard):

| Var | Example / notes |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://db.<project>.supabase.co:5432/postgres` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | Supabase DB password |
| `SUPABASE_JWT_SECRET` | Supabase -> Settings -> API -> JWT Secret |
| `SUPABASE_URL` | `https://<project>.supabase.co` |
| `META_APP_ID`, `META_APP_SECRET` | **Instagram app ID / secret** from Instagram product → API setup with Instagram login (OAuth only — not the top-level Meta App ID/Secret) |
| `META_WEBHOOK_APP_SECRET` | **Top-level Meta App Secret** from App settings → Basic — used to verify `X-Hub-Signature-256` on webhook POSTs (falls back to `META_APP_SECRET` if unset) |
| `META_WEBHOOK_VERIFY_TOKEN` | random string; must match the verify token in Meta Webhooks |
| `INSTAGRAM_REDIRECT_URI` | `https://dmme.co.in/instagram/callback` |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | from Razorpay |
| `CORS_ORIGINS` | `https://dmme.co.in` (comma-separate multiple origins) |

- Render provides `PORT`; the app already binds to `${PORT:8080}`.
- Health check path: `/api/health`.
- Flyway runs the migrations automatically on first boot.
- Webhook URLs to register once deployed:
  - Meta: `https://dmme-mntq.onrender.com/api/webhooks/instagram`
  - Razorpay: `https://dmme-mntq.onrender.com/api/webhooks/razorpay`

## Frontend → Cloudflare Pages (static)

File: [`frontend/public/_redirects`](../frontend/public/_redirects) (SPA fallback so `/login`, `/app/*` don't 404 on refresh; Vite copies it into `dist`).

Cloudflare Pages -> Create project -> connect repo, then set:

- Root directory: `frontend`
- Build command: `npm run build`
- Build output directory: `dist`

Environment variables (Pages -> Settings -> Environment variables):

| Var | Value |
| --- | --- |
| `VITE_SUPABASE_URL` | `https://<project>.supabase.co` |
| `VITE_SUPABASE_ANON_KEY` | Supabase anon key |
| `VITE_API_BASE_URL` | `https://dmme-mntq.onrender.com` (backend root only — do **not** append `/api`) |

After deploy, confirm the backend's `CORS_ORIGINS` includes `https://dmme.co.in`, and add that
URL to Supabase → Authentication → URL Configuration (Site URL + redirect allow-list) so
login redirects work.
