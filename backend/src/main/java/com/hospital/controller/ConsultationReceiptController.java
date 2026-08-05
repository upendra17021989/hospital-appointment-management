package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.ConsultationReceiptDtos;
import com.hospital.model.ConsultationReceipt;
import com.hospital.model.ConsultationPayment;
import com.hospital.repository.ConsultationReceiptRepo;
import com.hospital.security.TenantContext;
import com.hospital.service.ConsultationReceiptPdfService;
import com.hospital.service.ConsultationReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/consultation-receipts")
@RequiredArgsConstructor
@Tag(name = "Consultation Receipts", description = "Consultation receipt printing APIs")
public class ConsultationReceiptController {

    private final ConsultationReceiptRepo receiptRepo;
    private final ConsultationReceiptService receiptService;
    private final ConsultationReceiptPdfService pdfService;
    private final TenantContext tenantContext;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','ACCOUNTANT','HOSPITAL_ADMIN','SUPER_ADMIN', 'DOCTOR')")
    @Operation(summary = "Search consultation receipts")
    public ResponseEntity<ApiResponse<?>> searchReceipts(
            @RequestParam(required = false) String receiptNumber,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String receiptStatus,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "receiptDateTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        ConsultationPayment.PaymentMode mode = null;
        if (paymentMode != null && !paymentMode.isBlank()) {
            mode = ConsultationPayment.PaymentMode.valueOf(paymentMode);
        }
        var req = ConsultationReceiptDtos.SearchReceiptsRequest.builder()
                .receiptNumber(receiptNumber)
                .patientName(patientName)
                .doctorName(doctorName)
                .departmentName(departmentName)
                .receiptStatus(receiptStatus)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        return ResponseEntity.ok(ApiResponse.success(receiptService.search(req, page, size, sortBy, sortDirection, mode)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','ACCOUNTANT','HOSPITAL_ADMIN','SUPER_ADMIN', 'DOCTOR')")
    @Operation(summary = "Receipt dashboard summary cards")
    public ResponseEntity<ApiResponse<ConsultationReceiptDtos.ReceiptDashboardStats>> dashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(receiptService.dashboardStats()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create consultation receipt from consultation payment")
    public ResponseEntity<ApiResponse<ConsultationReceiptDtos.ConsultationReceiptResponse>> createReceipt(
            @Valid @RequestBody ConsultationReceiptDtos.CreateConsultationReceiptRequest req) {
        ConsultationReceipt receipt = receiptService.createReceiptForPayment(req.getConsultationPaymentId());
        return ResponseEntity.ok(ApiResponse.success("Receipt saved successfully", receiptService.toResponse(receipt)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','ACCOUNTANT','HOSPITAL_ADMIN','SUPER_ADMIN', 'DOCTOR')")
    @Operation(summary = "View consultation receipt")
    public ResponseEntity<ApiResponse<ConsultationReceiptDtos.ConsultationReceiptResponse>> getReceipt(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.toResponse(receiptService.getReceipt(id))));
    }

    @GetMapping("/appointment/{appointmentId}/active")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','ACCOUNTANT','HOSPITAL_ADMIN','SUPER_ADMIN', 'DOCTOR')")
    @Operation(summary = "Get active consultation receipt for an appointment")
    public ResponseEntity<ApiResponse<ConsultationReceiptDtos.ConsultationReceiptResponse>> getActiveReceiptForAppointment(
            @PathVariable UUID appointmentId) {
        return receiptService.findActiveReceiptForAppointment(appointmentId)
                .map(receipt -> ResponseEntity.ok(ApiResponse.success(receiptService.toResponse(receipt))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update consultation receipt")
    public ResponseEntity<ApiResponse<ConsultationReceiptDtos.ConsultationReceiptResponse>> updateReceipt(
            @PathVariable UUID id,
            @Valid @RequestBody ConsultationReceiptDtos.UpdateConsultationReceiptRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Receipt updated successfully", receiptService.toResponse(receiptService.updateReceipt(id, req))));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Void consultation receipt")
    public ResponseEntity<ApiResponse<ConsultationReceiptDtos.VoidReceiptResponse>> voidReceipt(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Receipt voided successfully", receiptService.voidReceipt(id)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Download receipt PDF by receipt id")
    public ResponseEntity<byte[]> downloadPdfById(@PathVariable UUID id) {
        ConsultationReceipt receipt = receiptService.getReceipt(id);
        if ("VOIDED".equalsIgnoreCase(receipt.getReceiptStatus())) {
            throw new RuntimeException("Receipt is voided and cannot be reprinted");
        }
        byte[] pdf = pdfService.generatePdf(receipt);
        return pdfResponse(receipt.getReceiptNumber(), pdf);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','ACCOUNTANT','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Receipt history for patient profile")
    public ResponseEntity<ApiResponse<?>> patientReceiptHistory(@PathVariable UUID patientId) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.patientHistory(patientId)));
    }

    @GetMapping("/reports/collections")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Daily, payment mode wise, doctor wise, and date range collection report")
    public ResponseEntity<ApiResponse<ConsultationReceiptDtos.DateRangeReportResponse>> collectionReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.report(startDate, endDate)));
    }

    @GetMapping("/reports/collections/export")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Export collection report as CSV, Excel, or PDF")
    public ResponseEntity<byte[]> exportCollectionReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "csv") String format) {
        byte[] body = receiptService.export(startDate, endDate, format);
        String normalized = format == null ? "csv" : format.toLowerCase();
        MediaType mediaType = "pdf".equals(normalized)
                ? MediaType.APPLICATION_PDF
                : ("xlsx".equals(normalized) || "excel".equals(normalized)
                    ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    : MediaType.parseMediaType("text/csv"));
        String ext = "excel".equals(normalized) ? "xlsx" : normalized;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"consultation-collections." + ext + "\"")
                .body(body);
    }

    @PostMapping("/payment/{consultationPaymentId}/pdf")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create receipt (if not already created) and download PDF")
    public ResponseEntity<byte[]> downloadPdfForPayment(
            @PathVariable UUID consultationPaymentId) {

        // Create a new receipt record for this payment.
        ConsultationReceipt receipt = receiptService.createReceiptForPayment(consultationPaymentId);

        byte[] pdf = pdfService.generatePdf(receipt);

        return pdfResponse(receipt.getReceiptNumber(), pdf);
    }

    @GetMapping("/hospital/{receiptNumber}/pdf")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Reprint receipt by receipt number (PDF)")
    public ResponseEntity<byte[]> downloadPdfByReceiptNumber(
            @PathVariable String receiptNumber) {

        UUID hospitalId = tenantContext.requireHospitalId();
        ConsultationReceipt receipt = receiptRepo.findByHospitalIdAndReceiptNumber(hospitalId, receiptNumber)
                .orElseThrow(() -> new RuntimeException("Receipt not found"));

        // Prevent printing voided receipts
        if ("VOIDED".equalsIgnoreCase(receipt.getReceiptStatus())) {
            throw new RuntimeException("Receipt is voided and cannot be reprinted");
        }

        byte[] pdf = pdfService.generatePdf(receipt);

        return pdfResponse(receipt.getReceiptNumber(), pdf);
    }

    private ResponseEntity<byte[]> pdfResponse(String receiptNumber, byte[] pdf) {
        String filename = receiptNumber + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}


