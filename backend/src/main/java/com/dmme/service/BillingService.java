package com.dmme.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.dmme.config.AppProperties;
import com.dmme.domain.AppUser;
import com.dmme.domain.Payment;
import com.dmme.domain.Plan;
import com.dmme.domain.Subscription;
import com.dmme.repository.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Razorpay integration: creates orders for plan upgrades, verifies the payment
 * signature returned by Razorpay Checkout, and activates the subscription.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final AppProperties props;
    private final PlanRepository plans;
    private final PaymentRepository payments;
    private final SubscriptionRepository subscriptions;
    private final AppUserRepository users;

    public BillingService(AppProperties props, PlanRepository plans, PaymentRepository payments,
                          SubscriptionRepository subscriptions, AppUserRepository users) {
        this.props = props;
        this.plans = plans;
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.users = users;
    }

    private RazorpayClient client() {
        try {
            return new RazorpayClient(props.getRazorpay().getKeyId(), props.getRazorpay().getKeySecret());
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Razorpay not configured");
        }
    }

    /** Create a Razorpay order for a plan upgrade. Returns checkout parameters. */
    @Transactional
    public Map<String, Object> createOrder(UUID userId, String planCode) {
        Plan plan = plans.findById(planCode)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Unknown plan"));
        if (plan.getPriceMinor() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Plan is not purchasable online");
        }
        String keyId = props.getRazorpay().getKeyId();
        String keySecret = props.getRazorpay().getKeySecret();
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            log.error("Razorpay keys are not configured (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET)");
            throw new ResponseStatusException(BAD_REQUEST,
                    "Payments are not configured yet. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET on the server.");
        }
        try {
            JSONObject req = new JSONObject();
            req.put("amount", plan.getPriceMinor());
            req.put("currency", plan.getCurrency());
            String receipt = "rcpt_" + userId.toString().replace("-", "").substring(0, 12)
                    + "_" + Long.toString(System.currentTimeMillis(), 36);
            req.put("receipt", receipt.length() > 40 ? receipt.substring(0, 40) : receipt);
            req.put("notes", new JSONObject().put("planCode", planCode).put("userId", userId.toString()));

            Order order = client().orders.create(req);
            String orderId = order.get("id");

            Payment p = new Payment();
            p.setUserId(userId);
            p.setRazorpayOrderId(orderId);
            p.setAmountMinor(plan.getPriceMinor());
            p.setCurrency(plan.getCurrency());
            p.setStatus("CREATED");
            payments.save(p);

            return Map.of(
                    "orderId", orderId,
                    "amount", plan.getPriceMinor(),
                    "currency", plan.getCurrency(),
                    "keyId", props.getRazorpay().getKeyId(),
                    "planCode", planCode);
        } catch (Exception e) {
            log.error("Failed to create Razorpay order", e);
            throw new ResponseStatusException(BAD_REQUEST, "Could not create order: " + e.getMessage());
        }
    }

    /** Verify the Checkout signature and, if valid, activate the plan. */
    @Transactional
    public void verifyAndActivate(UUID userId, String orderId, String paymentId, String signature) {
        try {
            JSONObject attrs = new JSONObject();
            attrs.put("razorpay_order_id", orderId);
            attrs.put("razorpay_payment_id", paymentId);
            attrs.put("razorpay_signature", signature);
            boolean valid = Utils.verifyPaymentSignature(attrs, props.getRazorpay().getKeySecret());
            if (!valid) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid payment signature");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Signature verification failed");
        }

        Payment p = payments.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
        p.setRazorpayPaymentId(paymentId);
        p.setStatus("PAID");
        payments.save(p);

        // Resolve the plan from the paid amount (mirrors the order's plan).
        Plan plan = plans.findAllByOrderBySortOrderAsc().stream()
                .filter(pl -> pl.getPriceMinor() == p.getAmountMinor() && pl.getPriceMinor() > 0)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Plan not resolved"));

        activatePlan(userId, plan.getCode());
    }

    @Transactional
    public void activatePlan(UUID userId, String planCode) {
        AppUser u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        u.setPlanCode(planCode);
        users.save(u);

        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setPlanCode(planCode);
        sub.setStatus("ACTIVE");
        subscriptions.save(sub);
    }

    /** Verify a Razorpay webhook signature (X-Razorpay-Signature header). */
    public boolean verifyWebhook(String payload, String signature) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, props.getRazorpay().getWebhookSecret());
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }
}
