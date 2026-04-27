package com.hospital.repository;

import com.hospital.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, UUID> {
    Page<Payment> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId, Pageable pageable);
}

