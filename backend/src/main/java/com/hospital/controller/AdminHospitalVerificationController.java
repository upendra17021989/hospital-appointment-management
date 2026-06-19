package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.Hospital;
import com.hospital.model.HospitalDocument;
import com.hospital.repository.HospitalDocumentRepo;
import com.hospital.repository.HospitalRepo;
import com.hospital.service.HospitalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/hospital-verifications")
@RequiredArgsConstructor
@Tag(name = "Admin Hospital Verification", description = "Super admin hospital KYC review APIs")
public class AdminHospitalVerificationController {

    private final HospitalRepo hospitalRepo;
    private final HospitalDocumentRepo documentRepo;
    private final HospitalDocumentService documentService;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HospitalVerificationResponse {
        private UUID id;
        private String name;
        private String slug;
        private String address;
        private String city;
        private String state;
        private String pincode;
        private String phone;
        private String email;
        private String website;
        private String licenseNumber;
        private String registrationNumber;
        private String gstNumber;
        private String panNumber;
        private String ownerDirectorName;
        private String verificationStatus;
        private String verificationNotes;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private List<HospitalDocumentService.HospitalDocumentResponse> documents;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRequest {
        private String status;
        private String notes;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List hospitals for verification review")
    public ResponseEntity<ApiResponse<List<HospitalVerificationResponse>>> list(
            @RequestParam(required = false) String status) {
        List<Hospital> hospitals = status == null || status.isBlank()
                ? hospitalRepo.findAllByOrderByCreatedAtDesc()
                : hospitalRepo.findByVerificationStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(hospitals.stream().map(this::toResponse).toList()));
    }

    @GetMapping("/{hospitalId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get hospital verification details")
    public ResponseEntity<ApiResponse<HospitalVerificationResponse>> get(@PathVariable UUID hospitalId) {
        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
        return ResponseEntity.ok(ApiResponse.success(toResponse(hospital)));
    }

    @PostMapping("/{hospitalId}/review")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve, reject, or reset hospital verification")
    public ResponseEntity<ApiResponse<HospitalVerificationResponse>> review(
            @PathVariable UUID hospitalId,
            @RequestBody ReviewRequest request) {
        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
        String status = normalizeStatus(request.getStatus());
        hospital.setVerificationStatus(status);
        hospital.setVerificationNotes(blankToNull(request.getNotes()));
        Hospital saved = hospitalRepo.save(hospital);
        return ResponseEntity.ok(ApiResponse.success("Hospital verification updated", toResponse(saved)));
    }

    @GetMapping("/{hospitalId}/documents/{documentId}/download")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Download hospital verification document as super admin")
    public ResponseEntity<?> downloadDocument(
            @PathVariable UUID hospitalId,
            @PathVariable UUID documentId) {
        var download = documentService.downloadForHospital(hospitalId, documentId);
        MediaType mediaType = download.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.getOriginalFilename().replace("\"", "") + "\"")
                .body(download.getResource());
    }

    private HospitalVerificationResponse toResponse(Hospital hospital) {
        return HospitalVerificationResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .slug(hospital.getSlug())
                .address(hospital.getAddress())
                .city(hospital.getCity())
                .state(hospital.getState())
                .pincode(hospital.getPincode())
                .phone(hospital.getPhone())
                .email(hospital.getEmail())
                .website(hospital.getWebsite())
                .licenseNumber(hospital.getLicenseNumber())
                .registrationNumber(hospital.getRegistrationNumber())
                .gstNumber(hospital.getGstNumber())
                .panNumber(hospital.getPanNumber())
                .ownerDirectorName(hospital.getOwnerDirectorName())
                .verificationStatus(hospital.getVerificationStatus())
                .verificationNotes(hospital.getVerificationNotes())
                .isActive(hospital.getIsActive())
                .createdAt(hospital.getCreatedAt())
                .documents(documentRepo.findByHospitalIdOrderByUploadedAtDesc(hospital.getId()).stream()
                        .map(this::toDocumentResponse)
                        .toList())
                .build();
    }

    private HospitalDocumentService.HospitalDocumentResponse toDocumentResponse(HospitalDocument document) {
        return HospitalDocumentService.HospitalDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!List.of("PENDING", "VERIFIED", "REJECTED").contains(normalized)) {
            throw new IllegalArgumentException("status must be PENDING, VERIFIED, or REJECTED");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
