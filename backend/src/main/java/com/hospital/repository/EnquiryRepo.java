package com.hospital.repository;

import com.hospital.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnquiryRepo extends JpaRepository<Enquiry, UUID> {
    List<Enquiry> findByStatus(Enquiry.Status status);
    List<Enquiry> findByEnquiryType(Enquiry.EnquiryType type);
    List<Enquiry> findByPhone(String phone);
    List<Enquiry> findByEmail(String email);

    // Multi-tenant
    List<Enquiry> findByHospitalId(UUID hospitalId);
    List<Enquiry> findByHospitalIdAndStatus(UUID hospitalId, Enquiry.Status status);

    @Query("SELECT e FROM Enquiry e WHERE e.status = 'open' OR e.status = 'in_progress' ORDER BY e.createdAt DESC")
    List<Enquiry> findActiveEnquiries();

    @Query("SELECT e FROM Enquiry e WHERE e.hospital.id = :hospitalId AND (e.status = 'open' OR e.status = 'in_progress') ORDER BY e.createdAt DESC")
    List<Enquiry> findActiveEnquiriesByHospital(@Param("hospitalId") UUID hospitalId);

    // Robust tenant scoping (covers older rows where enquiry.hospital might be null)
    @Query("SELECT e FROM Enquiry e WHERE (e.hospital.id = :hospitalId OR e.department.hospital.id = :hospitalId) AND (e.status = 'open' OR e.status = 'in_progress') ORDER BY e.createdAt DESC")
    List<Enquiry> findActiveEnquiriesByHospitalRobust(@Param("hospitalId") UUID hospitalId);

    @Query("SELECT e FROM Enquiry e WHERE (e.hospital.id = :hospitalId OR e.department.hospital.id = :hospitalId) AND e.status = :status ORDER BY e.createdAt DESC")
    List<Enquiry> findByHospitalOrDepartmentHospitalIdAndStatusRobust(@Param("hospitalId") UUID hospitalId, @Param("status") Enquiry.Status status);

    @Query("SELECT e FROM Enquiry e WHERE (e.hospital.id = :hospitalId OR e.department.hospital.id = :hospitalId) ORDER BY e.createdAt DESC")
    List<Enquiry> findByHospitalOrDepartmentHospitalIdRobust(@Param("hospitalId") UUID hospitalId);
}
