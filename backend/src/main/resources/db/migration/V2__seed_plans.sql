-- Seed the subscription plans. Prices are in paise (INR minor units).
-- Update razorpay_plan_id with the IDs created in your Razorpay dashboard.

insert into plan (code, name, price_minor, currency, dm_limit, contact_limit, razorpay_plan_id, features, sort_order)
values
  ('FREE',   'Free',   0,      'INR', 1000, 1000, null,
   '["1,000 DM replies / month","Up to 1,000 contacts","Unlimited automations","Product catalog up to 25 items","Basic analytics","Email support"]'::jsonb, 1),

  ('PRO',    'Pro',    39900,  'INR', null, null, 'plan_replace_me_pro',
   '["Everything in Free","Unlimited DM replies","Unlimited contacts","Re-trigger campaigns","Ask-for-follow gates","Lead-gen forms","Advanced analytics","Priority support"]'::jsonb, 2),

  ('AGENCY', 'Agency', 0,      'INR', null, null, null,
   '["Multiple Instagram accounts","Custom automation limits","Team roles & permissions","Dedicated onboarding","Custom integrations","Priority SLA"]'::jsonb, 3)
on conflict (code) do nothing;
