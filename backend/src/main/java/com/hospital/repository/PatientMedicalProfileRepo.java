package com.hospital.repository;

import com.hospital.model.PatientMedicalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientMedicalProfileRepo extends JpaRepository<PatientMedicalProfile, UUID> {
    Optional<PatientMedicalProfile> findByPatient_Id(UUID patientId);
}
