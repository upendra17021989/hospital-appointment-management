package com.hospital.repository;

import com.hospital.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepo extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<Prescription> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);
    List<Prescription> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);
    Optional<Prescription> findByAppointmentId(UUID appointmentId);

    @Query("SELECT p FROM Prescription p WHERE p.hospital.id = :hospitalId ORDER BY p.createdAt DESC")
    List<Prescription> findByHospitalIdDesc(@Param("hospitalId") UUID hospitalId);
}
