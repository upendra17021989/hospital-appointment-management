package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LegalAgreementService {
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private final LegalDocumentRepo legalDocumentRepo;
    private final HospitalLegalAcceptanceRepo acceptanceRepo;
    private final HospitalSignedAgreementRepo signedAgreementRepo;
    private final HospitalRepo hospitalRepo;
    private final HospitalSubscriptionRepo subscriptionRepo;
    private final TenantContext tenantContext;

    @Value("${app.uploads.signed-agreements-dir:uploads/signed-agreements}")
    private String signedAgreementsDir;

    @Data @Builder public static class LegalDocumentResponse {
        private UUID id;
        private LegalDocument.DocumentType documentType;
        private String title;
        private String version;
        private java.time.LocalDate effectiveDate;
        private String content;
        private Boolean accepted;
    }

    @Data @Builder public static class AcceptanceResponse {
        private UUID id;
        private LegalDocument.DocumentType documentType;
        private String documentVersion;
        private String acceptedByName;
        private String acceptedByEmail;
        private LocalDateTime acceptedAt;
        private String subscriptionPlan;
        private Integer maxUsers;
        private String billingCycle;
    }

    @Data @Builder public static class SignedAgreementResponse {
        private UUID id;
        private UUID hospitalId;
        private String hospitalName;
        private LegalDocument.DocumentType agreementType;
        private String originalFilename;
        private String contentType;
        private Long fileSize;
        private HospitalSignedAgreement.ReviewStatus reviewStatus;
        private String reviewNotes;
        private LocalDateTime reviewedAt;
        private UUID reviewedBy;
        private LocalDateTime uploadedAt;
    }

    @Data @Builder public static class AgreementDownload {
        private Resource resource;
        private String originalFilename;
        private String contentType;
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> activeDocumentsForHospital() {
        UUID hospitalId = tenantContext.requireHospitalId();
        return legalDocumentRepo.findByIsActiveTrueOrderByDocumentTypeAscEffectiveDateDesc()
                .stream()
                .map(doc -> toDocumentResponse(doc, isAccepted(hospitalId, doc)))
                .toList();
    }

    @Transactional
    public List<AcceptanceResponse> acceptAll(HttpServletRequest request, String acceptanceText) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Hospital hospital = hospitalRepo.findById(hospitalId).orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
        User user = tenantContext.getCurrentUser().orElseThrow(() -> new IllegalArgumentException("User not found"));
        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId).orElse(null);
        var docs = legalDocumentRepo.findByIsActiveTrueOrderByDocumentTypeAscEffectiveDateDesc();

        for (LegalDocument doc : docs) {
            acceptanceRepo.findByHospitalIdAndDocumentTypeAndDocumentVersion(hospitalId, doc.getDocumentType(), doc.getVersion())
                    .orElseGet(() -> acceptanceRepo.save(HospitalLegalAcceptance.builder()
                            .hospital(hospital)
                            .legalDocument(doc)
                            .documentType(doc.getDocumentType())
                            .documentVersion(doc.getVersion())
                            .acceptedByUserId(user.getId())
                            .acceptedByName(user.getFullName())
                            .acceptedByEmail(user.getEmail())
                            .acceptanceText(acceptanceText)
                            .ipAddress(request.getRemoteAddr())
                            .userAgent(request.getHeader("User-Agent"))
                            .subscriptionPlan(sub != null && sub.getPlan() != null ? sub.getPlan().getName() : null)
                            .maxUsers(sub != null && sub.getPlan() != null ? sub.getPlan().getMaxUsers() : null)
                            .billingCycle(sub != null && sub.getBillingCycle() != null ? sub.getBillingCycle().name() : null)
                            .acceptedAt(LocalDateTime.now())
                            .build()));
        }
        return acceptanceHistory();
    }

    @Transactional(readOnly = true)
    public List<AcceptanceResponse> acceptanceHistory() {
        UUID hospitalId = tenantContext.requireHospitalId();
        return acceptanceRepo.findByHospitalIdOrderByAcceptedAtDesc(hospitalId).stream().map(this::toAcceptanceResponse).toList();
    }

    @Transactional
    public SignedAgreementResponse uploadSignedAgreement(MultipartFile file) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Hospital hospital = hospitalRepo.findById(hospitalId).orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
        validateFile(file);
        try {
            Path hospitalDir = Paths.get(signedAgreementsDir).toAbsolutePath().normalize().resolve(hospitalId.toString()).normalize();
            Files.createDirectories(hospitalDir);
            String original = safeFilename(file.getOriginalFilename());
            String stored = "service-agreement-" + UUID.randomUUID() + extension(original);
            Path target = hospitalDir.resolve(stored).normalize();
            if (!target.startsWith(hospitalDir)) throw new IllegalArgumentException("Invalid file path");
            file.transferTo(target);
            var saved = signedAgreementRepo.save(HospitalSignedAgreement.builder()
                    .hospital(hospital)
                    .agreementType(LegalDocument.DocumentType.SERVICE_AGREEMENT)
                    .originalFilename(original)
                    .storedFilename(stored)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(target.toString())
                    .reviewStatus(HospitalSignedAgreement.ReviewStatus.PENDING)
                    .build());
            return toSignedResponse(saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload signed agreement: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<SignedAgreementResponse> signedAgreementsForHospital() {
        UUID hospitalId = tenantContext.requireHospitalId();
        return signedAgreementRepo.findByHospitalIdOrderByUploadedAtDesc(hospitalId).stream().map(this::toSignedResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SignedAgreementResponse> adminSignedAgreements(String status) {
        if (status != null && !status.isBlank()) {
            return signedAgreementRepo.findByReviewStatusOrderByUploadedAtDesc(HospitalSignedAgreement.ReviewStatus.valueOf(status.trim().toUpperCase()))
                    .stream().map(this::toSignedResponse).toList();
        }
        return signedAgreementRepo.findAllByOrderByUploadedAtDesc().stream().map(this::toSignedResponse).toList();
    }

    @Transactional
    public SignedAgreementResponse reviewSignedAgreement(UUID id, String status, String notes) {
        var agreement = signedAgreementRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Signed agreement not found"));
        agreement.setReviewStatus(HospitalSignedAgreement.ReviewStatus.valueOf(status.trim().toUpperCase()));
        agreement.setReviewNotes(blankToNull(notes));
        agreement.setReviewedAt(LocalDateTime.now());
        agreement.setReviewedBy(tenantContext.getCurrentUser().map(User::getId).orElse(null));
        return toSignedResponse(signedAgreementRepo.save(agreement));
    }

    @Transactional(readOnly = true)
    public AgreementDownload download(UUID id, UUID hospitalIdOrNull) {
        HospitalSignedAgreement agreement = hospitalIdOrNull == null
                ? signedAgreementRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Signed agreement not found"))
                : signedAgreementRepo.findByIdAndHospitalId(id, hospitalIdOrNull).orElseThrow(() -> new IllegalArgumentException("Signed agreement not found"));
        try {
            Resource resource = new UrlResource(Paths.get(agreement.getStoragePath()).toAbsolutePath().normalize().toUri());
            if (!resource.exists() || !resource.isReadable()) throw new IllegalArgumentException("Agreement file is not available");
            return AgreementDownload.builder().resource(resource).originalFilename(agreement.getOriginalFilename()).contentType(agreement.getContentType()).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read agreement: " + e.getMessage(), e);
        }
    }

    private boolean isAccepted(UUID hospitalId, LegalDocument doc) {
        return acceptanceRepo.findByHospitalIdAndDocumentTypeAndDocumentVersion(hospitalId, doc.getDocumentType(), doc.getVersion()).isPresent();
    }

    private LegalDocumentResponse toDocumentResponse(LegalDocument doc, boolean accepted) {
        return LegalDocumentResponse.builder().id(doc.getId()).documentType(doc.getDocumentType()).title(doc.getTitle()).version(doc.getVersion()).effectiveDate(doc.getEffectiveDate()).content(doc.getContent()).accepted(accepted).build();
    }

    private AcceptanceResponse toAcceptanceResponse(HospitalLegalAcceptance a) {
        return AcceptanceResponse.builder().id(a.getId()).documentType(a.getDocumentType()).documentVersion(a.getDocumentVersion()).acceptedByName(a.getAcceptedByName()).acceptedByEmail(a.getAcceptedByEmail()).acceptedAt(a.getAcceptedAt()).subscriptionPlan(a.getSubscriptionPlan()).maxUsers(a.getMaxUsers()).billingCycle(a.getBillingCycle()).build();
    }

    private SignedAgreementResponse toSignedResponse(HospitalSignedAgreement a) {
        Hospital h = a.getHospital();
        return SignedAgreementResponse.builder().id(a.getId()).hospitalId(h != null ? h.getId() : null).hospitalName(h != null ? h.getName() : null).agreementType(a.getAgreementType()).originalFilename(a.getOriginalFilename()).contentType(a.getContentType()).fileSize(a.getFileSize()).reviewStatus(a.getReviewStatus()).reviewNotes(a.getReviewNotes()).reviewedAt(a.getReviewedAt()).reviewedBy(a.getReviewedBy()).uploadedAt(a.getUploadedAt()).build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Signed agreement PDF is required");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Signed agreement must be 10 MB or smaller");
        if (!"application/pdf".equals(file.getContentType())) throw new IllegalArgumentException("Only PDF signed agreements are allowed");
    }

    private String safeFilename(String filename) {
        String value = filename == null || filename.isBlank() ? "signed-agreement.pdf" : filename;
        return Paths.get(value).getFileName().toString().replaceAll("[\\r\\n]", "").trim();
    }

    private String extension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? ".pdf" : filename.substring(idx).replaceAll("[^A-Za-z0-9.]", "").toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
