package com.hospital.repository;

import com.hospital.model.CommonMedicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommonMedicineRepo extends JpaRepository<CommonMedicine, UUID> {
    
    List<CommonMedicine> findByHospitalIdOrderByNameAsc(UUID hospitalId);
    
    Optional<CommonMedicine> findByHospitalIdAndNameIgnoreCase(UUID hospitalId, String name);
}

