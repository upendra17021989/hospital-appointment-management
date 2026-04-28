package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.CheckoutSessionResponse;
import com.hospital.dto.Dtos.SubscriptionPlanResponse;
import com.hospital.dto.Dtos.SubscriptionResponse;
import com.hospital.dto.Dtos.SubscriptionUsageResponse;
import com.hospital.model.Hospital;
import com.hospital.model.HospitalSubscription;
import com.hospital.model.SubscriptionPlan;
import com.hospital.model.User;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.HospitalSubscriptionRepo;
import com.hospital.repository.SubscriptionPlanRepo;
import com.hospital.security.TenantContext;
import com.hospital.service.PaymentService;
import com.hospital.service.SubscriptionService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Management", description = "Subscription plans, checkout, and status endpoints")
public class SubscriptionController {

    private final SubscriptionPlanRepo planRepo;
    private final HospitalSubscriptionRepo subscriptionRepo;
    private final HospitalRepo hospitalRepo;
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final TenantContext tenantContext;

    @Value("${stripe.success.url}")
    private String successUrl;

    // ── GET /subscriptions/plans - Public list of active plans ──

    @GetMapping("/plans")
    @Operation(summary = "Get all active subscription plans (public)")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getPlans() {
        List<SubscriptionPlan> plans = planRepo.findByIsActiveTrueOrderByMonthlyPriceAsc();
        List<SubscriptionPlanResponse> responses = plans.stream()
                .map(this::toPlanResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ── GET /subscriptions/me - Current hospital subscription ──

    @GetMapping("/me")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get current hospital's subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMySubscription(
            @AuthenticationPrincipal User user) {

        UUID hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        if (hospitalId == null && user.getRole() == User.Role.SUPER_ADMIN) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        if (hospitalId == null) {
            return ResponseEntity.ok(ApiResponse.error("No hospital associated with user"));
        }

        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId).orElse(null);
        if (sub == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        return ResponseEntity.ok(ApiResponse.success(toSubscriptionResponse(sub)));
    }

    // ── GET /subscriptions/usage - Current usage against limits ──

    @GetMapping("/usage")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get current subscription usage statistics")
    public ResponseEntity<ApiResponse<SubscriptionUsageResponse>> getUsage(
            @AuthenticationPrincipal User user) {

        UUID hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        if (hospitalId == null) {
            return ResponseEntity.ok(ApiResponse.error("No hospital associated with user"));
        }

        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getUsage(hospitalId)));
    }

    // ── POST /subscriptions/checkout - Create Stripe checkout session ──

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Start subscription checkout with Stripe")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> createCheckout(
            @AuthenticationPrincipal User user,
            @RequestParam UUID planId,
            @RequestParam(defaultValue = "monthly") String billingCycle) {

        UUID hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        if (hospitalId == null) {
            return ResponseEntity.ok(ApiResponse.error("No hospital associated with user"));
        }

        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));

        SubscriptionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        // Free plans bypass Stripe and activate immediately
        BigDecimal planPrice = "yearly".equalsIgnoreCase(billingCycle)
                ? plan.getYearlyPrice()
                : plan.getMonthlyPrice();
        if (planPrice != null && planPrice.compareTo(BigDecimal.ZERO) == 0) {
            HospitalSubscription.BillingCycle cycle = "yearly".equalsIgnoreCase(billingCycle)
                    ? HospitalSubscription.BillingCycle.yearly
                    : HospitalSubscription.BillingCycle.monthly;
            subscriptionService.createOrUpdateSubscription(hospital, plan,
                    HospitalSubscription.Status.active, cycle);
            return ResponseEntity.ok(ApiResponse.success(
                    CheckoutSessionResponse.builder().checkoutUrl(successUrl + "?free_plan=true").build()));
        }

        try {
            String checkoutUrl = paymentService.createCheckoutSession(
                    hospitalId, planId, billingCycle, user.getEmail(), hospital.getName());
            return ResponseEntity.ok(ApiResponse.success(
                    CheckoutSessionResponse.builder().checkoutUrl(checkoutUrl).build()));
        } catch (StripeException e) {
            return ResponseEntity.ok(ApiResponse.error("Payment service error: " + e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // ── POST /subscriptions/cancel - Cancel subscription ──

    @PostMapping("/cancel")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    @Operation(summary = "Cancel current subscription (downgrades to free at period end)")
    public ResponseEntity<ApiResponse<String>> cancelSubscription(
            @AuthenticationPrincipal User user) {

        UUID hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        if (hospitalId == null) {
            return ResponseEntity.ok(ApiResponse.error("No hospital associated with user"));
        }

        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId).orElse(null);
        if (sub == null) {
            return ResponseEntity.ok(ApiResponse.error("No active subscription found"));
        }

        sub.setStatus(HospitalSubscription.Status.cancelled);
        sub.setCancelledAt(LocalDateTime.now());
        subscriptionRepo.save(sub);

        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled. You will keep access until the end of your billing period.", "cancelled"));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .slug(plan.getSlug())
                .description(plan.getDescription())
                .monthlyPrice(plan.getMonthlyPrice())
                .yearlyPrice(plan.getYearlyPrice())
                .maxDoctors(plan.getMaxDoctors())
                .maxUsers(plan.getMaxUsers())
                .maxAppointmentsPerMonth(plan.getMaxAppointmentsPerMonth())
                .allowPrescriptions(plan.getAllowPrescriptions())
                .allowSms(plan.getAllowSms())
                .allowWhatsapp(plan.getAllowWhatsapp())
                .allowCustomBranding(plan.getAllowCustomBranding())
                .prioritySupport(plan.getPrioritySupport())
                .isActive(plan.getIsActive())
                .build();
    }

    private SubscriptionResponse toSubscriptionResponse(HospitalSubscription sub) {
        SubscriptionPlan plan = sub.getPlan();
        boolean isTrial = sub.getStatus() == HospitalSubscription.Status.trial;
        boolean isExpired = sub.isExpired();

        Integer daysUntilExpiry = null;
        if (isTrial && sub.getTrialEndsAt() != null) {
            daysUntilExpiry = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getTrialEndsAt());
        } else if (sub.getStatus() == HospitalSubscription.Status.active && sub.getCurrentPeriodEnd() != null) {
            daysUntilExpiry = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getCurrentPeriodEnd());
        }
        if (daysUntilExpiry != null && daysUntilExpiry < 0) daysUntilExpiry = 0;

        return SubscriptionResponse.builder()
                .id(sub.getId())
                .plan(plan != null ? toPlanResponse(plan) : null)
                .status(sub.getStatus().name())
                .billingCycle(sub.getBillingCycle() != null ? sub.getBillingCycle().name() : null)
                .trialEndsAt(sub.getTrialEndsAt())
                .currentPeriodStart(sub.getCurrentPeriodStart())
                .currentPeriodEnd(sub.getCurrentPeriodEnd())
                .cancelledAt(sub.getCancelledAt())
                .isTrial(isTrial)
                .isExpired(isExpired)
                .daysUntilExpiry(daysUntilExpiry)
                .build();
    }
}

