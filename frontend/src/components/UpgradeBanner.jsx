import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { IconRocket } from "./DashIcons";

/** Blue "Unlock Pro" banner shown to free-plan users. Renders nothing for paid plans. */
export default function UpgradeBanner() {
  const [isFree, setIsFree] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    api.get("/api/me")
      .then((m) => setIsFree(!m?.planCode || m.planCode === "FREE"))
      .catch(() => {});
  }, []);

  if (!isFree) return null;

  return (
    <div className="pro-banner">
      <div className="pro-banner-text">
        <div className="pro-banner-title"><IconRocket /> Unlock Pro Power!</div>
        <div className="pro-banner-sub">Get unlimited automations, contacts & advanced analytics.</div>
      </div>
      <button className="pro-banner-btn" onClick={() => navigate("/app/billing")}>Upgrade to Pro</button>
    </div>
  );
}
