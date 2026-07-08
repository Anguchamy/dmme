/**
 * Decorative animated 3D-style gradient orbs. Purely cosmetic and
 * non-interactive (pointer-events: none). Animation is disabled via CSS
 * when the user prefers reduced motion. Place inside a position:relative parent.
 */
export default function Backdrop3D({ variant = "" }) {
  return (
    <div className={`bg3d ${variant}`} aria-hidden="true">
      <span className="orb orb-1" />
      <span className="orb orb-2" />
      <span className="orb orb-3" />
      <span className="orb-grid" />
    </div>
  );
}
