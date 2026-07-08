import { Link } from "react-router-dom";
import { brand } from "../config/brand";

const UPDATED = "July 8, 2026";

export default function Contact() {
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
        <h1>Contact Us</h1>
        <p className="legal-updated">Last updated: {UPDATED}</p>

        <p>
          Have a question about {brand.name}, your account, billing, or automations? We're here
          to help. The fastest way to reach us is by email — we do not offer phone support at
          this time.
        </p>

        <div className="legal-contact-card">
          <p className="legal-contact-label">Support email</p>
          <a
            className="legal-contact-email"
            href={`mailto:${brand.contactEmail}`}
          >
            {brand.contactEmail}
          </a>
        </div>

        <h2>Response time</h2>
        <p>
          We aim to reply to most messages within <strong>1–2 business days</strong> (Monday
          through Friday, excluding public holidays in India). Complex billing or technical
          issues may take a little longer; we'll keep you updated if we need more time.
        </p>

        <h2>What to include</h2>
        <p>To help us resolve your request quickly, please include:</p>
        <ul>
          <li>The email address you use to sign in to {brand.name}.</li>
          <li>Your connected Instagram handle (e.g., @yourusername).</li>
          <li>A clear description of the issue or question.</li>
          <li>Screenshots of any error messages or unexpected behavior, if applicable.</li>
          <li>For billing or refund requests: the charge date and plan name.</li>
        </ul>

        <h2>Account and data deletion</h2>
        <p>
          You can disconnect Instagram accounts from Settings at any time. To request deletion
          of your {brand.name} account and associated personal data, email{" "}
          <a href={`mailto:${brand.contactEmail}`}>{brand.contactEmail}</a> with the subject
          line "Data deletion". See our{" "}
          <Link to="/privacy">Privacy Policy</Link> for more details.
        </p>

        <h2>Other policies</h2>
        <p>
          Legal and billing information is available on our{" "}
          <Link to="/terms">Terms &amp; Conditions</Link>,{" "}
          <Link to="/privacy">Privacy Policy</Link>, and{" "}
          <Link to="/refund">Return &amp; Refund Policy</Link> pages.
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
