package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

// ── Prescription Medicine ─────────────────────────────────────
@Entity
@Table(name = "prescription_medicines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionMedicine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Prescription prescription;

    @Column(name = "medicine_name", nullable = false, length = 200)
    private String medicineName;

    @Column(length = 100)
    private String dosage;           // e.g. 500mg

    @Column(length = 100)
    private String frequency;        // e.g. Twice daily

    @Column(length = 100)
    private String duration;         // e.g. 7 days

    @Column(length = 50)
    private String route;            // Oral / Injection / Topical / Inhaler

    @Column(name = "before_food")
    @Builder.Default
    private Boolean beforeFood = false;

    @Column(length = 200)
    private String instructions;     // Special instructions

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
