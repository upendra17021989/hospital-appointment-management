package com.hospital.service;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.hospital.model.Hospital;
import com.hospital.model.HospitalSubscription;
import com.hospital.model.Payment;
import com.hospital.model.SubscriptionPlan;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.HospitalSubscriptionRepo;
import com.hospital.repository.PaymentRepo;
import com.hospital.repository.SubscriptionPlanRepo;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepo paymentRepo;
    private final HospitalSubscriptionRepo subscriptionRepo;
    private final SubscriptionPlanRepo planRepo;
    private final HospitalRepo hospitalRepo;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @Value("${stripe.fallback.price.id:}")
    private String fallbackPriceId;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Create a Stripe Checkout Session for subscription purchase/upgrade.
     */
    @Transactional
    public String createCheckoutSession(UUID hospitalId, UUID planId, String billingCycle, String email, String hospitalName) throws StripeException {
        SubscriptionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));

        // Find or create Stripe customer
        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId).orElse(null);
        String customerId = sub != null ? sub.getStripeCustomerId() : null;

        if (customerId == null || customerId.isBlank()) {
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(hospitalName)
                    .putMetadata("hospital_id", hospitalId.toString())
                    .build();
            Customer customer = Customer.create(customerParams);
            customerId = customer.getId();
        }

        // Free plans should not go through Stripe checkout
        BigDecimal planPrice = "yearly".equalsIgnoreCase(billingCycle)
                ? plan.getYearlyPrice()
                : plan.getMonthlyPrice();
        if (planPrice != null && planPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Free plans cannot be processed through Stripe. Please activate the free plan directly.");
        }

        String priceId = "yearly".equalsIgnoreCase(billingCycle)
                ? plan.getStripePriceIdYearly()
                : plan.getStripePriceIdMonthly();

        if (priceId == null || priceId.isBlank()) {
            if (fallbackPriceId != null && !fallbackPriceId.isBlank()) {
                log.warn("No Stripe price ID configured for plan {}. Using configured fallback price ID.", plan.getSlug());
                priceId = fallbackPriceId;
            } else {
                throw new IllegalStateException(
                        "No Stripe price ID configured for plan '" + plan.getSlug() + "' and billing cycle '" + billingCycle + "'. " +
                        "Please set the stripe_price_id_monthly / stripe_price_id_yearly columns in the subscription_plans table, " +
                        "or configure stripe.fallback.price.id in application.properties for testing."
                );
            }
        }

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .putMetadata("hospital_id", hospitalId.toString())
                .putMetadata("plan_id", planId.toString())
                .putMetadata("billing_cycle", billingCycle)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                );

        // If upgrading from an existing paid subscription, allow proration
        if (sub != null && sub.getStripeSubscriptionId() != null) {
            sessionBuilder.setSubscriptionData(
                SessionCreateParams.SubscriptionData.builder()
                    .setBillingCycleAnchor(
                        Instant.now()
                            .plusSeconds(60)
                            .getEpochSecond()
                    )
                    .setProrationBehavior(
                        SessionCreateParams.SubscriptionData.ProrationBehavior.CREATE_PRORATIONS
                    )
                    .build()
            );
        }

        Session session = Session.create(sessionBuilder.build());
        return session.getUrl();
    }

    /**
     * Handle Stripe webhook events.
     */
    @Transactional
    public void handleWebhook(Event event) {
        log.info("Stripe webhook received: type={}, id={}", event.getType(), event.getId());

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
            case "invoice.payment_failed":
                handleInvoicePaymentFailed(event);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;
            case "customer.subscription.updated":
                handleSubscriptionUpdated(event);
                break;
            default:
                log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {

        log.info("Event received: type={}, apiVersion={}", event.getType(), event.getApiVersion());

        // ✅ Only process correct event
        if (!"checkout.session.completed".equals(event.getType())) {
            log.warn("Ignoring unsupported event type: {}", event.getType());
            return;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        Session session = null;

        try {
            // ✅ First try safe deserialization
            if (deserializer.getObject().isPresent()) {
                session = (Session) deserializer.getObject().get();
                log.info("✅ Safe deserialization worked");
            } else {
                // ⚠️ Fallback to unsafe (required for API mismatch)
                log.warn("⚠️ Safe deserialization failed, using unsafe fallback");

                StripeObject stripeObject = deserializer.deserializeUnsafe();

                if (stripeObject instanceof Session) {
                    session = (Session) stripeObject;
                    log.info("✅ Unsafe deserialization worked");
                } else {
                    log.error("❌ Deserialized object is not a Session: {}", stripeObject.getClass());
                    return;
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to deserialize Stripe event", e);
            return;
        }

        // ❌ Still null → stop
        if (session == null) {
            log.error("❌ Session is NULL even after unsafe deserialization");
            return;
        }

        // ✅ Debug logs
        log.info("Session ID: {}", session.getId());
        log.info("Customer: {}", session.getCustomer());
        log.info("Subscription: {}", session.getSubscription());
        log.info("Payment Status: {}", session.getPaymentStatus());

        // ✅ Extract metadata
        String hospitalIdStr = session.getMetadata() != null ? session.getMetadata().get("hospital_id") : null;
        String planIdStr = session.getMetadata() != null ? session.getMetadata().get("plan_id") : null;
        String billingCycle = session.getMetadata() != null ? session.getMetadata().get("billing_cycle") : "monthly";

        log.info("Metadata → hospitalId={}, planId={}, billingCycle={}",
                hospitalIdStr, planIdStr, billingCycle);

        if (hospitalIdStr == null || planIdStr == null) {
            log.error("❌ Missing metadata, skipping processing");
            return;
        }

        try {
            UUID hospitalId = UUID.fromString(hospitalIdStr);
            UUID planId = UUID.fromString(planIdStr);

            Hospital hospital = hospitalRepo.findById(hospitalId).orElse(null);
            SubscriptionPlan plan = planRepo.findById(planId).orElse(null);

            if (hospital == null || plan == null) {
                log.error("❌ Invalid hospital or plan");
                return;
            }

            HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId).orElse(null);

            if (sub == null) {
                sub = HospitalSubscription.builder()
                        .hospital(hospital)
                        .plan(plan)
                        .build();
            }

            sub.setPlan(plan);
            sub.setStatus(HospitalSubscription.Status.active);
            sub.setBillingCycle("yearly".equalsIgnoreCase(billingCycle)
                    ? HospitalSubscription.BillingCycle.yearly
                    : HospitalSubscription.BillingCycle.monthly);
            sub.setStripeCustomerId(session.getCustomer());
            sub.setStripeSubscriptionId(session.getSubscription());
            sub.setTrialEndsAt(null);

            LocalDateTime computedStart = LocalDateTime.now();
            int monthsToAdd = "yearly".equalsIgnoreCase(billingCycle) ? 12 : 1;
            LocalDateTime computedEnd = computedStart.plusMonths(monthsToAdd);

            log.info("[checkout.session.completed] hospitalId={}, billingCycle={}, monthsToAdd={}, computedStart={}, computedEnd={}",
                    hospitalId, billingCycle, monthsToAdd, computedStart, computedEnd);

            sub.setCurrentPeriodStart(computedStart);
            sub.setCurrentPeriodEnd(computedEnd);


            subscriptionRepo.save(sub);

            log.info("✅ Subscription activated for hospital {}", hospitalId);

        } catch (Exception e) {
            log.error("❌ Error processing subscription logic", e);
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        log.info("Event received: type={}, apiVersion={}", event.getType(), event.getApiVersion());

        // ✅ Only process correct event
        if (!"invoice.payment_succeeded".equals(event.getType())) {
            log.warn("Ignoring unsupported event type: {}", event.getType());
            return;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        Invoice invoice = null;

        try {
            // ✅ First try safe deserialization
            if (deserializer.getObject().isPresent()) {
                invoice = (Invoice) deserializer.getObject().get();
                log.info("✅ Safe deserialization worked");
            } else {
                // ⚠️ Fallback to unsafe (required for API mismatch)
                log.warn("⚠️ Safe deserialization failed, using unsafe fallback");

                StripeObject stripeObject = deserializer.deserializeUnsafe();

                if (stripeObject instanceof Invoice) {
                    invoice = (Invoice) stripeObject;
                    log.info("✅ Unsafe deserialization worked");
                } else {
                    log.error("❌ Deserialized object is not an Invoice: {}", stripeObject.getClass());
                    return;
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to deserialize Stripe event", e);
            return;
        }

        // ❌ Still null → stop
        if (invoice == null) {
            log.error("❌ Invoice is NULL even after unsafe deserialization");
            return;
        }

        String customerId = invoice.getCustomer();
        Optional<HospitalSubscription> subOpt = subscriptionRepo.findByStripeCustomerId(customerId);
        if (subOpt.isEmpty()) return;

        HospitalSubscription sub = subOpt.get();

        // Record payment
        Payment payment = Payment.builder()
                .hospital(sub.getHospital())
                .subscription(sub)
                .stripeInvoiceId(invoice.getId())
                .stripePaymentIntentId(invoice.getPaymentIntent())
                .amount(BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100)))
                .currency(invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase() : "USD")
                .status(Payment.Status.succeeded)
                .description("Subscription payment - " + sub.getPlan().getName())
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepo.save(payment);

        // Update subscription period (prevent clobbering computed end with unexpected Stripe payload)
        if (invoice.getPeriodStart() != null) {
            LocalDateTime stripeStart = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(invoice.getPeriodStart()), ZoneId.systemDefault());
            if (sub.getCurrentPeriodStart() == null) {
                sub.setCurrentPeriodStart(stripeStart);
            }

        }
        if (invoice.getPeriodEnd() != null) {
            LocalDateTime stripeEnd = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(invoice.getPeriodEnd()), ZoneId.systemDefault());
            if (sub.getCurrentPeriodEnd() == null || stripeEnd.isAfter(sub.getCurrentPeriodEnd())) {
                sub.setCurrentPeriodEnd(stripeEnd);
            }
        }

        // Only flip status; period end should remain what we computed unless Stripe sends a newer value.
        sub.setStatus(HospitalSubscription.Status.active);
        subscriptionRepo.save(sub);



        log.info("Payment succeeded for hospital {}: ${}", sub.getHospital().getId(), payment.getAmount());
    }

    private void handleInvoicePaymentFailed(Event event) {
        log.info("Event received: type={}, apiVersion={}", event.getType(), event.getApiVersion());

        // ✅ Only process correct event
        if (!"invoice.payment_failed".equals(event.getType())) {
            log.warn("Ignoring unsupported event type: {}", event.getType());
            return;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        Invoice invoice = null;

        try {
            // ✅ First try safe deserialization
            if (deserializer.getObject().isPresent()) {
                invoice = (Invoice) deserializer.getObject().get();
                log.info("✅ Safe deserialization worked");
            } else {
                // ⚠️ Fallback to unsafe (required for API mismatch)
                log.warn("⚠️ Safe deserialization failed, using unsafe fallback");

                StripeObject stripeObject = deserializer.deserializeUnsafe();

                if (stripeObject instanceof Invoice) {
                    invoice = (Invoice) stripeObject;
                    log.info("✅ Unsafe deserialization worked");
                } else {
                    log.error("❌ Deserialized object is not an Invoice: {}", stripeObject.getClass());
                    return;
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to deserialize Stripe event", e);
            return;
        }

        // ❌ Still null → stop
        if (invoice == null) {
            log.error("❌ Invoice is NULL even after unsafe deserialization");
            return;
        }

        String customerId = invoice.getCustomer();
        Optional<HospitalSubscription> subOpt = subscriptionRepo.findByStripeCustomerId(customerId);
        if (subOpt.isEmpty()) return;

        HospitalSubscription sub = subOpt.get();
        sub.setStatus(HospitalSubscription.Status.past_due);
        subscriptionRepo.save(sub);

        Payment payment = Payment.builder()
                .hospital(sub.getHospital())
                .subscription(sub)
                .stripeInvoiceId(invoice.getId())
                .amount(BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100)))
                .currency(invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase() : "USD")
                .status(Payment.Status.failed)
                .description("Failed subscription payment - " + sub.getPlan().getName())
                .build();
        paymentRepo.save(payment);

        log.warn("Payment failed for hospital {}", sub.getHospital().getId());
    }

    private void handleSubscriptionDeleted(Event event) {
        log.info("Event received: type={}, apiVersion={}", event.getType(), event.getApiVersion());

        // ✅ Only process correct event
        if (!"customer.subscription.deleted".equals(event.getType())) {
            log.warn("Ignoring unsupported event type: {}", event.getType());
            return;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        Subscription stripeSub = null;

        try {
            // ✅ First try safe deserialization
            if (deserializer.getObject().isPresent()) {
                stripeSub = (Subscription) deserializer.getObject().get();
                log.info("✅ Safe deserialization worked");
            } else {
                // ⚠️ Fallback to unsafe (required for API mismatch)
                log.warn("⚠️ Safe deserialization failed, using unsafe fallback");

                StripeObject stripeObject = deserializer.deserializeUnsafe();

                if (stripeObject instanceof Subscription) {
                    stripeSub = (Subscription) stripeObject;
                    log.info("✅ Unsafe deserialization worked");
                } else {
                    log.error("❌ Deserialized object is not a Subscription: {}", stripeObject.getClass());
                    return;
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to deserialize Stripe event", e);
            return;
        }

        // ❌ Still null → stop
        if (stripeSub == null) {
            log.error("❌ Subscription is NULL even after unsafe deserialization");
            return;
        }

        Optional<HospitalSubscription> subOpt = subscriptionRepo.findByStripeSubscriptionId(stripeSub.getId());
        if (subOpt.isEmpty()) return;

        HospitalSubscription sub = subOpt.get();
        sub.setStatus(HospitalSubscription.Status.cancelled);
        sub.setCancelledAt(LocalDateTime.now());
        subscriptionRepo.save(sub);

        log.info("Subscription cancelled for hospital {}", sub.getHospital().getId());
    }

    private void handleSubscriptionUpdated(Event event) {
         log.info("Event received: type={}, apiVersion={}", event.getType(), event.getApiVersion());

        // ✅ Only process correct event
        if (!"customer.subscription.updated".equals(event.getType())) {
            log.warn("Ignoring unsupported event type: {}", event.getType());
            return;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        Subscription stripeSub = null;

        try {
            // ✅ First try safe deserialization
            if (deserializer.getObject().isPresent()) {
                stripeSub = (Subscription) deserializer.getObject().get();
                log.info("✅ Safe deserialization worked");
            } else {
                // ⚠️ Fallback to unsafe (required for API mismatch)
                log.warn("⚠️ Safe deserialization failed, using unsafe fallback");

                StripeObject stripeObject = deserializer.deserializeUnsafe();

                if (stripeObject instanceof Subscription) {
                    stripeSub = (Subscription) stripeObject;
                    log.info("✅ Unsafe deserialization worked");
                } else {
                    log.error("❌ Deserialized object is not a Subscription: {}", stripeObject.getClass());
                    return;
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to deserialize Stripe event", e);
            return;
        }

        // ❌ Still null → stop
        if (stripeSub == null) {
            log.error("❌ Subscription is NULL even after unsafe deserialization");
            return;
        }
        Optional<HospitalSubscription> subOpt = subscriptionRepo.findByStripeSubscriptionId(stripeSub.getId());
        if (subOpt.isEmpty()) return;

        HospitalSubscription sub = subOpt.get();

        switch (stripeSub.getStatus()) {
            case "active":
                sub.setStatus(HospitalSubscription.Status.active);
                break;
            case "past_due":
                sub.setStatus(HospitalSubscription.Status.past_due);
                break;
            case "canceled":
            case "unpaid":
                sub.setStatus(HospitalSubscription.Status.expired);
                break;
            default:
                break;
        }

        if (stripeSub.getCurrentPeriodStart() != null) {
            sub.setCurrentPeriodStart(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart()), ZoneId.systemDefault()));
        }
        if (stripeSub.getCurrentPeriodEnd() != null) {
            sub.setCurrentPeriodEnd(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()), ZoneId.systemDefault()));
        }

        subscriptionRepo.save(sub);
    }
}

