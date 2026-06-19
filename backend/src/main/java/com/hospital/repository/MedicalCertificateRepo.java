package com.hospital.repository;

import com.hospital.model.MedicalCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalCertificateRepo extends JpaRepository<MedicalCertificate, UUID>, JpaSpecificationExecutor<MedicalCertificate> {
    Optional<MedicalCertificate> findByHospitalIdAndCertificateNumber(UUID hospitalId, String certificateNumber);

    long countByHospitalIdAndCertificateNumberStartingWith(UUID hospitalId, String prefix);

    List<MedicalCertificate> findByHospitalIdAndPatientIdOrderByCreatedAtDesc(UUID hospitalId, UUID patientId);
}
