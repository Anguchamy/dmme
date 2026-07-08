import { useRef } from "react";

/** True only on devices with a fine pointer and no reduced-motion preference. */
function tiltEnabled() {
  if (typeof window === "undefined" || !window.matchMedia) return false;
  const fine = window.matchMedia("(hover: hover) and (pointer: fine)").matches;
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  return fine && !reduced;
}

/**
 * Wraps a card in an interactive 3D tilt that follows the pointer.
 * Drop-in replacement for a card's outer <div> — pass the same className.
 * Gracefully disabled on touch devices and when reduced-motion is set.
 */
export default function Tilt({ className = "", max = 9, children, ...rest }) {
  const ref = useRef(null);

  function onMove(e) {
    if (!tiltEnabled() || !ref.current) return;
    const el = ref.current;
    const r = el.getBoundingClientRect();
    const px = (e.clientX - r.left) / r.width - 0.5;
    const py = (e.clientY - r.top) / r.height - 0.5;
    el.style.setProperty("--rx", `${(-py * max).toFixed(2)}deg`);
    el.style.setProperty("--ry", `${(px * max).toFixed(2)}deg`);
    el.classList.add("tilt-active");
  }

  function reset() {
    if (!ref.current) return;
    ref.current.style.setProperty("--rx", "0deg");
    ref.current.style.setProperty("--ry", "0deg");
    ref.current.classList.remove("tilt-active");
  }

  return (
    <div
      ref={ref}
      className={`tilt ${className}`}
      onMouseMove={onMove}
      onMouseLeave={reset}
      {...rest}
    >
      {children}
    </div>
  );
}
