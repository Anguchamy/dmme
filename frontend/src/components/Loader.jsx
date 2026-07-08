import { useEffect, useState } from "react";
import { LogoMark } from "./Logo";

/** Small inline gradient ring spinner. */
export function Spinner({ size = 22 }) {
  return <span className="spinner" style={{ width: size, height: size }} aria-label="Loading" />;
}

/**
 * Full-screen branded loader. Used for auth checks and slow backend responses.
 * After a short delay it shows a friendly note (the backend can cold-start).
 */
export function FullPageLoader({ label = "Loading…" }) {
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    const t = setTimeout(() => setSlow(true), 3500);
    return () => clearTimeout(t);
  }, []);
  return (
    <div className="full-loader" role="status" aria-live="polite">
      <div className="full-loader-mark">
        <span className="loader-ring" />
        <span className="loader-logo"><LogoMark size={40} /></span>
      </div>
      <div className="full-loader-text">{label}</div>
      {slow && <div className="full-loader-sub">Waking things up — this can take a few seconds…</div>}
    </div>
  );
}

/** Inline centered loader for page/section content. */
export function SectionLoader({ label = "Loading…" }) {
  return (
    <div className="section-loader" role="status" aria-live="polite">
      <Spinner size={26} />
      <span>{label}</span>
    </div>
  );
}

/** Simple shimmering skeleton block. */
export function Skeleton({ width = "100%", height = 16, radius = 8, style }) {
  return <span className="skeleton" style={{ width, height, borderRadius: radius, ...style }} />;
}

/** Skeleton rows for tables while data loads. */
export function TableSkeleton({ rows = 5, cols = 4 }) {
  return (
    <div className="table-card">
      <div className="table-skeleton">
        {Array.from({ length: rows }).map((_, r) => (
          <div className="tsk-row" key={r}>
            {Array.from({ length: cols }).map((__, c) => (
              <Skeleton key={c} height={14} width={c === 0 ? 40 : `${60 + ((r + c) % 3) * 12}%`} />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
