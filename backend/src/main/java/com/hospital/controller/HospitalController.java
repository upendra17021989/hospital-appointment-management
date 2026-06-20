package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.Hospital;
import com.hospital.model.HospitalDocument;
import com.hospital.repository.HospitalRepo;
import com.hospital.security.TenantContext;
import com.hospital.service.HospitalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/hospital")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Hospital Profile", description = "Hospital profile and receipt header settings")
public class HospitalController {

    private final HospitalRepo hospitalRepo;
    private final TenantContext tenantContext;
    private final HospitalDocumentService documentService;

    @Value("${app.uploads.hospital-logos-dir:uploads/hospital-logos}")
    private String hospitalLogosDir;

    @Value("${app.uploads.hospital-qr-codes-dir:uploads/hospital-qr-codes}")
    private String hospitalQrCodesDir;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HospitalProfileResponse {
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
        private String logoUrl;
        private Boolean consultationReceiptHeaderEnabled;
        private String consultationReceiptQrCodeUrl;
        private String description;
        private String licenseNumber;
        private String registrationNumber;
        private String clinicalEstablishmentRegistrationNumber;
        private String municipalLicenseNumber;
        private String pharmacyLicenseNumber;
        private String laboratoryLicenseNumber;
        private String gstNumber;
        private String panNumber;
        private String ownerDirectorName;
        private String verificationStatus;
        private String verificationNotes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateHospitalProfileRequest {
        private String name;
        private String address;
        private String city;
        private String state;
        private String pincode;
        private String phone;
        private String email;
        private String website;
        private String logoUrl;
        private Boolean consultationReceiptHeaderEnabled;
        private String consultationReceiptQrCodeUrl;
        private String description;
        private String licenseNumber;
        private String registrationNumber;
        private String clinicalEstablishmentRegistrationNumber;
        private String municipalLicenseNumber;
        private String pharmacyLicenseNumber;
        private String laboratoryLicenseNumber;
        private String gstNumber;
        private String panNumber;
        private String ownerDirectorName;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get current hospital profile")
    public ResponseEntity<ApiResponse<HospitalProfileResponse>> getCurrentHospital() {
        return ResponseEntity.ok(ApiResponse.success(toResponse(currentHospital())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update current hospital profile and receipt header details")
    public ResponseEntity<ApiResponse<HospitalProfileResponse>> updateCurrentHospital(
            @RequestBody UpdateHospitalProfileRequest request) {
        Hospital hospital = currentHospital();

        if (request.getName() != null && !request.getName().isBlank()) hospital.setName(request.getName().trim());
        hospital.setAddress(blankToNull(request.getAddress()));
        hospital.setCity(blankToNull(request.getCity()));
        hospital.setState(blankToNull(request.getState()));
        hospital.setPincode(blankToNull(request.getPincode()));
        hospital.setPhone(blankToNull(request.getPhone()));
        hospital.setEmail(blankToNull(request.getEmail()));
        hospital.setWebsite(blankToNull(request.getWebsite()));
        hospital.setLogoUrl(blankToNull(request.getLogoUrl()));
        if (request.getConsultationReceiptHeaderEnabled() != null) {
            hospital.setConsultationReceiptHeaderEnabled(request.getConsultationReceiptHeaderEnabled());
        }
        hospital.setConsultationReceiptQrCodeUrl(blankToNull(request.getConsultationReceiptQrCodeUrl()));
        hospital.setDescription(blankToNull(request.getDescription()));
        hospital.setLicenseNumber(blankToNull(request.getLicenseNumber()));
        hospital.setRegistrationNumber(blankToNull(firstPresent(request.getRegistrationNumber(), request.getLicenseNumber())));
        hospital.setClinicalEstablishmentRegistrationNumber(blankToNull(request.getClinicalEstablishmentRegistrationNumber()));
        hospital.setMunicipalLicenseNumber(blankToNull(request.getMunicipalLicenseNumber()));
        hospital.setPharmacyLicenseNumber(blankToNull(request.getPharmacyLicenseNumber()));
        hospital.setLaboratoryLicenseNumber(blankToNull(request.getLaboratoryLicenseNumber()));
        hospital.setGstNumber(blankToNull(request.getGstNumber()));
        hospital.setPanNumber(blankToNull(request.getPanNumber()));
        hospital.setOwnerDirectorName(blankToNull(request.getOwnerDirectorName()));
        hospital.setVerificationStatus("PENDING");

        return ResponseEntity.ok(ApiResponse.success("Hospital profile updated", toResponse(hospitalRepo.save(hospital))));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Upload hospital logo for consultation receipt PDF")
    public ResponseEntity<ApiResponse<HospitalProfileResponse>> uploadLogo(@RequestParam MultipartFile file) {
        Hospital hospital = currentHospital();
        validateImage(file, "Logo image");

        try {
            Path target = storeHospitalImage(hospital, file, hospitalLogosDir, "logo");
            hospital.setLogoUrl(target.toString());

            return ResponseEntity.ok(ApiResponse.success("Logo uploaded", toResponse(hospitalRepo.save(hospital))));
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload logo: " + e.getMessage(), e);
        }
    }

    @PostMapping(value = "/consultation-qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Upload hospital QR code for consultation receipt PDF")
    public ResponseEntity<ApiResponse<HospitalProfileResponse>> uploadConsultationQr(@RequestParam MultipartFile file) {
        Hospital hospital = currentHospital();
        validateImage(file, "QR code image");

        try {
            Path target = storeHospitalImage(hospital, file, hospitalQrCodesDir, "qr");
            hospital.setConsultationReceiptQrCodeUrl(target.toString());

            return ResponseEntity.ok(ApiResponse.success("QR code uploaded", toResponse(hospitalRepo.save(hospital))));
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload QR code: " + e.getMessage(), e);
        }
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "List current hospital verification documents")
    public ResponseEntity<ApiResponse<?>> listDocuments() {
        return ResponseEntity.ok(ApiResponse.success(documentService.list()));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Upload a hospital verification document")
    public ResponseEntity<ApiResponse<HospitalDocumentService.HospitalDocumentResponse>> uploadDocument(
            @RequestParam HospitalDocument.DocumentType documentType,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Document uploaded", documentService.upload(documentType, file)));
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Download a hospital verification document")
    public ResponseEntity<?> downloadDocument(@PathVariable UUID id) {
        HospitalDocumentService.DocumentDownload download = documentService.download(id);
        MediaType mediaType = download.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.getOriginalFilename().replace("\"", "") + "\"")
                .body(download.getResource());
    }

    private Hospital currentHospital() {
        UUID hospitalId = tenantContext.requireHospitalId();
        return hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
    }

    private HospitalProfileResponse toResponse(Hospital hospital) {
        return HospitalProfileResponse.builder()
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
                .logoUrl(hospital.getLogoUrl())
                .consultationReceiptHeaderEnabled(hospital.getConsultationReceiptHeaderEnabled())
                .consultationReceiptQrCodeUrl(hospital.getConsultationReceiptQrCodeUrl())
                .description(hospital.getDescription())
                .licenseNumber(hospital.getLicenseNumber())
                .registrationNumber(hospital.getRegistrationNumber())
                .clinicalEstablishmentRegistrationNumber(hospital.getClinicalEstablishmentRegistrationNumber())
                .municipalLicenseNumber(hospital.getMunicipalLicenseNumber())
                .pharmacyLicenseNumber(hospital.getPharmacyLicenseNumber())
                .laboratoryLicenseNumber(hospital.getLaboratoryLicenseNumber())
                .gstNumber(hospital.getGstNumber())
                .panNumber(hospital.getPanNumber())
                .ownerDirectorName(hospital.getOwnerDirectorName())
                .verificationStatus(hospital.getVerificationStatus())
                .verificationNotes(hospital.getVerificationNotes())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private void validateImage(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        if (file.getSize() > 2L * 1024L * 1024L) {
            throw new IllegalArgumentException(label + " must be 2 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        }
    }

    private Path storeHospitalImage(Hospital hospital, MultipartFile file, String baseDir, String prefix) throws Exception {
        Path hospitalDir = Paths.get(baseDir).toAbsolutePath().normalize()
                .resolve(hospital.getId().toString())
                .normalize();
        Files.createDirectories(hospitalDir);

        String original = safeFilename(file.getOriginalFilename());
        String stored = prefix + "-" + UUID.randomUUID() + extension(original);
        Path target = hospitalDir.resolve(stored).normalize();
        if (!target.startsWith(hospitalDir)) {
            throw new IllegalArgumentException("Invalid upload path");
        }
        file.transferTo(target);
        return target;
    }

    private String safeFilename(String filename) {
        String value = filename == null || filename.isBlank() ? "logo" : filename;
        return Paths.get(value).getFileName().toString().replaceAll("[\\r\\n]", "").trim();
    }

    private String extension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx).replaceAll("[^A-Za-z0-9.]", "").toLowerCase();
    }
}
