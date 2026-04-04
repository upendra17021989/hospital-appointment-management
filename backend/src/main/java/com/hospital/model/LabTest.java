package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "lab_tests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Prescription prescription;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    @Column(length = 200)
    private String instructions;

    @Column(name = "is_urgent")
    @Builder.Default
    private Boolean isUrgent = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
