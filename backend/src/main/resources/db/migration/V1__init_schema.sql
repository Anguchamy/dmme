-- dmme initial schema
-- Runs against the Supabase Postgres database via Flyway.
-- NOTE: Supabase Auth owns the `auth.users` table. Our `app_user.id`
-- mirrors the Supabase auth user id (UUID) so the two stay in sync.

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- Users & connected Instagram accounts
-- ---------------------------------------------------------------------------
create table app_user (
    id            uuid primary key,               -- == supabase auth uid
    email         text not null unique,
    full_name     text,
    avatar_url    text,
    plan_code     text not null default 'FREE',
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

create table instagram_account (
    id                bigserial primary key,
    user_id           uuid not null references app_user(id) on delete cascade,
    ig_user_id        text not null,              -- Instagram-scoped user id
    ig_username       text,
    facebook_page_id  text,
    page_access_token text,                        -- store encrypted at rest
    token_expires_at  timestamptz,
    connected_at      timestamptz not null default now(),
    is_active         boolean not null default true,
    unique (user_id, ig_user_id)
);

-- ---------------------------------------------------------------------------
-- Automations, triggers, and the DM flow (steps + questions)
-- ---------------------------------------------------------------------------
create table automation (
    id             bigserial primary key,
    user_id        uuid not null references app_user(id) on delete cascade,
    ig_account_id  bigint references instagram_account(id) on delete set null,
    name           text not null,
    -- COMMENT | STORY_REPLY | LIVE | DM
    type           text not null default 'COMMENT',
    -- ACTIVE | PAUSED | DRAFT
    status         text not null default 'DRAFT',
    -- null => applies to all media; otherwise a specific post/reel id
    ig_media_id    text,
    -- ANY comment triggers, or only when a keyword matches
    match_any      boolean not null default false,
    -- optional public comment reply posted back on the comment
    public_reply   text,
    ask_follow_enabled boolean not null default false,
    ask_follow_message text,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create index idx_automation_user on automation(user_id);
create index idx_automation_media on automation(ig_media_id) where ig_media_id is not null;

create table trigger_keyword (
    id             bigserial primary key,
    automation_id  bigint not null references automation(id) on delete cascade,
    keyword        text not null,
    -- EXACT | CONTAINS
    match_type     text not null default 'CONTAINS'
);
create index idx_trigger_automation on trigger_keyword(automation_id);

-- Ordered messages sent to the follower once triggered.
create table flow_step (
    id             bigserial primary key,
    automation_id  bigint not null references automation(id) on delete cascade,
    step_order     int not null,
    -- TEXT | BUTTONS | QUESTION
    step_type      text not null default 'TEXT',
    body           text,                            -- message text
    -- for BUTTONS: [{ "title": "Shop now", "url": "https://..." }]
    buttons        jsonb,
    -- for QUESTION: which lead field the answer is stored into
    -- EMAIL | PHONE | NAME | CUSTOM
    collect_field  text,
    -- for QUESTION with collect_field=CUSTOM
    custom_key     text,
    created_at     timestamptz not null default now(),
    unique (automation_id, step_order)
);
create index idx_step_automation on flow_step(automation_id);

-- ---------------------------------------------------------------------------
-- Leads (people who engaged) + collected answers
-- ---------------------------------------------------------------------------
create table lead (
    id             bigserial primary key,
    user_id        uuid not null references app_user(id) on delete cascade,
    automation_id  bigint references automation(id) on delete set null,
    ig_user_id     text not null,
    ig_username    text,
    email          text,
    phone          text,
    name           text,
    -- arbitrary custom answers { "key": "value" }
    custom_data    jsonb not null default '{}'::jsonb,
    did_follow     boolean,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);
create index idx_lead_user on lead(user_id);
create unique index uq_lead_scope on lead(user_id, automation_id, ig_user_id);

-- Tracks where a follower is inside a multi-step flow (for questions).
create table conversation_state (
    id             bigserial primary key,
    ig_account_id  bigint not null references instagram_account(id) on delete cascade,
    ig_user_id     text not null,
    automation_id  bigint not null references automation(id) on delete cascade,
    current_step   int not null default 0,
    awaiting_field text,                            -- field we asked the follower for
    updated_at     timestamptz not null default now(),
    unique (ig_account_id, ig_user_id, automation_id)
);

-- Every inbound/outbound message, for analytics + audit.
create table message_log (
    id             bigserial primary key,
    user_id        uuid not null references app_user(id) on delete cascade,
    automation_id  bigint references automation(id) on delete set null,
    ig_user_id     text,
    -- IN | OUT
    direction      text not null,
    -- COMMENT | DM | STORY_REPLY
    channel        text not null default 'DM',
    content        text,
    -- SENT | DELIVERED | FAILED | RECEIVED
    status         text not null default 'SENT',
    created_at     timestamptz not null default now()
);
create index idx_log_user_time on message_log(user_id, created_at);

-- ---------------------------------------------------------------------------
-- Billing: plans, subscriptions, payments, usage metering
-- ---------------------------------------------------------------------------
create table plan (
    code           text primary key,             -- FREE | PRO | AGENCY
    name           text not null,
    price_minor    int not null,                  -- price in paise
    currency       text not null default 'INR',
    dm_limit       int,                           -- null => unlimited
    contact_limit  int,                           -- null => unlimited
    razorpay_plan_id text,
    features       jsonb not null default '[]'::jsonb,
    sort_order     int not null default 0
);

create table subscription (
    id                       bigserial primary key,
    user_id                  uuid not null references app_user(id) on delete cascade,
    plan_code                text not null references plan(code),
    razorpay_subscription_id text unique,
    -- CREATED | ACTIVE | HALTED | CANCELLED | EXPIRED
    status                   text not null default 'CREATED',
    current_period_end       timestamptz,
    created_at               timestamptz not null default now()
);
create index idx_sub_user on subscription(user_id);

create table payment (
    id                  bigserial primary key,
    user_id             uuid not null references app_user(id) on delete cascade,
    razorpay_order_id   text,
    razorpay_payment_id text,
    amount_minor        int not null,
    currency            text not null default 'INR',
    -- CREATED | PAID | FAILED | REFUNDED
    status              text not null default 'CREATED',
    created_at          timestamptz not null default now()
);
create index idx_payment_user on payment(user_id);

-- Monthly usage counter for plan enforcement.
create table usage_counter (
    user_id     uuid not null references app_user(id) on delete cascade,
    period      text not null,                     -- 'YYYY-MM'
    dms_sent    int not null default 0,
    primary key (user_id, period)
);
