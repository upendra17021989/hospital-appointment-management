package com.hospital.repository;

import com.hospital.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorRepo extends JpaRepository<Doctor, UUID> {
    List<Doctor> findByIsAvailableTrue();
    List<Doctor> findByDepartmentIdAndIsAvailableTrue(UUID departmentId);

    @Query("SELECT d FROM Doctor d WHERE d.isAvailable = TRUE AND " +
           "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(d.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(d.specialization) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Doctor> searchDoctors(@Param("q") String query);
}
