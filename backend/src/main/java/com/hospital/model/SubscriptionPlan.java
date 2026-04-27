package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    @Column(name = "yearly_price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal yearlyPrice = BigDecimal.ZERO;

    @Column(name = "stripe_price_id_monthly", length = 100)
    private String stripePriceIdMonthly;

    @Column(name = "stripe_price_id_yearly", length = 100)
    private String stripePriceIdYearly;

    @Column(name = "max_doctors")
    @Builder.Default
    private Integer maxDoctors = 3;

    @Column(name = "max_users")
    @Builder.Default
    private Integer maxUsers = 2;

    @Column(name = "max_appointments_per_month")
    @Builder.Default
    private Integer maxAppointmentsPerMonth = 100;

    @Column(name = "allow_prescriptions")
    @Builder.Default
    private Boolean allowPrescriptions = false;

    @Column(name = "allow_sms")
    @Builder.Default
    private Boolean allowSms = false;

    @Column(name = "allow_whatsapp")
    @Builder.Default
    private Boolean allowWhatsapp = false;

    @Column(name = "allow_custom_branding")
    @Builder.Default
    private Boolean allowCustomBranding = false;

    @Column(name = "priority_support")
    @Builder.Default
    private Boolean prioritySupport = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

