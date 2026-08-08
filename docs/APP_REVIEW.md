# dmme — Meta App Review Submission Package

Everything you need to submit **Instagram API with Instagram Login** permissions for production.
Copy the paste-ready blocks directly into the Meta App Dashboard. All UI paths and button labels
match the live app at `https://dmme.co.in`.

---

## 1. App Settings → Basic

Enter these values under **App Dashboard → App settings → Basic**.

| Field | Value | Required before review? |
| --- | --- | --- |
| **Display name** | `dmme` | Yes |
| **App domains** | `dmme.co.in` | Yes |
| **Privacy Policy URL** | `https://dmme.co.in/privacy` | Yes |
| **Terms of Service URL** | `https://dmme.co.in/terms` | Yes |
| **Data Deletion Instructions URL** | `https://dmme.co.in/privacy` (Section 6 — Data deletion) | Yes |
| **Category** | `Business and pages` (or `Social networks and dating`) | Yes |
| **Contact email** | `contact@dmme.co.in` | Yes |
| **App icon** | Upload your dmme logo (1024×1024 PNG) | Recommended |
| **Namespace** | Leave blank unless you use Facebook Login | No |

Also confirm under **Instagram → API setup with Instagram login**:

| Field | Value |
| --- | --- |
| **Valid OAuth redirect URIs** | `https://dmme.co.in/instagram/callback` |
| **Deauthorize callback URL** | `https://dmme-mntq.onrender.com/api/webhooks/instagram` (optional but recommended) |
| **Webhook callback URL** | `https://dmme-mntq.onrender.com/api/webhooks/instagram` |
| **Webhook verify token** | Must match `META_WEBHOOK_VERIFY_TOKEN` on the backend |
| **Webhook fields subscribed** | `comments`, `messages`, `live_comments` |

> **Mandatory before you can click Submit:** Privacy Policy URL, Terms of Service URL, Data
> Deletion Instructions URL, Contact email, and App domains must all be filled in and the URLs
> must load publicly without login. Business Verification must be completed for Advanced Access.

---

## 2. Permission justifications

Paste each block below into the **“Tell us how you'll use this permission”** field for that
permission. Do not combine them — one permission, one answer.

### `instagram_business_basic`

```
--- COPY BELOW INTO META (instagram_business_basic) ---

dmme is a SaaS dashboard where Instagram Business and Creator account holders connect their
account and build keyword-triggered automations. We use instagram_business_basic to identify
the connected account after OAuth (Instagram user ID and username shown in Settings and the
sidebar account switcher) and to list the account's recent posts and reels in the automation
builder's post picker ("Select post or reel" in Automations → New). The business owner
explicitly connects their account by clicking "Connect Instagram" in Settings and approving
the Instagram OAuth consent screen.

We do not scrape or bulk-download profiles. Media metadata (post ID, caption, thumbnail) is
used only so the owner can choose which post an automation watches. Profile and media data are
stored in our PostgreSQL database (Supabase) while the account remains connected and deleted
when the owner clicks "Disconnect" in Settings or deletes their dmme account. Access tokens
are stored encrypted at rest and refreshed per Meta's long-lived token rules.

This permission is required for the reviewer to see the connected @handle in Settings and to
select a specific post in the automation builder before the comment-trigger demo runs.
--- END COPY ---
```

### `instagram_business_manage_messages`

```
--- COPY BELOW INTO META (instagram_business_manage_messages) ---

dmme sends Instagram direct messages on behalf of the business owner when an automation they
created and activated fires. For example, when someone comments a keyword on the owner's
post, dmme sends a configured DM ("Response Flow" text, link buttons, or a follow-up question
to capture email/phone). The owner opts in by (1) connecting Instagram in Settings, (2) creating
an automation with Status set to Active, and (3) writing the exact DM content in the builder.
We also handle inbound DMs and story replies via the messages webhook to match "User DMs to you"
and "User replies to your stories" trigger types and to continue multi-step flows when a user
answers a Question step.

Messages are sent through Meta's official Send API (/me/messages) using the long-lived token
issued during OAuth. We log send/receive events in our database for the Home dashboard metrics
("Messages sent", "Messages received") and enforce per-plan DM quotas. Message content and
logs are retained while the account is connected; owners can disconnect at any time in Settings
or revoke dmme from Instagram's Apps and websites settings.

This permission is strictly necessary for the core product: without it, comment-triggered
automations cannot deliver links, codes, or lead-capture DMs. The screencast shows the resulting
DM arriving in the commenter's Instagram inbox after they comment on the owner's post.
--- END COPY ---
```

