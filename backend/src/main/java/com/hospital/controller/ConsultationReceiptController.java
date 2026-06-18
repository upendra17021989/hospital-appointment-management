package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.ConsultationReceipt;
import com.hospital.repository.ConsultationReceiptRepo;
import com.hospital.security.TenantContext;
import com.hospital.service.ConsultationReceiptNumberService;
import com.hospital.service.ConsultationReceiptPdfService;
import com.hospital.service.ConsultationReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/consultation-receipts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Consultation Receipts", description = "Consultation receipt printing APIs")
public class ConsultationReceiptController {

    private final ConsultationReceiptRepo receiptRepo;
    private final ConsultationReceiptService receiptService;
    private final ConsultationReceiptPdfService pdfService;
    private final TenantContext tenantContext;

    @PostMapping("/payment/{consultationPaymentId}/pdf")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create receipt (if not already created) and download PDF")
    public ResponseEntity<byte[]> downloadPdfForPayment(
            @PathVariable UUID consultationPaymentId) {

        // Create a new receipt record for this payment.
        ConsultationReceipt receipt = receiptService.createReceiptForPayment(consultationPaymentId);

        byte[] pdf = pdfService.generatePdf(receipt);

        String filename = receipt.getReceiptNumber() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
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

        String filename = receipt.getReceiptNumber() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}



