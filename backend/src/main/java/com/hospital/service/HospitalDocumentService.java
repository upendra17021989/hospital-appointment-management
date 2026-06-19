package com.hospital.service;

import com.hospital.model.Hospital;
import com.hospital.model.HospitalDocument;
import com.hospital.repository.HospitalDocumentRepo;
import com.hospital.repository.HospitalRepo;
import com.hospital.security.TenantContext;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HospitalDocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private final HospitalDocumentRepo documentRepo;
    private final HospitalRepo hospitalRepo;
    private final TenantContext tenantContext;

    @Value("${app.uploads.hospital-documents-dir:uploads/hospital-documents}")
    private String documentsDir;

    @Data
    @Builder
    public static class HospitalDocumentResponse {
        private UUID id;
        private HospitalDocument.DocumentType documentType;
        private String originalFilename;
        private String contentType;
        private Long fileSize;
        private java.time.LocalDateTime uploadedAt;
    }

    @Data
    @Builder
    public static class DocumentDownload {
        private Resource resource;
        private String originalFilename;
        private String contentType;
    }

    @Transactional
    public HospitalDocumentResponse upload(HospitalDocument.DocumentType documentType, MultipartFile file) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
        validateFile(file);

        try {
            Path hospitalDir = Paths.get(documentsDir).toAbsolutePath().normalize().resolve(hospitalId.toString()).normalize();
            Files.createDirectories(hospitalDir);

            String original = safeFilename(file.getOriginalFilename());
            String extension = extension(original);
            String stored = documentType.name().toLowerCase() + "-" + UUID.randomUUID() + extension;
            Path target = hospitalDir.resolve(stored).normalize();
            if (!target.startsWith(hospitalDir)) {
                throw new IllegalArgumentException("Invalid file path");
            }
            file.transferTo(target);

            HospitalDocument saved = documentRepo.save(HospitalDocument.builder()
                    .hospital(hospital)
                    .documentType(documentType)
                    .originalFilename(original)
                    .storedFilename(stored)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(target.toString())
                    .build());
            hospital.setVerificationStatus("PENDING");
            hospitalRepo.save(hospital);
            return toResponse(saved);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<HospitalDocumentResponse> list() {
        UUID hospitalId = tenantContext.requireHospitalId();
        return documentRepo.findByHospitalIdOrderByUploadedAtDesc(hospitalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(UUID documentId) {
        UUID hospitalId = tenantContext.requireHospitalId();
        HospitalDocument document = documentRepo.findByIdAndHospitalId(documentId, hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        try {
            Path path = Paths.get(document.getStoragePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Document file is not available");
            }
            return DocumentDownload.builder()
                    .resource(resource)
                    .originalFilename(document.getOriginalFilename())
                    .contentType(document.getContentType())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read document: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Document file must be 10 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("application/pdf")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Only PDF, JPEG, PNG, and WebP documents are allowed");
        }
    }

    private String safeFilename(String filename) {
        String value = filename == null || filename.isBlank() ? "document" : filename;
        return Paths.get(value).getFileName().toString().replaceAll("[\\r\\n]", "").trim();
    }

    private String extension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx).replaceAll("[^A-Za-z0-9.]", "").toLowerCase();
    }

    private HospitalDocumentResponse toResponse(HospitalDocument document) {
        return HospitalDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
