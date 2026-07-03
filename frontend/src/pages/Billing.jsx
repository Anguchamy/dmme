import { useEffect, useState } from "react";
import { api } from "../lib/api";

export default function Billing() {
  const [plans, setPlans] = useState([]);
  const [me, setMe] = useState(null);
  const [msg, setMsg] = useState("");

  const load = () => {
    api.get("/api/plans").then(setPlans).catch(() => {});
    api.get("/api/me").then(setMe).catch(() => {});
  };
  useEffect(load, []);

  async function upgrade(planCode) {
    setMsg("");
    try {
      const order = await api.post("/api/billing/order", { planCode });
      const rzp = new window.Razorpay({
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: "dmme",
        description: `Upgrade to ${planCode}`,
        order_id: order.orderId,
        handler: async (resp) => {
          await api.post("/api/billing/verify", {
            razorpayOrderId: resp.razorpay_order_id,
            razorpayPaymentId: resp.razorpay_payment_id,
            razorpaySignature: resp.razorpay_signature,
          });
          setMsg(`You're now on ${planCode}!`);
          load();
        },
      });
      rzp.open();
    } catch (e) {
      setMsg(e.message);
    }
  }

  const price = (p) =>
    p.priceMinor === 0 ? "Free" : `₹${(p.priceMinor / 100).toLocaleString("en-IN")}/mo`;

  return (
    <div>
      <h1>Billing</h1>
      <p style={{ color: "var(--muted)" }}>
        Current plan: <span className="tag">{me?.planCode || "FREE"}</span>
      </p>
      {msg && <div className="card" style={{ marginBottom: 16 }}>{msg}</div>}

      <div className="pricing" style={{ marginTop: 20 }}>
        {plans.map((p) => {
          const current = me?.planCode === p.code;
          const purchasable = p.priceMinor > 0 && !current;
          return (
            <div className={`plan ${p.code === "PRO" ? "featured" : ""}`} key={p.code}>
              <h3>{p.name}</h3>
              <div className="price">{price(p)}</div>
              <ul>{(p.features || []).map((f) => <li key={f}>{f}</li>)}</ul>
              {current ? (
                <button className="btn btn-ghost" style={{ width: "100%" }} disabled>Current plan</button>
              ) : purchasable ? (
                <button className="btn btn-primary" style={{ width: "100%" }} onClick={() => upgrade(p.code)}>
                  Upgrade
                </button>
              ) : (
                <button className="btn btn-ghost" style={{ width: "100%" }} disabled>
                  {p.code === "AGENCY" ? "Contact sales" : "—"}
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
