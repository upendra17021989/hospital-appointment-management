package com.hospital.repository;

import com.hospital.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientRepo extends JpaRepository<Patient, UUID> {
    List<Patient> findByPhone(String phone);
    List<Patient> findByEmail(String email);
    List<Patient> findByHospitalId(UUID hospitalId);
    List<Patient> findByHospitalIdAndPhone(UUID hospitalId, String phone);
}