### `instagram_business_manage_comments`

```
--- COPY BELOW INTO META (instagram_business_manage_comments) ---

dmme listens for new comments on the connected account's posts, reels, and live videos via
Instagram webhooks (fields: comments, live_comments). When a comment matches an active automation
(keyword or "trigger on any comment"), the engine runs the owner's configured response. If the
owner set an optional public reply ("What do you want to reply to those comments?" in the
builder), dmme posts that reply on the comment thread via the Graph API before sending the DM.
Trigger types supported in the UI are "User comments on your post or reel" (COMMENT) and
"User comments on your LIVE" (LIVE).

The business owner explicitly configures which post to watch (or all posts), which keywords
trigger the flow, and whether to post a public comment reply. We store comment text only in
internal message logs for analytics and troubleshooting, not for advertising or resale.
Comment data is tied to the owner's account and deleted when they disconnect Instagram or
request account deletion (see our Privacy Policy).

This permission is required because the product's primary use case starts with a real Instagram
comment event. The screencast demonstrates a second account commenting on the connected account's
post, the webhook firing, and the automation executing — which is impossible without read access
to comments and the ability to reply on the owner's behalf when configured.
--- END COPY ---
```

---

## 3. Screencast shot-list

Record **one continuous video** (no cuts, no speed-up) at **1920×1080** or **1280×720**, English
UI, browser URL bar visible. Keep total length **under 5 minutes**. Use a screen recorder that
captures both browser tabs and the Instagram mobile app (phone camera pointed at phone screen is
acceptable for the DM inbox step).

| Time | Scene | What must be visible on screen |
| --- | --- | --- |
| 0:00–0:15 | Open `https://dmme.co.in` landing page. Click **Start free** (or go to `/login`). | Production URL in address bar. |
| 0:15–0:45 | Sign in on `/login` with test dmme credentials (email + **Sign In**, or Google). Redirect to `/app` (Home). | Successful login. |
| 0:45–1:30 | Sidebar → **Settings**. In the Instagram card, click **Connect Instagram**. Browser redirects to `https://www.instagram.com/oauth/authorize`. | **Pause on the Instagram permission dialog** — all three scopes must be readable: `instagram_business_basic`, `instagram_business_manage_messages`, `instagram_business_manage_comments`. Click Allow/Continue. |
| 1:30–1:50 | Land on `/instagram/callback` → **Connecting…** → **Congratulations! 🎉** → `@<YOUR_BUSINESS_IG_HANDLE> is successfully connected!** Click **Next**. | Connected username shown. |
| 1:50–2:05 | Back in **Settings**, confirm the account row shows `@<YOUR_BUSINESS_IG_HANDLE>` and status **Active**. Optionally click **Enable live triggers** and show banner **Live triggers enabled — comments & DMs will now fire your automations.** | Account connected in dmme. |
| 2:05–3:00 | Sidebar → **Automations** → **+ Create**. In the builder: select trigger **User comments on your post or reel**. Choose the connected Instagram account. Click **Select post or reel** and pick a real post (post picker loads thumbnails — uses `instagram_business_basic`). Set keyword e.g. `link`. In Response Flow, enter DM text e.g. `Here's the link you asked for! https://example.com`. Turn **Status** toggle ON (Active). Click **Save Changes**. | Full automation setup visible; Active status. |
| 3:00–3:15 | Note the post permalink from the picker (or open the same post on Instagram web logged in as the business account). | Reviewer can identify which post will be commented on. |
| 3:15–3:45 | On a **second device/account** logged in as `<YOUR_TEST_IG_HANDLE>`, open that post and leave a comment containing the keyword (e.g. `link`). | Real comment posted — not simulated. |
| 3:45–4:15 | Switch to Instagram **DM inbox** on the second account. Show the automated DM from `<YOUR_BUSINESS_IG_HANDLE>` with the configured message text. | **The DM must be clearly visible** — this proves `instagram_business_manage_messages`. |
| 4:15–4:35 | Return to dmme → **Home**. Under **Metrics (Last 30 days)**, show **Messages sent** incremented (was 0 or lower before). | Dashboard reflects the fired automation. |
| 4:35–4:50 | (Optional) If the automation included a Question step, open sidebar → **Contacts** and show the captured row. | Lead capture, if demonstrated. |
| 4:50–5:00 | End on **Settings** with the connected Instagram account still visible. | Clean ending. |

**Recording tips**

