package com.hospital.dto;

import com.hospital.model.ConsultationPayment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ConsultationReceiptDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptLineItemDto {
        @NotBlank(message = "particulars is required")
        @Size(max = 250)
        private String particulars;

        @NotNull(message = "amount is required")
        private BigDecimal amount;
    }

    // ============ Create / Update ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateConsultationReceiptRequest {
        @NotNull(message = "consultationPaymentId is required")
        private UUID consultationPaymentId;

        // optional override fields; for now we snapshot from payment, but allow updating status/line items later
        private ConsultationPayment.PaymentMode paymentMode;

        private String paymentReference;

        @NotNull(message = "amountPaid is required")
        private BigDecimal amountPaid;

        @NotBlank(message = "receivedByName is required")
        @Size(max = 150)
        private String receivedByName;

        private List<ReceiptLineItemDto> lineItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateConsultationReceiptRequest {
        @Size(max = 120)
        private String paymentReference;

        @NotNull
        private BigDecimal amountPaid;

        @Size(max = 150)
        private String receivedByName;

        private List<ReceiptLineItemDto> lineItems;
    }

    // ============ View ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultationReceiptResponse {
        private UUID id;
        private String receiptNumber;
        private LocalDateTime receiptDateTime;

        private String patientName;
        private String doctorName;
        private String departmentName;

        private BigDecimal amountPaid;
        private ConsultationPayment.PaymentMode paymentMode;
        private String paymentReference;
        private String receivedByName;

        private String receiptStatus;
        private LocalDateTime voidedAt;
        private UUID voidedBy;

        private List<ReceiptLineItemDto> lineItems;
    }

    // ============ Search ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchReceiptsRequest {
        private String receiptNumber;
        private String patientName;
        private String doctorName;
        private String departmentName;

        private String receiptStatus; // ACTIVE | VOIDED

        private LocalDate startDate;
        private LocalDate endDate;
    }

    // ============ Void ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoidReceiptResponse {
        private UUID receiptId;
        private String receiptNumber;
        private String receiptStatus;
        private LocalDateTime voidedAt;
        private UUID voidedBy;
    }

    // ============ Dashboard / Reports ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptDashboardStats {
        private Long todayReceipts;
        private BigDecimal todayCollection;
        private Long pendingReceipts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCollectionRow {
        private LocalDate date;
        private BigDecimal totalCollected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentModeWiseRow {
        private ConsultationPayment.PaymentMode paymentMode;
        private BigDecimal totalCollected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorWiseRow {
        private String doctorName;
        private BigDecimal totalCollected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeReportResponse {
        private List<DailyCollectionRow> dailyCollection;
        private List<PaymentModeWiseRow> paymentModeWiseCollection;
        private List<DoctorWiseRow> doctorWiseCollection;
    }
}

