package com.hospital.repository;

import com.hospital.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepo extends JpaRepository<Doctor, UUID> {
    // Legacy
    List<Doctor> findByIsAvailableTrue();
    List<Doctor> findByDepartmentIdAndIsAvailableTrue(UUID departmentId);

    // Multi-tenant
    List<Doctor> findByHospitalIdAndIsAvailableTrue(UUID hospitalId);
    List<Doctor> findByHospitalIdAndDepartmentIdAndIsAvailableTrue(UUID hospitalId, UUID departmentId);
    Optional<Doctor> findByIdAndHospitalId(UUID id, UUID hospitalId);
    List<Doctor> findByDepartmentHospitalIdAndIsAvailableTrue(UUID hospitalId);
    List<Doctor> findByDepartmentHospitalIdAndDepartmentIdAndIsAvailableTrue(UUID hospitalId, UUID departmentId);

    @Query("SELECT d FROM Doctor d WHERE d.isAvailable = TRUE AND " +
           "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(d.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(d.specialization) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Doctor> searchDoctors(@Param("q") String query);

    @Query("SELECT d FROM Doctor d WHERE d.hospital.id = :hospitalId AND d.isAvailable = TRUE AND " +
           "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(d.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(d.specialization) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Doctor> searchDoctorsByHospital(@Param("hospitalId") UUID hospitalId, @Param("q") String query);

    @Query("SELECT d FROM Doctor d WHERE d.department.hospital.id = :hospitalId AND d.isAvailable = TRUE AND " +
           "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(d.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(d.specialization) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Doctor> searchDoctorsByDepartmentHospital(@Param("hospitalId") UUID hospitalId, @Param("q") String query);
}
