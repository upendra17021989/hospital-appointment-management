package com.hospital.repository;

import com.hospital.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepo extends JpaRepository<Department, UUID> {
    // Legacy (no tenant isolation)
    List<Department> findByIsActiveTrue();
    // Multi-tenant
    List<Department> findByHospitalIdAndIsActiveTrue(UUID hospitalId);
    List<Department> findByHospitalId(UUID hospitalId);
    Optional<Department> findByIdAndHospitalId(UUID id, UUID hospitalId);
}
