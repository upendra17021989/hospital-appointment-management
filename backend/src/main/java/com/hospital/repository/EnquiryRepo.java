package com.hospital.repository;

import com.hospital.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnquiryRepo extends JpaRepository<Enquiry, UUID> {
    List<Enquiry> findByStatus(Enquiry.Status status);
    List<Enquiry> findByEnquiryType(Enquiry.EnquiryType type);
    List<Enquiry> findByPhone(String phone);
    List<Enquiry> findByEmail(String email);

    @Query("SELECT e FROM Enquiry e WHERE e.status = 'open' OR e.status = 'in_progress' ORDER BY e.createdAt DESC")
    List<Enquiry> findActiveEnquiries();
}
