# dmme — Setup Guide

End-to-end instructions to run the backend, frontend, and connect Supabase, Meta (Instagram), and Razorpay.

---

## 1. Prerequisites

- Java 21 (`java -version`)
- Maven 3.9+ (`mvn -version`) — or use the generated wrapper
- Node.js 18+ and npm
- A Supabase project
- A Meta Developer app with Instagram messaging permissions
- A Razorpay account (test mode is fine to start)

---

## 2. Supabase (database + auth)

1. Create a project at supabase.com.
2. **Database:** Project Settings → Database → Connection string. Convert the URI to JDBC:
   ```
   jdbc:postgresql://db.<project>.supabase.co:5432/postgres
   ```
   Note the DB password.
3. **JWT secret:** Project Settings → API → **JWT Secret**. The backend uses this to verify user tokens (HS256).
4. **Anon key + URL:** Project Settings → API. The frontend needs these.
5. **Auth providers:** Authentication → Providers. Enable Email, and optionally Google
   (add your OAuth client). Set the Site URL to `http://localhost:5173` for local dev.

Flyway creates all tables automatically on first backend start — you do **not** run SQL manually.

---

## 3. Meta / Instagram Messaging API

The DM automation uses **Instagram API with Instagram Login** (OAuth via
`https://www.instagram.com/oauth/authorize`, token exchange via
`https://api.instagram.com/oauth/access_token`). You need:

1. An **Instagram Business or Creator account** (linked to a Facebook Page is fine, but the
   app does **not** use the older Facebook-Login / Page-token flow).
2. A **Meta Developer app** at [developers.facebook.com](https://developers.facebook.com) with:
   - **Instagram** → add the product **Instagram API with Instagram Login** (not "Instagram API
     with Facebook Login")
   - **Webhooks**
3. **App credentials — read carefully (two different secrets):**

   | Env var | Where to find it | Used for |
   | --- | --- | --- |
   | `META_APP_ID` / `INSTAGRAM_APP_ID` | Instagram product → **API setup with Instagram login** → Instagram app ID | OAuth authorize + token exchange |
   | `META_APP_SECRET` / `INSTAGRAM_APP_SECRET` | Same screen → **Instagram app secret** | OAuth `client_secret` at `api.instagram.com/oauth/access_token` |
   | `META_WEBHOOK_APP_SECRET` | App **Settings → Basic** → **App Secret** (top-level Meta app secret) | Verifying `X-Hub-Signature-256` on webhook POSTs |

   **Do not mix these up.** The Instagram app secret and the top-level Meta App Secret are
   different strings. OAuth requires the Instagram pair; Meta signs webhooks with the top-level
   App Secret. If `META_WEBHOOK_APP_SECRET` is unset, the backend also tries the OAuth secret as
   a fallback, but production should set `META_WEBHOOK_APP_SECRET` explicitly.
4. **OAuth redirect URIs** — register both in the Instagram Login product settings and set
   `INSTAGRAM_REDIRECT_URI` on the backend to match the environment you are running:

   | Environment | Redirect URI |
   | --- | --- |
   | Local dev | `http://localhost:5173/instagram/callback` |
   | Production | `https://dmme.co.in/instagram/callback` |

   The frontend callback route posts the authorization code to the backend; the redirect URI
   must match exactly (including scheme and path).
5. **Permissions / scopes** requested by the app (App Review required for production):
   - `instagram_business_basic`
   - `instagram_business_manage_messages`
   - `instagram_business_manage_comments`
6. **Webhook configuration** (App Dashboard → Webhooks → Instagram):
   - Callback URL: `https://dmme-mntq.onrender.com/api/webhooks/instagram` (local: tunnel with
     `ngrok http 8080` and use `https://<ngrok-host>/api/webhooks/instagram`)
   - Verify token: must match `META_WEBHOOK_VERIFY_TOKEN` in your backend env
   - Subscribe to fields: `comments`, `messages`, `live_comments`
7. **Connect an account in the app:** sign in to dmme → **Settings → Connect Instagram**. The
   browser completes Instagram OAuth; the backend stores a long-lived token automatically. No
   manual token paste is required for normal use.

> **Obsolete (do not follow):** instructions that reference `instagram_basic`, `pages_show_list`,
> Facebook Login, or pasting a Page access token into Settings described an older integration
> path. A manual token form still exists under **Advanced** in Settings for dev/testing only.

> Until App Review is approved, only users added as **testers/roles** on your Meta app can
> authorize and trigger live automations. Everything else works in the meantime.

---

## 4. Razorpay (payments)

1. Dashboard → Settings → API Keys → generate **Key ID** and **Key Secret**.
2. (Optional) Create **Plans** for recurring subscriptions; put their plan IDs in
   `V2__seed_plans.sql` (`razorpay_plan_id`) or update the `plan` table.
3. Webhooks → add `https://<your-backend-host>/api/webhooks/razorpay`, set a **webhook secret**,
   and subscribe to `payment.captured`, `subscription.charged`, `payment.failed`.
4. The frontend loads Razorpay Checkout from `checkout.razorpay.com` (already in `index.html`).

---

## 5. Configure & run the backend

```bash
cd backend
cp src/main/resources/application-example.yml src/main/resources/application-local.yml
# edit application-local.yml with your Supabase / Meta / Razorpay values
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Alternatively, pass everything as environment variables (see `application.yml` for names):
`DB_URL, DB_USER, DB_PASSWORD, SUPABASE_JWT_SECRET, SUPABASE_URL, CORS_ORIGINS,
META_APP_ID, META_APP_SECRET, META_WEBHOOK_APP_SECRET, META_WEBHOOK_VERIFY_TOKEN,
INSTAGRAM_REDIRECT_URI, RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET`.

Backend runs on `http://localhost:8080`. Check `GET /api/health`.

---

## 6. Configure & run the frontend

```bash
cd frontend
cp .env.example .env
# set VITE_SUPABASE_URL, VITE_SUPABASE_ANON_KEY, VITE_API_BASE_URL
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`.

---

## 7. Try it end-to-end

1. Sign up / sign in on `/login` (Supabase Auth).
2. **Settings → Connect Instagram** — complete the Instagram OAuth flow for your Business or
   Creator account.
3. **Automations → New** — set a keyword (e.g. `link`), write a DM, add a Question step to
   collect an email, optionally enable Ask-for-follow. Save & activate.
4. Comment the keyword on the connected account's post (from a test user). The webhook fires,
   the engine matches the automation, sends the DM, and captures the lead.
5. See results in **Leads** and **Overview**.

---

## Architecture recap

```
Instagram ──webhook──▶ /api/webhooks/instagram ──▶ AutomationEngine
                                                      │  match trigger
                                                      │  run DM flow (text/buttons/questions)
                                                      │  ask-for-follow gate
                                                      ▼
                              InstagramClient ──Graph API──▶ sends DM / comment reply
                                                      │
                                                      ▼
                                     leads · message_log · usage_counter (Supabase Postgres)

React (Vite) ──Supabase JWT──▶ Spring Boot REST API ──▶ Supabase Postgres
Razorpay Checkout ◀── /api/billing/order → verify → activate plan
```
