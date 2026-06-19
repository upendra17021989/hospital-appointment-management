package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "medical_certificates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_medical_certificates_hospital_number", columnNames = {"hospital_id", "certificate_number"})
        }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalCertificate {

    public enum CertificateType {
        SICK_LEAVE,
        FITNESS,
        FIT_TO_FLY,
        FORM_1A_DRIVING_LICENSE,
        VACCINATION,
        RECOVERY,
        CARETAKER_MEDICAL_LEAVE,
        CARA_ADOPTION_FITNESS
    }

    public enum CertificateStatus {
        ACTIVE,
        VOIDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "certificate_number", nullable = false, length = 60)
    private String certificateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 60)
    private CertificateType certificateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_status", nullable = false, length = 30)
    @Builder.Default
    private CertificateStatus certificateStatus = CertificateStatus.ACTIVE;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "diagnosis_or_reason", columnDefinition = "TEXT")
    private String diagnosisOrReason;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "dynamic_fields", columnDefinition = "TEXT")
    private String dynamicFields;

    @Column(name = "patient_name", nullable = false, length = 200)
    private String patientName;

    @Column(name = "doctor_name", nullable = false, length = 200)
    private String doctorName;

    @Column(name = "department_name", length = 200)
    private String departmentName;

    @Column(name = "hospital_name", nullable = false, length = 150)
    private String hospitalName;

    @Column(name = "hospital_address", columnDefinition = "TEXT")
    private String hospitalAddress;

    @Column(name = "hospital_phone", length = 25)
    private String hospitalPhone;

    @Column(name = "issued_by_name", length = 150)
    private String issuedByName;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "voided_by")
    private UUID voidedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
