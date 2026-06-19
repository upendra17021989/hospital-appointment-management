package com.hospital.repository;

import com.hospital.model.HospitalLegalAcceptance;
import com.hospital.model.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalLegalAcceptanceRepo extends JpaRepository<HospitalLegalAcceptance, UUID> {
    List<HospitalLegalAcceptance> findByHospitalIdOrderByAcceptedAtDesc(UUID hospitalId);
    Optional<HospitalLegalAcceptance> findByHospitalIdAndDocumentTypeAndDocumentVersion(UUID hospitalId, LegalDocument.DocumentType documentType, String documentVersion);
}
