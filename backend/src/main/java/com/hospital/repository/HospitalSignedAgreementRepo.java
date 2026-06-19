package com.hospital.repository;

import com.hospital.model.HospitalSignedAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalSignedAgreementRepo extends JpaRepository<HospitalSignedAgreement, UUID> {
    List<HospitalSignedAgreement> findByHospitalIdOrderByUploadedAtDesc(UUID hospitalId);
    Optional<HospitalSignedAgreement> findByIdAndHospitalId(UUID id, UUID hospitalId);
    List<HospitalSignedAgreement> findAllByOrderByUploadedAtDesc();
    List<HospitalSignedAgreement> findByReviewStatusOrderByUploadedAtDesc(HospitalSignedAgreement.ReviewStatus reviewStatus);
}
