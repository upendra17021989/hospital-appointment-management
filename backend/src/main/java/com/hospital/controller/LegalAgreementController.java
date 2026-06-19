package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.security.TenantContext;
import com.hospital.service.LegalAgreementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
public class LegalAgreementController {
    private final LegalAgreementService legalService;
    private final TenantContext tenantContext;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AcceptRequest {
        private String acceptanceText;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ReviewRequest {
        private String status;
        private String notes;
    }

    @GetMapping("/documents/active")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> activeDocuments() {
        return ResponseEntity.ok(ApiResponse.success(legalService.activeDocumentsForHospital()));
    }

    @PostMapping("/accept")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> acceptAll(@RequestBody AcceptRequest req, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success("Legal documents accepted", legalService.acceptAll(servletRequest, req.getAcceptanceText())));
    }

    @GetMapping("/acceptances")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> acceptances() {
        return ResponseEntity.ok(ApiResponse.success(legalService.acceptanceHistory()));
    }

    @GetMapping("/signed-agreements")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> signedAgreements() {
        return ResponseEntity.ok(ApiResponse.success(legalService.signedAgreementsForHospital()));
    }

    @PostMapping(value = "/signed-agreements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> uploadSignedAgreement(@RequestParam MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Signed agreement uploaded", legalService.uploadSignedAgreement(file)));
    }

    @GetMapping("/signed-agreements/{id}/download")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> downloadOwnSignedAgreement(@PathVariable UUID id) {
        var download = legalService.download(id, tenantContext.requireHospitalId());
        return fileResponse(download);
    }

    @GetMapping("/admin/signed-agreements")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> adminSignedAgreements(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(legalService.adminSignedAgreements(status)));
    }

    @PostMapping("/admin/signed-agreements/{id}/review")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> reviewSignedAgreement(@PathVariable UUID id, @RequestBody ReviewRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Signed agreement reviewed", legalService.reviewSignedAgreement(id, req.getStatus(), req.getNotes())));
    }

    @GetMapping("/admin/signed-agreements/{id}/download")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> adminDownloadSignedAgreement(@PathVariable UUID id) {
        return fileResponse(legalService.download(id, null));
    }

    private ResponseEntity<?> fileResponse(LegalAgreementService.AgreementDownload download) {
        MediaType mediaType = download.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(download.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.getOriginalFilename().replace("\"", "") + "\"")
                .body(download.getResource());
    }
}
