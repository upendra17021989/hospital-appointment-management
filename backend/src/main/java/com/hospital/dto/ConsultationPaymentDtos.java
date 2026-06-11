package com.hospital.dto;

import com.hospital.model.ConsultationPayment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

public class ConsultationPaymentDtos {

    @Data
    public static class CreateConsultationPaymentRequest {
        @NotNull
        private UUID appointmentId;

        @NotNull
        @Positive
        private BigDecimal amountPaid;

        @NotNull
        private ConsultationPayment.PaymentMode paymentMode;

        // Nullable for CASH/UPI etc.
        private String paymentReference;

        // Allow client to pass if you don’t want server to derive from doctor.
        // We still store it in consultation_payment.snapshot via ConsultationReceipt.
        // If you want strict server-side fee, we can derive from appointment.doctor.consultationFee.
        @NotNull
        @Positive
        private BigDecimal consultationFee;

        // Optional override; recommended to use authenticated user.
        private String receivedByName;
    }

    @Data
    public static class CreateConsultationPaymentResponse {
        private UUID consultationPaymentId;
    }
}

