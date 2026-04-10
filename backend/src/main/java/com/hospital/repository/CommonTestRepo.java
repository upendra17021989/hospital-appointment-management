package com.hospital.repository;

import com.hospital.model.CommonTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommonTestRepo extends JpaRepository<CommonTest, UUID> {
    
    List<CommonTest> findByHospitalIdOrderByNameAsc(UUID hospitalId);
    
    Optional<CommonTest> findByHospitalIdAndNameIgnoreCase(UUID hospitalId, String name);
}