- Do **not** edit or splice the video — Meta reviewers reject cuts.
- Show the **permission consent screen** for at least 3 seconds.
- Show the **DM in the recipient's inbox**, not only the dmme dashboard.
- If the DM is delayed, wait on camera — webhooks can take 5–30 seconds.
- Use Instagram **professional** (Business or Creator) accounts only.
- Before recording, confirm webhooks are verified and the test business account is subscribed
  (OAuth connect auto-subscribes; **Enable live triggers** re-runs subscription if needed).

---

## 4. Reviewer test instructions

Paste the block below into **App Review → Instructions for reviewer** (and provide test
credentials in the test-user section). Replace every `<PLACEHOLDER>`.

```
--- COPY BELOW INTO META (Instructions for reviewer) ---

App URL: https://dmme.co.in
Backend (webhooks/API): https://dmme-mntq.onrender.com

TEST CREDENTIALS (dmme account)
Email: <YOUR_DMME_TEST_EMAIL>
Password: <YOUR_DMME_TEST_PASSWORD>

INSTAGRAM ACCOUNTS NEEDED
- Business/Creator account (already connected to this dmme test user): @<YOUR_BUSINESS_IG_HANDLE>
- Second personal account to trigger the automation: @<YOUR_TEST_IG_HANDLE>
  (Add @<YOUR_TEST_IG_HANDLE> as an Instagram Tester on Meta App ID <YOUR_META_APP_ID> if required.)

WHAT dmme DOES
dmme lets Instagram Business/Creator account holders automate DMs when someone comments on a
post, replies to a story, sends a DM, or comments on a live video — based on keywords the owner
configures.

STEP-BY-STEP TEST

1. Open https://dmme.co.in/login and sign in with the test credentials above.

2. You should land on Home (/app). The left sidebar shows: Home, Automations, Contacts,
   Billing, Settings.

3. Go to Settings (/app/settings).
   - If Instagram is not connected: click "Connect Instagram", approve the Instagram OAuth
     screen (you should see permissions for basic, manage messages, and manage comments),
     wait for "Congratulations! @<handle> is successfully connected!", click Next.
   - Confirm the account appears in the table with status Active.
   - Click "Enable live triggers" if automations do not fire (success banner appears).

4. Go to Automations (/app/automations) → click "+ Create".

5. In the automation builder (/app/automations/new):
   - Trigger: select "User comments on your post or reel".
   - Instagram account: select @<YOUR_BUSINESS_IG_HANDLE>.
   - Click "Select post or reel" and choose any recent post (or leave empty for all posts).
   - Keyword: enter "review" (or check "Trigger on any comment").
   - Response Flow: enter message text "Thanks for commenting — here is your link: https://example.com"
   - Turn Status toggle ON (Active).
   - Click "Save Changes".

6. From a second Instagram account (@<YOUR_TEST_IG_HANDLE>), comment "review" on the selected
   post (or any post if you chose all posts).

7. Within ~30 seconds, @<YOUR_TEST_IG_HANDLE> should receive a DM from @<YOUR_BUSINESS_IG_HANDLE>
   with the message configured in step 5. Check the second account's Instagram inbox.

8. Return to dmme Home (/app). Under Metrics → Last 30 days, "Messages sent" should increase.

9. (Optional) If you added a Question step, open Contacts (/app/leads) to see captured data.

EXPECTED RESULT
Comment on the business post → automated DM delivered to the commenter → dmme Home shows
increased "Messages sent". This exercises instagram_business_basic (post picker + account
identity), instagram_business_manage_comments (comment webhook), and
instagram_business_manage_messages (outbound DM).

NOTES
- dmme is not affiliated with Meta/Instagram; we use the official Instagram API with Instagram Login.
- Privacy Policy: https://dmme.co.in/privacy (includes data deletion instructions in Section 6).
- Terms: https://dmme.co.in/terms
- Support: contact@dmme.co.in
--- END COPY ---
```

---

## 5. Pre-submission checklist

Complete every item before clicking **Submit for review**.

### Meta Developer App

- [ ] **Business Verification** completed (Settings → Business verification).
- [ ] App mode: submit from **Development**; after approval, switch to **Live**.
- [ ] **Instagram API with Instagram Login** product added (not the legacy Facebook Login flow).
- [ ] **Instagram app ID** and **Instagram app secret** from API setup copied to backend env
      (`META_APP_ID`, `META_APP_SECRET`) — not the top-level Meta App ID/Secret.
