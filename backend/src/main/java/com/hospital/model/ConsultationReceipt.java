package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Entity
@Table(
        name = "consultation_receipts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_consultation_receipts_hospital_receipt_number", columnNames = {"hospital_id", "receipt_number"})
        }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_payment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ConsultationPayment consultationPayment;

    @Column(name = "receipt_number", nullable = false, length = 60)
    private String receiptNumber;

    @Column(name = "receipt_date_time", nullable = false)
    private LocalDateTime receiptDateTime;

    // Snapshot fields
    @Column(name = "hospital_name", nullable = false, length = 150)
    private String hospitalName;

    @Column(name = "hospital_address", columnDefinition = "TEXT")
    private String hospitalAddress;

    @Column(name = "hospital_phone", length = 25)
    private String hospitalPhone;

    @Column(name = "patient_name", nullable = false, length = 200)
    private String patientName;

    @Column(name = "patient_identifier", nullable = false, length = 120)
    private String patientIdentifier;

    @Column(name = "doctor_name", nullable = false, length = 200)
    private String doctorName;

    @Column(name = "department_name", nullable = false, length = 200)
    private String departmentName;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 30)
    private ConsultationPayment.PaymentMode paymentMode;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "received_by_name", nullable = false, length = 150)
    private String receivedByName;

    @Column(name = "stamp_placeholder", length = 200)
    private String stampPlaceholder;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("srNo ASC")
    private List<ConsultationReceiptLineItem> lineItems;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}


