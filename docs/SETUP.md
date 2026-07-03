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

The DM automation runs on Meta's official API. You need:

1. A **Facebook Page** and an **Instagram Business/Creator account** linked to that Page.
2. A **Meta Developer app** (developers.facebook.com) with the products:
   - **Instagram** (Instagram API setup with Facebook Login)
   - **Webhooks**
3. Request these permissions (App Review required for production):
   - `instagram_basic`
   - `instagram_manage_messages`
   - `instagram_manage_comments`
   - `pages_show_list`, `pages_messaging`
4. **Webhook configuration** (App Dashboard → Webhooks → Instagram):
   - Callback URL: `https://<your-backend-host>/api/webhooks/instagram`
   - Verify token: the same value you set in `META_WEBHOOK_VERIFY_TOKEN`
   - Subscribe to fields: `comments`, `messages`
   - For local testing, tunnel with `ngrok http 8080` and use the https URL.
5. Get a **long-lived Page access token** for the connected Page. Paste the IG user ID,
   username, Page ID, and token into **Settings → Connect an account** in the app.

> Until App Review is approved, only users added as **testers/roles** on your Meta app can
> trigger live automations. Everything else works in the meantime.

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
`DB_URL, DB_USER, DB_PASSWORD, SUPABASE_JWT_SECRET, META_APP_ID, META_APP_SECRET,
META_WEBHOOK_VERIFY_TOKEN, RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET`.

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
2. **Settings → Connect an account** — paste your IG business account + Page token.
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
