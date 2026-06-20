package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospitals")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String website;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "consultation_receipt_header_enabled")
    @Builder.Default
    private Boolean consultationReceiptHeaderEnabled = true;

    @Column(name = "consultation_receipt_qr_code_url", columnDefinition = "TEXT")
    private String consultationReceiptQrCodeUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "clinical_establishment_registration_number", length = 100)
    private String clinicalEstablishmentRegistrationNumber;

    @Column(name = "municipal_license_number", length = 100)
    private String municipalLicenseNumber;

    @Column(name = "pharmacy_license_number", length = 100)
    private String pharmacyLicenseNumber;

    @Column(name = "laboratory_license_number", length = 100)
    private String laboratoryLicenseNumber;

    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "owner_director_name", length = 150)
    private String ownerDirectorName;

    @Column(name = "verification_status", length = 30)
    @Builder.Default
    private String verificationStatus = "PENDING";

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public boolean hasActiveSubscription() {
        // Will be populated by service layer
        return true;
    }
}
