import { Link } from "react-router-dom";
import { brand } from "../config/brand";

const UPDATED = "July 8, 2026";

export default function Terms() {
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
        <h1>Terms &amp; Conditions</h1>
        <p className="legal-updated">Last updated: {UPDATED}</p>

        <p>
          These Terms &amp; Conditions ("Terms") govern your access to and use of the{" "}
          {brand.name} application and website at {brand.domain} (the "Service"),
          operated by {brand.name} ("we", "us", or "our"). By creating an account, subscribing
          to a paid plan, or otherwise using the Service, you agree to these Terms. If you do
          not agree, do not use the Service.
        </p>
        <p>
          {brand.name} is an independent product and is <strong>not affiliated with,
          endorsed by, or sponsored by Instagram or Meta Platforms, Inc.</strong> We use
          Meta's official Instagram APIs strictly within their approved terms.
        </p>

        <h2>1. Description of the Service</h2>
        <p>
          {brand.name} is a software-as-a-service platform that helps Instagram professional
          (Business or Creator) account holders automate responses to comments, story replies,
          and direct messages using keyword-triggered flows. Features may include lead capture,
          analytics, and subscription-based usage limits depending on your plan.
        </p>

        <h2>2. Eligibility and account registration</h2>
        <ul>
          <li>You must be at least 18 years old (or the age of majority in your jurisdiction)
              to use the Service.</li>
          <li>You must have a valid Instagram professional account and the authority to
              connect it to {brand.name}.</li>
          <li>You agree to provide accurate account information and keep your login credentials
              secure. You are responsible for all activity under your account.</li>
          <li>You may sign in using Google or other methods we support via our authentication
              provider.</li>
        </ul>

        <h2>3. Acceptable use</h2>
        <p>You agree to use the Service only for lawful purposes and in compliance with these
          Terms, applicable laws, and all Instagram and Meta platform policies, including
          Meta's Terms of Use, Community Guidelines, and Platform Terms.</p>
        <p>You must not:</p>
        <ul>
          <li>Send spam, unsolicited bulk messages, or deceptive or misleading content.</li>
          <li>Harass, abuse, or impersonate others, or collect data without consent where
              required by law.</li>
          <li>Use the Service to distribute malware, phishing links, or illegal content.</li>
          <li>Attempt to bypass usage limits, scrape the Service, or interfere with its
              operation or security.</li>
          <li>Reverse engineer, resell, or sublicense the Service except as expressly
              permitted.</li>
          <li>Use automations in ways that violate Instagram or Meta rules or that could
              cause your Instagram account to be restricted or suspended.</li>
        </ul>
        <p>
          We may suspend or terminate access if we reasonably believe you have violated these
          rules or platform policies.
        </p>

        <h2>4. Instagram and Meta API usage</h2>
        <p>
          The Service connects to Instagram through Meta's official APIs. By connecting your
          Instagram account, you authorize {brand.name} to access and use Instagram data on
          your behalf as described in our{" "}
          <Link to="/privacy">Privacy Policy</Link> and only for the automations you configure.
        </p>
        <ul>
          <li>You remain solely responsible for your Instagram account, content, and
              compliance with Meta's policies.</li>
          <li>Meta may change, limit, or revoke API access at any time; we are not liable for
              interruptions caused by Meta or Instagram.</li>
          <li>You can disconnect {brand.name} from Instagram at any time in Settings or via
              Instagram's "Apps and websites" settings.</li>
        </ul>

        <h2>5. Subscriptions, billing, and taxes</h2>
        <p>
          Paid plans are billed in Indian Rupees (INR) through Razorpay. By subscribing, you
          authorize us and Razorpay to charge your selected payment method for recurring
          subscription fees at the interval shown at checkout (e.g., monthly or annual).
        </p>
        <ul>
          <li><strong>Auto-renewal:</strong> Subscriptions renew automatically until you cancel.
              You can cancel from Billing or Settings before the next billing date to avoid
              future charges.</li>
          <li><strong>Price changes:</strong> We may change plan prices with reasonable notice.
              Continued use after a price change constitutes acceptance of the new price for
              subsequent billing periods.</li>
          <li><strong>Taxes:</strong> Fees are exclusive of applicable taxes (including GST
              where required), which may be added at checkout as required by law.</li>
          <li><strong>Failed payments:</strong> If a payment fails, we may suspend paid features
              until payment is resolved.</li>
        </ul>
        <p>
          Refund terms are described in our{" "}
          <Link to="/refund">Return &amp; Refund Policy</Link>.
        </p>

        <h2>6. Free plan</h2>
        <p>
          We may offer a free tier with limited features or usage (such as caps on automations,
          messages, or connected accounts). Free plan limits are subject to change. We do not
          guarantee that any free tier will remain available indefinitely.
        </p>

        <h2>7. Intellectual property</h2>
        <p>
          The Service, including its software, design, branding, and documentation, is owned by
          {brand.name} or its licensors and is protected by intellectual property laws. You
          receive a limited, non-exclusive, non-transferable license to use the Service for
          your internal business or personal use while your account is in good standing.
        </p>
        <p>
          You retain ownership of your content and Instagram data. You grant us a license to
          use that content only as needed to operate the Service and your automations.
        </p>

        <h2>8. Disclaimers</h2>
        <p>
          THE SERVICE IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT WARRANTIES OF ANY KIND,
          WHETHER EXPRESS OR IMPLIED, INCLUDING IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS
          FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. WE DO NOT WARRANT THAT THE SERVICE
          WILL BE UNINTERRUPTED, ERROR-FREE, OR THAT AUTOMATIONS WILL ACHIEVE ANY SPECIFIC
          BUSINESS RESULT (SUCH AS FOLLOWER GROWTH OR SALES).
        </p>

        <h2>9. Limitation of liability</h2>
        <p>
          TO THE MAXIMUM EXTENT PERMITTED BY LAW, {brand.name.toUpperCase()} AND ITS
          AFFILIATES, OFFICERS, AND SUPPLIERS WILL NOT BE LIABLE FOR ANY INDIRECT,
          INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES, OR ANY LOSS OF PROFITS,
          REVENUE, DATA, OR GOODWILL, ARISING FROM YOUR USE OF THE SERVICE. OUR TOTAL
          LIABILITY FOR ANY CLAIM RELATING TO THE SERVICE IS LIMITED TO THE AMOUNT YOU PAID US
          FOR THE SERVICE IN THE TWELVE (12) MONTHS BEFORE THE EVENT GIVING RISE TO THE CLAIM,
          OR INR 5,000, WHICHEVER IS GREATER.
        </p>
        <p>
          Some jurisdictions do not allow certain limitations; in those cases, our liability
          is limited to the fullest extent permitted by law.
        </p>

        <h2>10. Indemnification</h2>
        <p>
          You agree to indemnify and hold harmless {brand.name} and its affiliates from claims,
          damages, losses, and expenses (including reasonable legal fees) arising from your use
          of the Service, your content, your Instagram account activity, or your violation of
          these Terms or third-party rights.
        </p>

        <h2>11. Termination and suspension</h2>
        <p>
          You may stop using the Service and delete your account at any time. We may suspend or
          terminate your access immediately if you breach these Terms, fail to pay, pose a
          security or legal risk, or if required by Meta, law, or payment partners.
        </p>
        <p>
          Upon termination, your right to use the Service ends. Sections that by nature should
          survive (including disclaimers, limitation of liability, and indemnification) will
          survive termination.
        </p>

        <h2>12. Changes to these Terms</h2>
        <p>
          We may update these Terms from time to time. We will update the "Last updated" date
          above and, where appropriate, notify you by email or through the Service. Continued
          use after changes become effective constitutes acceptance of the revised Terms.
        </p>

        <h2>13. Governing law</h2>
        <p>
          These Terms are governed by the laws of India, without regard to conflict-of-law
          principles. Any disputes shall be subject to the exclusive jurisdiction of the courts
          located in India, unless otherwise required by applicable consumer protection law.
        </p>

        <h2>14. Contact</h2>
        <p>
          Questions about these Terms? Contact us at{" "}
          <a href={`mailto:${brand.contactEmail}`}>{brand.contactEmail}</a> or visit our{" "}
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
