package com.hospital.repository;

import com.hospital.model.HospitalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalDocumentRepo extends JpaRepository<HospitalDocument, UUID> {
    List<HospitalDocument> findByHospitalIdOrderByUploadedAtDesc(UUID hospitalId);

    Optional<HospitalDocument> findByIdAndHospitalId(UUID id, UUID hospitalId);

    List<HospitalDocument> findByHospitalIdAndDocumentTypeOrderByUploadedAtDesc(UUID hospitalId, HospitalDocument.DocumentType documentType);
}
