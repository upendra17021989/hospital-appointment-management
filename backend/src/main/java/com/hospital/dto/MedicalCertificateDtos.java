package com.hospital.dto;

import com.hospital.model.MedicalCertificate;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class MedicalCertificateDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateMedicalCertificateRequest {
        @NotNull private UUID patientId;
        @NotNull private UUID doctorId;
        private UUID appointmentId;
        @NotNull private MedicalCertificate.CertificateType certificateType;
        private LocalDate issueDate;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private String diagnosisOrReason;
        private String remarks;
        private Map<String, Object> dynamicFields;
        private String issuedByName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateMedicalCertificateRequest {
        private LocalDate issueDate;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private String diagnosisOrReason;
        private String remarks;
        private Map<String, Object> dynamicFields;
        private String issuedByName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalCertificateResponse {
        private UUID id;
        private String certificateNumber;
        private MedicalCertificate.CertificateType certificateType;
        private MedicalCertificate.CertificateStatus certificateStatus;
        private LocalDate issueDate;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private String diagnosisOrReason;
        private String remarks;
        private Map<String, Object> dynamicFields;
        private UUID patientId;
        private String patientName;
        private Integer patientAge;
        private String patientGender;
        private String patientPhone;
        private UUID doctorId;
        private String doctorName;
        private String doctorQualification;
        private String doctorSpecialization;
        private UUID appointmentId;
        private String departmentName;
        private String hospitalName;
        private String hospitalAddress;
        private String hospitalPhone;
        private String issuedByName;
        private LocalDateTime voidedAt;
        private UUID voidedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoidCertificateResponse {
        private UUID certificateId;
        private String certificateNumber;
        private MedicalCertificate.CertificateStatus certificateStatus;
        private LocalDateTime voidedAt;
        private UUID voidedBy;
    }
}
