package com.hospital.repository;

import com.hospital.model.ConsultationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ConsultationReceiptRepo extends JpaRepository<ConsultationReceipt, UUID> {

    Optional<ConsultationReceipt> findByHospitalIdAndReceiptNumber(UUID hospitalId, String receiptNumber);

    List<ConsultationReceipt> findByHospitalIdAndConsultationPaymentIdOrderByReceiptDateTimeDesc(UUID hospitalId, UUID consultationPaymentId);

    long countByHospitalIdAndReceiptNumberStartingWith(UUID hospitalId, String prefix);

    // Dashboard counts
    long countByHospitalIdAndReceiptStatus(UUID hospitalId, String receiptStatus);

    // Search / list (paged)
    org.springframework.data.domain.Page<ConsultationReceiptSearchProjection> findAllProjectedByHospitalIdAndReceiptStatus(
            UUID hospitalId,
            String receiptStatus,
            org.springframework.data.domain.Pageable pageable
    );

    // Date filtering helpers
    long countByHospitalIdAndReceiptDateTimeBetween(UUID hospitalId, LocalDateTime start, LocalDateTime end);

    // Patient-scoped history
    List<ConsultationReceipt> findByHospitalIdAndPatientIdentifierOrderByReceiptDateTimeDesc(UUID hospitalId, String patientIdentifier);
}




