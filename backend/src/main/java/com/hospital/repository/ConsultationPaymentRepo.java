package com.hospital.repository;

import com.hospital.model.ConsultationPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsultationPaymentRepo extends JpaRepository<ConsultationPayment, UUID> {

    List<ConsultationPayment> findByHospitalIdAndPatientIdOrderByPaidAtDesc(UUID hospitalId, UUID patientId);

    boolean existsByHospitalIdAndId(UUID hospitalId, UUID id);
}

