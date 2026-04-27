package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.PaymentResponse;
import com.hospital.model.Payment;
import com.hospital.model.User;
import com.hospital.repository.PaymentRepo;
import com.hospital.service.PaymentService;
import com.stripe.model.Event;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment history and Stripe webhook endpoints")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepo paymentRepo;

    // ── GET /payments/history - Billing history for current hospital ──

    @GetMapping("/history")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get payment history for current hospital")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        if (hospitalId == null) {
            return ResponseEntity.ok(ApiResponse.error("No hospital associated with user"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> payments = paymentRepo.findByHospitalIdOrderByCreatedAtDesc(hospitalId, pageable);

        List<PaymentResponse> response = payments.getContent().stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── POST /payments/webhook - Stripe webhook handler ──

    @PostMapping("/webhook")
    @Operation(summary = "Receive Stripe webhook events")
    public ResponseEntity<String> handleWebhook(
            HttpServletRequest request,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        String payload;
        try {
            payload = request.getReader().lines().collect(java.util.stream.Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            log.error("Failed to read webhook payload", e);
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        try {
            Event event = com.stripe.net.Webhook.constructEvent(payload, sigHeader,
                    System.getProperty("stripe.webhook.secret", ""));
            paymentService.handleWebhook(event);
            return ResponseEntity.ok("Webhook processed");
        } catch (com.stripe.exception.SignatureVerificationException e) {
            log.error("Invalid Stripe signature", e);
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception e) {
            log.error("Webhook processing error", e);
            return ResponseEntity.status(500).body("Internal error");
        }
    }

    // ── Helper ────────────────────────────────────────────────────

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .description(payment.getDescription())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

