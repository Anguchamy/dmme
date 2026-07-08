import { useId } from "react";

/**
 * dmme logo mark: a DM speech bubble with a lightning bolt (automation),
 * on a rounded blue gradient tile.
 */
export function LogoMark({ size = 32 }) {
  const id = useId();
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="dmme logo"
    >
      <defs>
        <linearGradient id={id} x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
          <stop stopColor="#2b2bff" />
          <stop offset="1" stopColor="#4f46e5" />
        </linearGradient>
      </defs>
      <rect width="40" height="40" rx="11" fill={`url(#${id})`} />
      {/* speech bubble */}
      <rect x="7" y="9" width="26" height="17" rx="8" fill="#fff" />
      <path d="M13 24 L13 31 L20.5 25 Z" fill="#fff" />
      {/* lightning bolt (cut-out to the tile gradient) */}
      <path d="M21 12 L16 18 L19.5 18 L18.5 23 L24 16 L20.5 16 Z" fill={`url(#${id})`} />
    </svg>
  );
}

/** Full lockup: mark + "dmme" wordmark. */
export function Logo({ size = 30, color = "var(--text)" }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 10 }}>
      <LogoMark size={size} />
      <span
        style={{
          fontWeight: 800,
          fontSize: Math.round(size * 0.72),
          letterSpacing: "-0.5px",
          color,
        }}
      >
        dmme
      </span>
    </span>
  );
}
