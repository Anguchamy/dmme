package com.dmme.web;

import com.dmme.service.BillingService;
import com.dmme.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billing;
    private final CurrentUserService currentUser;

    public BillingController(BillingService billing, CurrentUserService currentUser) {
        this.billing = billing;
        this.currentUser = currentUser;
    }

    public record OrderRequest(String planCode) {}

    /** Step 1: create a Razorpay order; frontend opens Checkout with the result. */
    @PostMapping("/order")
    public Map<String, Object> order(@RequestBody OrderRequest req) {
        currentUser.require();
        return billing.createOrder(currentUser.userId(), req.planCode());
    }

    public record VerifyRequest(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {}

    /** Step 2: verify the Checkout signature and activate the plan. */
    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody VerifyRequest req) {
        currentUser.require();
        billing.verifyAndActivate(currentUser.userId(),
                req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        return Map.of("status", "active");
    }
}
