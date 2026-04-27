package com.hospital.service;

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
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
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

        String priceId = "yearly".equalsIgnoreCase(billingCycle)
                ? plan.getStripePriceIdYearly()
                : plan.getStripePriceIdMonthly();

        if (priceId == null || priceId.isBlank()) {
            // Fallback: use test mode without real Stripe price IDs
            log.warn("No Stripe price ID configured for plan {}. Running in test mode.", plan.getSlug());
            priceId = "price_test_fallback";
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
                            .setProrationBehavior(SessionCreateParams.SubscriptionData.ProrationBehavior.CREATE_PRORATIONS)
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
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) return;

        String hospitalIdStr = session.getMetadata() != null ? session.getMetadata().get("hospital_id") : null;
        String planIdStr = session.getMetadata() != null ? session.getMetadata().get("plan_id") : null;
        String billingCycle = session.getMetadata() != null ? session.getMetadata().get("billing_cycle") : "monthly";

        if (hospitalIdStr == null || planIdStr == null) return;

        UUID hospitalId = UUID.fromString(hospitalIdStr);
        UUID planId = UUID.fromString(planIdStr);

        Hospital hospital = hospitalRepo.findById(hospitalId).orElse(null);
        SubscriptionPlan plan = planRepo.findById(planId).orElse(null);
        if (hospital == null || plan == null) return;

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
        sub.setCurrentPeriodStart(LocalDateTime.now());
        sub.setCurrentPeriodEnd(LocalDateTime.now().plusMonths("yearly".equalsIgnoreCase(billingCycle) ? 12 : 1));

        subscriptionRepo.save(sub);
        log.info("Subscription activated for hospital {} on plan {}", hospitalId, plan.getName());
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (invoice == null) return;

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

        // Update subscription period
        if (invoice.getPeriodStart() != null) {
            sub.setCurrentPeriodStart(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(invoice.getPeriodStart()), ZoneId.systemDefault()));
        }
        if (invoice.getPeriodEnd() != null) {
            sub.setCurrentPeriodEnd(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(invoice.getPeriodEnd()), ZoneId.systemDefault()));
        }
        sub.setStatus(HospitalSubscription.Status.active);
        subscriptionRepo.save(sub);

        log.info("Payment succeeded for hospital {}: ${}", sub.getHospital().getId(), payment.getAmount());
    }

    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (invoice == null) return;

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
        Subscription stripeSub = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSub == null) return;

        Optional<HospitalSubscription> subOpt = subscriptionRepo.findByStripeSubscriptionId(stripeSub.getId());
        if (subOpt.isEmpty()) return;

        HospitalSubscription sub = subOpt.get();
        sub.setStatus(HospitalSubscription.Status.cancelled);
        sub.setCancelledAt(LocalDateTime.now());
        subscriptionRepo.save(sub);

        log.info("Subscription cancelled for hospital {}", sub.getHospital().getId());
    }

    private void handleSubscriptionUpdated(Event event) {
        Subscription stripeSub = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSub == null) return;

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

