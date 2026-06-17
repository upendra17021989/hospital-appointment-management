package com.hospital.repository;

import com.hospital.model.ConsultationReceiptLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsultationReceiptLineItemRepo extends JpaRepository<ConsultationReceiptLineItem, UUID> {

    List<ConsultationReceiptLineItem> findByReceiptIdOrderBySrNoAsc(UUID receiptId);
}

