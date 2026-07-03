package com.dmme.web;

import com.dmme.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Razorpay webhook endpoint (configure in Razorpay dashboard). Handles async
 * events such as subscription charges and payment failures. The raw body is
 * required for signature verification, so we accept it as a String.
 */
@RestController
@RequestMapping("/api/webhooks/razorpay")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final BillingService billing;

    public RazorpayWebhookController(BillingService billing) {
        this.billing = billing;
    }

    @PostMapping
    public ResponseEntity<String> receive(
            @RequestBody String payload,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature) {
        if (signature == null || !billing.verifyWebhook(payload, signature)) {
            return ResponseEntity.status(400).body("Invalid signature");
        }
        // Event bodies are logged for now; extend here to sync subscription state
        // (subscription.charged, subscription.halted, payment.failed, etc.).
        log.info("Razorpay webhook received: {}", payload);
        return ResponseEntity.ok("OK");
    }
}
