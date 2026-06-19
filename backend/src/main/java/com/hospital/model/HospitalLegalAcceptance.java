package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospital_legal_acceptances", uniqueConstraints = {
        @UniqueConstraint(name = "uq_hospital_legal_acceptance_version", columnNames = {"hospital_id", "document_type", "document_version"})
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HospitalLegalAcceptance {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_document_id", nullable = false)
    private LegalDocument legalDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 60)
    private LegalDocument.DocumentType documentType;

    @Column(name = "document_version", nullable = false, length = 40)
    private String documentVersion;

    @Column(name = "accepted_by_user_id")
    private UUID acceptedByUserId;

    @Column(name = "accepted_by_name", length = 150)
    private String acceptedByName;

    @Column(name = "accepted_by_email", length = 150)
    private String acceptedByEmail;

    @Column(name = "acceptance_text", columnDefinition = "TEXT")
    private String acceptanceText;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "subscription_plan", length = 100)
    private String subscriptionPlan;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "billing_cycle", length = 40)
    private String billingCycle;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
}
