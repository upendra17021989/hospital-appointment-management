package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "consultation_payment_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationPaymentLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private ConsultationPayment payment;

    @Column(name = "sr_no", nullable = false)
    private Integer srNo;

    @Column(name = "particulars", nullable = false, length = 250)
    private String particulars;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
}

