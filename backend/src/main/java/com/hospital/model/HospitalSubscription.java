package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospital_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class HospitalSubscription {

    public enum Status {
        trial, active, past_due, cancelled, expired
    }

    public enum BillingCycle {
        monthly, yearly
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.trial;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", length = 10)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.monthly;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 100)
    private String stripeSubscriptionId;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public boolean isActiveOrTrial() {
        return status == Status.active || status == Status.trial;
    }

    @Transient
    public boolean isExpired() {
        if (status == Status.trial && trialEndsAt != null) {
            return LocalDateTime.now().isAfter(trialEndsAt);
        }
        if (status == Status.active && currentPeriodEnd != null) {
            return LocalDateTime.now().isAfter(currentPeriodEnd);
        }
        return status == Status.expired || status == Status.cancelled;
    }
}

