package com.hospital.repository;

import com.hospital.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepo extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByPatientId(UUID patientId);
    List<Appointment> findByDoctorId(UUID doctorId);
    List<Appointment> findByDoctorIdAndAppointmentDate(UUID doctorId, LocalDate date);
    List<Appointment> findByAppointmentDate(LocalDate date);
    List<Appointment> findByStatus(Appointment.Status status);

    // Multi-tenant
    List<Appointment> findByHospitalId(UUID hospitalId);
    List<Appointment> findByHospitalIdAndAppointmentDate(UUID hospitalId, LocalDate date);
    List<Appointment> findByHospitalIdAndStatus(UUID hospitalId, Appointment.Status status);
    List<Appointment> findByHospitalIdAndPatientId(UUID hospitalId, UUID patientId);
    List<Appointment> findByHospitalIdAndDoctorId(UUID hospitalId, UUID doctorId);

    // Robust tenant scoping (covers older rows where appointment.hospital might be null)
    @Query("SELECT a FROM Appointment a WHERE (a.hospital.id = :hospitalId OR a.doctor.hospital.id = :hospitalId) ORDER BY a.appointmentDate, a.appointmentTime")
    List<Appointment> findByHospitalOrDoctorHospitalId(@Param("hospitalId") UUID hospitalId);

    @Query("SELECT a FROM Appointment a WHERE (a.hospital.id = :hospitalId OR a.doctor.hospital.id = :hospitalId) AND a.appointmentDate = :date ORDER BY a.appointmentDate, a.appointmentTime")
    List<Appointment> findByHospitalOrDoctorHospitalIdAndAppointmentDate(@Param("hospitalId") UUID hospitalId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE (a.hospital.id = :hospitalId OR a.doctor.hospital.id = :hospitalId) AND a.status = :status ORDER BY a.appointmentDate, a.appointmentTime")
    List<Appointment> findByHospitalOrDoctorHospitalIdAndStatus(@Param("hospitalId") UUID hospitalId, @Param("status") Appointment.Status status);

    @Query("SELECT a FROM Appointment a WHERE (a.hospital.id = :hospitalId OR a.doctor.hospital.id = :hospitalId) AND a.patient.id = :patientId ORDER BY a.appointmentDate, a.appointmentTime")
    List<Appointment> findByHospitalOrDoctorHospitalIdAndPatientId(@Param("hospitalId") UUID hospitalId, @Param("patientId") UUID patientId);

    @Query("SELECT a FROM Appointment a WHERE (a.hospital.id = :hospitalId OR a.doctor.hospital.id = :hospitalId) AND a.doctor.id = :doctorId ORDER BY a.appointmentDate, a.appointmentTime")
    List<Appointment> findByHospitalOrDoctorHospitalIdAndDoctorId(@Param("hospitalId") UUID hospitalId, @Param("doctorId") UUID doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate BETWEEN :start AND :end ORDER BY a.appointmentDate, a.appointmentTime")
    List<Appointment> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.status <> 'cancelled'")
    Long countActiveAppointmentsForDoctorOnDate(@Param("doctorId") UUID doctorId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.hospital.id = :hospitalId AND a.appointmentDate BETWEEN :start AND :end")
    Long countByHospitalIdAndAppointmentDateBetween(@Param("hospitalId") UUID hospitalId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // Paged queries with robust tenant scoping
    @Query("SELECT a FROM Appointment a WHERE (a.hospital.id = :hospitalId OR a.doctor.hospital.id = :hospitalId)")
    Page<Appointment> findByHospitalOrDoctorHospitalIdPaged(@Param("hospitalId") UUID hospitalId, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE (a.hospital.id = :hospitalId OR a.doctor.hospital.id = :hospitalId) AND a.appointmentDate = :date")
    Page<Appointment> findByHospitalOrDoctorHospitalIdAndAppointmentDatePaged(@Param("hospitalId") UUID hospitalId, @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE (a.hospital.id = :hospitalId OR a.doctor.hospital.id = :hospitalId) AND a.status = :status")
    Page<Appointment> findByHospitalOrDoctorHospitalIdAndStatusPaged(@Param("hospitalId") UUID hospitalId, @Param("status") Appointment.Status status, Pageable pageable);
}
