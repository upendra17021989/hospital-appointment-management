package com.hospital.repository;

import com.hospital.model.ConsultationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ConsultationReceiptRepo extends JpaRepository<ConsultationReceipt, UUID>, JpaSpecificationExecutor<ConsultationReceipt> {

    Optional<ConsultationReceipt> findByHospitalIdAndReceiptNumber(UUID hospitalId, String receiptNumber);

    List<ConsultationReceipt> findByHospitalIdAndConsultationPaymentIdOrderByReceiptDateTimeDesc(UUID hospitalId, UUID consultationPaymentId);

    @Query("""
            SELECT r FROM ConsultationReceipt r
            WHERE r.hospital.id = :hospitalId
              AND r.consultationPayment.id = :consultationPaymentId
              AND UPPER(r.receiptStatus) <> 'VOIDED'
            ORDER BY r.receiptDateTime DESC
            """)
    List<ConsultationReceipt> findActiveByHospitalIdAndConsultationPaymentId(
            @Param("hospitalId") UUID hospitalId,
            @Param("consultationPaymentId") UUID consultationPaymentId
    );

    @Query("""
            SELECT r FROM ConsultationReceipt r
            WHERE r.hospital.id = :hospitalId
              AND r.consultationPayment.appointment.id = :appointmentId
              AND UPPER(r.receiptStatus) <> 'VOIDED'
            ORDER BY r.receiptDateTime DESC
            """)
    List<ConsultationReceipt> findActiveByHospitalIdAndAppointmentId(
            @Param("hospitalId") UUID hospitalId,
            @Param("appointmentId") UUID appointmentId
    );

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

    @Query("""
            SELECT COALESCE(SUM(r.amountPaid), 0) FROM ConsultationReceipt r
            WHERE r.hospital.id = :hospitalId
              AND r.receiptStatus = 'ACTIVE'
              AND r.receiptDateTime BETWEEN :start AND :end
            """)
    BigDecimal sumActiveCollectionBetween(@Param("hospitalId") UUID hospitalId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(r) FROM ConsultationReceipt r
            WHERE r.hospital.id = :hospitalId
              AND r.receiptStatus = 'ACTIVE'
              AND r.receiptDateTime BETWEEN :start AND :end
            """)
    long countActiveReceiptsBetween(@Param("hospitalId") UUID hospitalId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT DATE(r.receipt_date_time) AS day, COALESCE(SUM(r.amount_paid), 0) AS totalCollected
            FROM consultation_receipts r
            WHERE r.hospital_id = :hospitalId
              AND r.receipt_status = 'ACTIVE'
              AND r.receipt_date_time BETWEEN :start AND :end
            GROUP BY DATE(r.receipt_date_time)
            ORDER BY DATE(r.receipt_date_time)
            """, nativeQuery = true)
    List<ConsultationReceiptAggregates.DailyCollectionAgg> dailyCollection(
            @Param("hospitalId") UUID hospitalId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = """
            SELECT r.payment_mode AS paymentMode, COALESCE(SUM(r.amount_paid), 0) AS totalCollected
            FROM consultation_receipts r
            WHERE r.hospital_id = :hospitalId
              AND r.receipt_status = 'ACTIVE'
              AND r.receipt_date_time BETWEEN :start AND :end
            GROUP BY r.payment_mode
            ORDER BY r.payment_mode
            """, nativeQuery = true)
    List<ConsultationReceiptAggregates.PaymentModeWiseAgg> paymentModeWiseCollection(
            @Param("hospitalId") UUID hospitalId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = """
            SELECT r.doctor_name AS doctorName, COALESCE(SUM(r.amount_paid), 0) AS totalCollected
            FROM consultation_receipts r
            WHERE r.hospital_id = :hospitalId
              AND r.receipt_status = 'ACTIVE'
              AND r.receipt_date_time BETWEEN :start AND :end
            GROUP BY r.doctor_name
            ORDER BY COALESCE(SUM(r.amount_paid), 0) DESC
            """, nativeQuery = true)
    List<ConsultationReceiptAggregates.DoctorWiseAgg> doctorWiseCollection(
            @Param("hospitalId") UUID hospitalId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
