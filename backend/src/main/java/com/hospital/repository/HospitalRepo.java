package com.hospital.repository;

import com.hospital.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRepo extends JpaRepository<Hospital, UUID> {
    Optional<Hospital> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByEmail(String email);
    List<Hospital> findByIsActiveTrue();
}
