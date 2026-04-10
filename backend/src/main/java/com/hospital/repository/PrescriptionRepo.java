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
    List<Prescription> findByHospitalIdAndPatientIdOrderByCreatedAtDesc(UUID hospitalId, UUID patientId);
    Optional<Prescription> findByAppointmentId(UUID appointmentId);
    Optional<Prescription> findByHospitalIdAndAppointmentId(UUID hospitalId, UUID appointmentId);

    @Query("SELECT p FROM Prescription p WHERE p.hospital.id = :hospitalId ORDER BY p.createdAt DESC")
    List<Prescription> findByHospitalIdDesc(@Param("hospitalId") UUID hospitalId);

    // Robust tenant scoping (covers older rows where prescription.hospital might be null)
    @Query("SELECT p FROM Prescription p WHERE " +
            "(p.hospital.id = :hospitalId OR p.doctor.hospital.id = :hospitalId OR p.patient.hospital.id = :hospitalId) " +
            "AND p.patient.id = :patientId ORDER BY p.createdAt DESC")
    List<Prescription> findByHospitalOrDoctorOrPatientHospitalIdAndPatientIdOrderByCreatedAtDesc(
            @Param("hospitalId") UUID hospitalId,
            @Param("patientId") UUID patientId);

    @Query("SELECT p FROM Prescription p WHERE " +
            "(p.hospital.id = :hospitalId OR p.doctor.hospital.id = :hospitalId OR p.patient.hospital.id = :hospitalId) " +
            "AND p.appointment.id = :appointmentId ORDER BY p.createdAt DESC")
    Optional<Prescription> findByHospitalOrDoctorOrPatientHospitalIdAndAppointmentId(
            @Param("hospitalId") UUID hospitalId,
            @Param("appointmentId") UUID appointmentId);

    @Query("SELECT p FROM Prescription p WHERE " +
            "(p.hospital.id = :hospitalId OR p.doctor.hospital.id = :hospitalId OR p.patient.hospital.id = :hospitalId) " +
            "AND p.id = :id")
    Optional<Prescription> findByHospitalOrDoctorOrPatientHospitalIdAndId(
            @Param("hospitalId") UUID hospitalId,
            @Param("id") UUID id);
}
