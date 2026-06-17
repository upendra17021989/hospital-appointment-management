package com.hospital.repository;

import com.hospital.model.ConsultationPaymentLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsultationPaymentLineItemRepo extends JpaRepository<ConsultationPaymentLineItem, UUID> {

    List<ConsultationPaymentLineItem> findByPaymentIdOrderBySrNoAsc(UUID paymentId);
}

