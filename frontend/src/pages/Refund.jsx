import { Link } from "react-router-dom";
import { brand } from "../config/brand";

const UPDATED = "July 8, 2026";

export default function Refund() {
  return (
    <div>
      <nav className="nav">
        <Link to="/" className="brand">{brand.name}</Link>
        <div className="row-flex">
          <Link to="/" className="btn btn-ghost btn-sm">Home</Link>
          <Link to="/login" className="btn btn-primary btn-sm">Start free</Link>
        </div>
      </nav>

      <main className="legal">
        <h1>Return &amp; Refund Policy</h1>
        <p className="legal-updated">Last updated: {UPDATED}</p>

        <p>
          This Return &amp; Refund Policy explains how refunds work for {brand.name} ("we",
          "us", or "our"), a digital software-as-a-service subscription available at{" "}
          {brand.domain}. {brand.name} does not sell physical goods — there are no product
          returns in the traditional sense.
        </p>

        <h2>1. Digital service</h2>
        <p>
          {brand.name} provides access to online software features immediately after purchase
          or plan activation. Because the Service is delivered digitally, standard "return"
          policies for physical products do not apply.
        </p>

        <h2>2. Subscription billing</h2>
        <p>
          Paid subscriptions are processed in Indian Rupees (INR) through Razorpay. Charges
          recur automatically at the billing interval you select (e.g., monthly or annual)
          until you cancel. Applicable taxes, including GST where required, may be added at
          checkout.
        </p>

        <h2>3. Refund eligibility</h2>
        <p>
          We want you to be satisfied with {brand.name}. If you believe you were charged in
          error or the Service did not work as reasonably expected, you may request a refund
          under the conditions below.
        </p>
        <ul>
          <li><strong>Request window:</strong> Contact us within <strong>7 days</strong> of
              the charge you are disputing.</li>
          <li><strong>How to request:</strong> Email{" "}
              <a href={`mailto:${brand.contactEmail}`}>{brand.contactEmail}</a> from the email
              address associated with your account. Include your account email, the charge date,
              and a brief description of the issue.</li>
          <li><strong>Duplicate or erroneous charges:</strong> We will refund verified duplicate
              or incorrect charges in full.</li>
          <li><strong>First-time paid subscription:</strong> If this is your first paid charge
              for a plan and you have not materially used paid features (for example, you have
              not consumed a significant portion of your plan's message or automation limits),
              we may offer a full or partial refund at our discretion.</li>
        </ul>

        <h2>4. Refund exclusions</h2>
        <p>We generally do not grant refunds when:</p>
        <ul>
          <li>More than 7 days have passed since the charge.</li>
          <li>You have substantially used paid plan limits or features during the billing
              period.</li>
          <li>The issue is caused by Instagram or Meta API outages, account restrictions on
              your Instagram account, or your violation of platform policies — not a failure
              of {brand.name} to provide the Service.</li>
          <li>You simply changed your mind after using the Service during the billing period,
              except where required by applicable law.</li>
          <li>A renewal charge applies and you did not cancel before the renewal date (see
              cancellation below).</li>
        </ul>

        <h2>5. How refunds are processed</h2>
        <p>
          Approved refunds are issued to the original payment method through Razorpay. Processing
          times depend on your bank or card issuer and may take 5–10 business days (or longer
          in some cases) to appear on your statement.
        </p>

        <h2>6. Cancelling your subscription</h2>
        <p>
          You can cancel a paid subscription at any time from <strong>Billing</strong> or{" "}
          <strong>Settings</strong> in your {brand.name} dashboard. Cancellation stops future
          renewals; it does not automatically refund the current billing period unless a refund
          is approved under this policy.
        </p>
        <ul>
          <li>After cancellation, you typically retain access to paid features until the end
              of the current paid period.</li>
          <li>When the period ends, your account may revert to a free plan (if available) or
              lose access to paid features until you resubscribe.</li>
          <li>Disconnecting Instagram or deleting automations does not cancel billing — you
              must cancel the subscription in Billing/Settings.</li>
        </ul>

        <h2>7. Free plan</h2>
        <p>
          The free plan does not involve charges and is not eligible for refunds. You may stop
          using the free plan at any time by deleting your account or disconnecting your
          Instagram account.
        </p>

        <h2>8. Changes to this policy</h2>
        <p>
          We may update this policy from time to time. The "Last updated" date above will
          reflect the latest version. Material changes will be communicated where appropriate.
        </p>

        <h2>9. Contact</h2>
        <p>
          For refund requests or billing questions, email{" "}
          <a href={`mailto:${brand.contactEmail}`}>{brand.contactEmail}</a> with the subject
          line "Refund request". You can also visit our{" "}
          <Link to="/contact">Contact</Link> page.
        </p>
      </main>

      <footer className="footer">
        <div className="brand" style={{ marginBottom: 8 }}>{brand.name}</div>
        <div className="row-flex" style={{ justifyContent: "center", gap: 16, marginBottom: 8 }}>
          <Link to="/">Home</Link>
          <Link to="/terms">Terms</Link>
          <Link to="/privacy">Privacy</Link>
          <Link to="/refund">Refund</Link>
          <Link to="/contact">Contact</Link>
        </div>
        <div>© {new Date().getFullYear()} {brand.name}. Not affiliated with Instagram or Meta.</div>
      </footer>
    </div>
  );
}
