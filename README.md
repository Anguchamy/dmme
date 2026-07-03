# dmme

An Instagram DM automation platform for creators and businesses — a feature-for-feature clone of the LinkPlease model, built with your own neutral branding.

Turn every comment, story reply, and DM into an automated conversation: keyword triggers → DM flows → ask-for-follow gate → lead capture → analytics, with subscription billing.

> **Branding note:** This is a functional replica. It intentionally does **not** copy the "LinkPlease" name, logo, or their real customer testimonials (that would be IP infringement). Rebrand freely by editing `frontend/src/config/brand.js`.

## Tech stack

| Layer      | Technology                                   |
| ---------- | -------------------------------------------- |
| Frontend   | React 18 + Vite, React Router                |
| Backend    | Java 21 + Spring Boot 3 (Maven)              |
| Database   | Supabase (Postgres) via JDBC + Flyway        |
| Auth       | Supabase Auth (JWT verified by the backend)  |
| Payments   | Razorpay (orders + subscriptions + webhooks) |
| IG channel | Meta Instagram Messaging API + Webhooks      |

## Repository layout

```
dmme/
├── backend/          Spring Boot API + automation engine + webhooks
├── frontend/         React dashboard + marketing landing page
├── docs/             Setup guides (Meta app, Supabase, Razorpay)
└── docker-compose.yml (optional local Postgres if not using hosted Supabase)
```

## How the automation works (high level)

1. A follower comments a **keyword** (e.g. `link`) on your post/Reel, or replies to a story, or DMs you.
2. Meta sends a **webhook** to `POST /api/webhooks/instagram`.
3. The **trigger engine** finds the matching active automation for that account + media.
4. It runs the automation's **DM flow**: opening message → optional **ask-for-follow** gate → link/buttons → **questions** that collect the follower's email/phone/custom answers.
5. Collected answers become a **lead**; every message is written to `message_log`; usage is metered against the user's plan.

## Quick start

See [`docs/SETUP.md`](docs/SETUP.md) for the full walkthrough. In short:

```bash
# 1. Backend
cd backend
cp src/main/resources/application-example.yml src/main/resources/application-local.yml   # fill in secrets
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 2. Frontend
cd frontend
cp .env.example .env    # fill in Supabase + API URL
npm install
npm run dev
```

## Required accounts / credentials

- **Supabase project** — Postgres connection string + JWT secret + anon key.
- **Meta Developer app** — App ID/Secret, a Business/Creator IG account linked to a Facebook Page, webhook verify token, and approved permissions (`instagram_manage_messages`, `instagram_manage_comments`, `pages_messaging`).
- **Razorpay account** — Key ID + Key Secret + webhook secret; plan IDs for subscriptions.
