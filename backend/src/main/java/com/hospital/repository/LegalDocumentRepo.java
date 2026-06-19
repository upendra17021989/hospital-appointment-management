package com.hospital.repository;

import com.hospital.model.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalDocumentRepo extends JpaRepository<LegalDocument, UUID> {
    List<LegalDocument> findByIsActiveTrueOrderByDocumentTypeAscEffectiveDateDesc();
    Optional<LegalDocument> findFirstByDocumentTypeAndIsActiveTrueOrderByEffectiveDateDesc(LegalDocument.DocumentType documentType);
}
