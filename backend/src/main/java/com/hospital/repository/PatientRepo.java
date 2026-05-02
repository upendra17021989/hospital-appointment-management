package com.hospital.repository;

import com.hospital.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientRepo extends JpaRepository<Patient, UUID> {
    List<Patient> findByPhone(String phone);
    List<Patient> findByEmail(String email);
List<Patient> findByHospitalId(UUID hospitalId);
    List<Patient> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);
    List<Patient> findByHospitalIdAndPhone(UUID hospitalId, String phone);
    List<Patient> findByHospitalIdAndEmail(UUID hospitalId, String email);

    @Query("SELECT p FROM Patient p WHERE p.hospital.id = :hospitalId " +
            "AND (LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.address) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Patient> searchByHospitalId(@Param("hospitalId") UUID hospitalId, @Param("query") String query);
}
