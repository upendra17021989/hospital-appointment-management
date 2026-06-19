package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.MedicalCertificateDtos;
import com.hospital.model.MedicalCertificate;
import com.hospital.service.MedicalCertificatePdfService;
import com.hospital.service.MedicalCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/medical-certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Medical Certificates", description = "Dynamic medical certificate APIs")
public class MedicalCertificateController {

    private final MedicalCertificateService certificateService;
    private final MedicalCertificatePdfService pdfService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Search medical certificates")
    public ResponseEntity<ApiResponse<?>> search(
            @RequestParam(required = false) String certificateNumber,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) MedicalCertificate.CertificateType certificateType,
            @RequestParam(required = false) MedicalCertificate.CertificateStatus certificateStatus,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return ResponseEntity.ok(ApiResponse.success(certificateService.search(
                certificateNumber,
                patientName,
                doctorName,
                certificateType,
                certificateStatus,
                startDate,
                endDate,
                page,
                size,
                sortBy,
                sortDirection
        )));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create medical certificate")
    public ResponseEntity<ApiResponse<MedicalCertificateDtos.MedicalCertificateResponse>> create(
            @Valid @RequestBody MedicalCertificateDtos.CreateMedicalCertificateRequest req) {
        MedicalCertificate saved = certificateService.create(req);
        return ResponseEntity.ok(ApiResponse.success("Certificate created successfully", certificateService.toResponse(saved)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get medical certificate")
    public ResponseEntity<ApiResponse<MedicalCertificateDtos.MedicalCertificateResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(certificateService.toResponse(certificateService.getCertificate(id))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update medical certificate")
    public ResponseEntity<ApiResponse<MedicalCertificateDtos.MedicalCertificateResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MedicalCertificateDtos.UpdateMedicalCertificateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Certificate updated successfully", certificateService.toResponse(certificateService.update(id, req))));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Void medical certificate")
    public ResponseEntity<ApiResponse<MedicalCertificateDtos.VoidCertificateResponse>> voidCertificate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Certificate voided successfully", certificateService.voidCertificate(id)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Download medical certificate PDF")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        MedicalCertificate certificate = certificateService.getCertificate(id);
        if (MedicalCertificate.CertificateStatus.VOIDED.equals(certificate.getCertificateStatus())) {
            throw new IllegalArgumentException("Voided certificates cannot be printed");
        }
        byte[] pdf = pdfService.generatePdf(certificate);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + certificate.getCertificateNumber() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get certificate history for patient")
    public ResponseEntity<ApiResponse<?>> patientHistory(@PathVariable UUID patientId) {
        return ResponseEntity.ok(ApiResponse.success(certificateService.patientHistory(patientId)));
    }
}
