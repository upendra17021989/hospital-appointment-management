package com.hospital.repository;

import com.hospital.model.ConsultationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationReceiptRepo extends JpaRepository<ConsultationReceipt, UUID> {

    Optional<ConsultationReceipt> findByHospitalIdAndReceiptNumber(UUID hospitalId, String receiptNumber);

    List<ConsultationReceipt> findByHospitalIdAndConsultationPaymentIdOrderByReceiptDateTimeDesc(UUID hospitalId, UUID consultationPaymentId);
}

