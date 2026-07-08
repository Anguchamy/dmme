import { Link } from "react-router-dom";
import { brand } from "../config/brand";

const UPDATED = "July 7, 2026";

export default function Privacy() {
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
        <h1>Privacy Policy</h1>
        <p className="legal-updated">Last updated: {UPDATED}</p>

        <p>
          {brand.name} ("we", "us", or "our") operates the {brand.name} application and
          website at {brand.domain} (the "Service"). This Privacy Policy explains what
          information we collect, how we use it, and the choices you have. By using the
          Service you agree to the practices described here.
        </p>
        <p>
          {brand.name} is an independent product and is <strong>not affiliated with,
          endorsed by, or sponsored by Instagram or Meta Platforms, Inc.</strong> We use
          Meta's official Instagram APIs strictly within their approved terms.
        </p>

        <h2>1. Information we collect</h2>
        <h3>Account information</h3>
        <p>
          When you sign up we collect your name and email address (via Google sign-in or
          the email you provide) to create and secure your account.
        </p>
        <h3>Instagram data</h3>
        <p>
          When you connect an Instagram professional (Business or Creator) account through
          Meta's official login, we access only the data needed to run your automations:
        </p>
        <ul>
          <li>Your Instagram account ID, username, and profile basics.</li>
          <li>Your media (posts and reels) so you can select which ones to automate.</li>
          <li>Comments, story replies, and direct messages sent to your account that
              match the automations you configure, so we can respond on your behalf.</li>
          <li>An access token issued by Meta, stored securely to make API calls you
              authorize.</li>
        </ul>
        <h3>Leads you collect</h3>
        <p>
          If your automations ask questions (for example, to capture an email or phone
          number), the responses your audience provides are stored so you can view them in
          your dashboard.
        </p>
        <h3>Usage and technical data</h3>
        <p>
          We collect basic usage information (such as automation activity and delivery
          counts) and standard technical data (such as log data) to operate and improve the
          Service.
        </p>

        <h2>2. How we use information</h2>
        <ul>
          <li>To provide the Service — running your automations and replying to comments,
              story replies, and DMs as you configure.</li>
          <li>To authenticate you and secure your account.</li>
          <li>To display analytics, leads, and usage in your dashboard.</li>
          <li>To process subscriptions and payments for paid plans.</li>
          <li>To communicate with you about your account and support requests.</li>
        </ul>
        <p>We do not sell your personal information or your audience's data.</p>

        <h2>3. Sharing and disclosure</h2>
        <p>We share information only with service providers that help us operate the
          Service, and only as needed:</p>
        <ul>
          <li><strong>Meta / Instagram</strong> — to send and receive messages and comments
              through their official APIs.</li>
          <li><strong>Supabase</strong> — authentication and database hosting.</li>
          <li><strong>Razorpay</strong> — payment processing for paid plans.</li>
          <li><strong>Hosting providers</strong> — to run our application and website.</li>
        </ul>
        <p>
          We may also disclose information if required by law or to protect the rights,
          safety, and security of our users and the Service.
        </p>

        <h2>4. Data retention</h2>
        <p>
          We keep your information for as long as your account is active. When you
          disconnect an Instagram account, we stop accessing its data and delete the stored
          access token. When you delete your {brand.name} account, we delete your associated
          personal data within a reasonable period, except where we must retain it to comply
          with legal obligations.
        </p>

        <h2>5. Your rights and choices</h2>
        <ul>
          <li>Disconnect any Instagram account at any time from Settings.</li>
          <li>Request access to, correction of, or deletion of your personal data.</li>
          <li>Revoke {brand.name}'s access from your Instagram account at any time via
              Instagram's "Apps and websites" settings.</li>
        </ul>

        <h2>6. Data deletion</h2>
        <p>
          To request deletion of your data, disconnect your account in Settings or email us
          at <a href={`mailto:${brand.contactEmail}`}>{brand.contactEmail}</a> with the
          subject "Data deletion". We will process verified requests promptly.
        </p>

        <h2>7. Security</h2>
        <p>
          We use industry-standard measures to protect your data, including encrypted
          transport (HTTPS) and secure storage of access tokens. No method of transmission
          or storage is completely secure, but we work to protect your information.
        </p>

        <h2>8. Children's privacy</h2>
        <p>
          The Service is not directed to individuals under 13 (or the minimum age required
          in your jurisdiction). We do not knowingly collect data from children.
        </p>

        <h2>9. Changes to this policy</h2>
        <p>
          We may update this Privacy Policy from time to time. We will update the "Last
          updated" date above and, where appropriate, notify you.
        </p>

        <h2>10. Contact us</h2>
        <p>
          If you have questions about this Privacy Policy or your data, contact us at{" "}
          <a href={`mailto:${brand.contactEmail}`}>{brand.contactEmail}</a>.
        </p>
      </main>

      <footer className="footer">
        <div className="brand" style={{ marginBottom: 8 }}>{brand.name}</div>
        <div className="row-flex" style={{ justifyContent: "center", gap: 16, marginBottom: 8 }}>
          <Link to="/">Home</Link>
          <Link to="/privacy">Privacy</Link>
        </div>
        <div>© {new Date().getFullYear()} {brand.name}. Not affiliated with Instagram or Meta.</div>
      </footer>
    </div>
  );
}