- [ ] OAuth redirect URI registered: `https://dmme.co.in/instagram/callback`.
- [ ] Webhook URL verified: `https://dmme-mntq.onrender.com/api/webhooks/instagram`.
- [ ] Webhook fields subscribed: `comments`, `messages`, `live_comments`.
- [ ] App Review submission requests exactly these three permissions (no extras):
      `instagram_business_basic`, `instagram_business_manage_messages`,
      `instagram_business_manage_comments`.

### Roles & test accounts

- [ ] Your dmme test user email added as **Admin/Developer/Tester** on the Meta app.
- [ ] `@<YOUR_BUSINESS_IG_HANDLE>` (Business or Creator) added as **Instagram Tester**.
- [ ] `@<YOUR_TEST_IG_HANDLE>` added as **Instagram Tester** (account that will comment).
- [ ] Both Instagram accounts can complete OAuth and receive DMs in Development mode.

### Legal & public URLs

- [ ] `https://dmme.co.in/privacy` loads — includes Section 6 **Data deletion**.
- [ ] `https://dmme.co.in/terms` loads.
- [ ] Privacy Policy URL, Terms URL, and Data Deletion URL entered in App Settings → Basic.
- [ ] App domain `dmme.co.in` entered and matches the live site.

### Product & infrastructure

- [ ] `https://dmme.co.in` and `https://dmme-mntq.onrender.com/api/health` reachable.
- [ ] Backend `CORS_ORIGINS` includes `https://dmme.co.in`.
- [ ] Supabase Site URL / redirect allow-list includes `https://dmme.co.in`.
- [ ] End-to-end test passed: comment → DM received → Home **Messages sent** increments.
- [ ] Screencast recorded (single take, permission dialog + DM inbox shown).
- [ ] Permission justification text pasted for all three permissions.
- [ ] Reviewer instructions + test credentials pasted.

---

## 6. Common rejection reasons (and how to avoid them)

| Rejection reason | Why Meta flags it | How to avoid with dmme |
| --- | --- | --- |
| Screencast doesn't show permissions in use | Reviewer can't tie scopes to behavior | Pause on Instagram OAuth dialog showing all three scopes; show real DM in recipient inbox. |
| Screencast has cuts or voiceover only | Can't verify live product | One continuous recording; no edits or time-lapse. |
| App URL not reachable | Reviewer can't log in | Test `https://dmme.co.in/login` in an incognito window before submitting. |
| Missing / invalid Privacy Policy | Required legal disclosures | Ensure `/privacy` is public; Section 6 covers data deletion (used as Data Deletion URL). |
| Permission requested but not demonstrated | Scope looks unnecessary | Demo comment trigger → DM (covers all three); don't request scopes beyond the three listed. |
| Wrong OAuth product or redirect mismatch | OAuth fails for reviewer | Use **Instagram Login** redirect `https://dmme.co.in/instagram/callback`; must match backend `INSTAGRAM_REDIRECT_URI` exactly. |
| Webhook not subscribed | Comment never triggers automation | After connect, confirm **Enable live triggers** or reconnect Instagram; verify webhook fields in Meta dashboard. |
| Tester accounts not added | OAuth or DM blocked in Dev mode | Add business + test commenter as Instagram Testers on app `<YOUR_META_APP_ID>`. |
| Automated messaging appears spammy | Violates platform policies | Demo keyword-triggered, opt-in style flows (user comments first); show owner-written message text, not bulk unsolicited DMs. |
| Business Verification incomplete | Required for Advanced Access | Finish verification before submitting messaging permissions. |
| Using Facebook Page token flow | Wrong integration for Instagram Login | Connect via Settings → **Connect Instagram** (OAuth), not Advanced manual token paste. |

---

## Codebase reference (for your own verification)

| Permission | Used in product for |
| --- | --- |
| `instagram_business_basic` | OAuth profile (`/me`), post picker media list (`/{ig-user-id}/media`), follower check for ask-for-follow |
| `instagram_business_manage_messages` | Send DMs (`/me/messages`), receive `messages` webhook (DM + story reply triggers) |
| `instagram_business_manage_comments` | Receive `comments` / `live_comments` webhooks, optional public comment reply (`/{comment-id}/replies`) |

Webhook handler: `backend/src/main/java/com/dmme/web/InstagramWebhookController.java`  
Automation engine: `backend/src/main/java/com/dmme/service/AutomationEngine.java`  
Connect flow UI: `frontend/src/pages/Settings.jsx` → `frontend/src/pages/InstagramCallback.jsx`
