package com.hospital.service;

import com.hospital.dto.Dtos.*;
import com.hospital.model.Hospital;
import com.hospital.model.Department;
import com.hospital.model.Enquiry;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.DepartmentRepo;
import com.hospital.repository.EnquiryRepo;
import com.hospital.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepo enquiryRepo;
    private final DepartmentRepo departmentRepo;
    private final HospitalRepo hospitalRepo;
    private final TenantContext tenantContext;

    @Transactional
    public EnquiryResponse submitEnquiry(EnquiryRequest request) {
        UUID hospitalId = tenantContext.requireHospitalId();

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepo.findById(request.getDepartmentId()).orElse(null);
        }

        Hospital hospital = null;
        if (department != null) {
            hospital = department.getHospital();
        }
        if (hospital == null) {
            hospital = hospitalRepo.findById(hospitalId)
                    .orElseThrow(() -> new RuntimeException("Hospital not found"));
        }

        if (department != null && department.getHospital() != null
                && !hospitalId.equals(department.getHospital().getId())) {
            throw new RuntimeException("Department not found for current hospital");
        }

        Enquiry enquiry = Enquiry.builder()
                .hospital(hospital)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .subject(request.getSubject())
                .message(request.getMessage())
                .department(department)
                .enquiryType(request.getEnquiryType() != null
                        ? request.getEnquiryType()
                        : Enquiry.EnquiryType.general)
                .priority(request.getPriority() != null
                        ? request.getPriority()
                        : Enquiry.Priority.normal)
                .status(Enquiry.Status.open)
                .build();

        return mapToResponse(enquiryRepo.save(enquiry));
    }

    public List<EnquiryResponse> getHospitalActiveEnquiries(UUID hospitalId) {
        return enquiryRepo.findActiveEnquiriesByHospitalRobust(hospitalId)
                .stream().map(this::mapToResponse).toList();
    }

    public List<EnquiryResponse> getHospitalEnquiriesByStatus(UUID hospitalId, Enquiry.Status status) {
        return enquiryRepo.findByHospitalOrDepartmentHospitalIdAndStatusRobust(hospitalId, status)
                .stream().map(this::mapToResponse).toList();
    }

    public List<EnquiryResponse> getHospitalAllEnquiries(UUID hospitalId) {
        return enquiryRepo.findByHospitalOrDepartmentHospitalIdRobust(hospitalId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public EnquiryResponse updateEnquiryStatusForHospital(UUID hospitalId, UUID id, Enquiry.Status status, String response) {
        Enquiry enquiry = enquiryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found with id: " + id));

        UUID effectiveHospitalId =
                enquiry.getHospital() != null ? enquiry.getHospital().getId()
                        : (enquiry.getDepartment() != null && enquiry.getDepartment().getHospital() != null
                            ? enquiry.getDepartment().getHospital().getId()
                            : null);

        if (effectiveHospitalId == null || !effectiveHospitalId.equals(hospitalId)) {
            throw new RuntimeException("Enquiry not found for current hospital");
        }

        enquiry.setStatus(status);
        if (response != null && !response.isBlank()) {
            enquiry.setResponse(response);
        }
        if (status == Enquiry.Status.resolved) {
            enquiry.setResolvedAt(LocalDateTime.now());
        }

        return mapToResponse(enquiryRepo.save(enquiry));
    }

    @Transactional
    public EnquiryResponse assignEnquiryForHospital(UUID hospitalId, UUID id, String assignedTo) {
        Enquiry enquiry = enquiryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found with id: " + id));

        UUID effectiveHospitalId =
                enquiry.getHospital() != null ? enquiry.getHospital().getId()
                        : (enquiry.getDepartment() != null && enquiry.getDepartment().getHospital() != null
                        ? enquiry.getDepartment().getHospital().getId()
                        : null);

        if (effectiveHospitalId == null || !effectiveHospitalId.equals(hospitalId)) {
            throw new RuntimeException("Enquiry not found for current hospital");
        }

        enquiry.setAssignedTo(assignedTo);
        enquiry.setStatus(Enquiry.Status.in_progress);
        return mapToResponse(enquiryRepo.save(enquiry));
    }

    public List<EnquiryResponse> getActiveEnquiries() {
        return enquiryRepo.findActiveEnquiries().stream().map(this::mapToResponse).toList();
    }

    public List<EnquiryResponse> getEnquiriesByStatus(Enquiry.Status status) {
        return enquiryRepo.findByStatus(status).stream().map(this::mapToResponse).toList();
    }

    public List<EnquiryResponse> getAllEnquiries() {
        return enquiryRepo.findAll().stream().map(this::mapToResponse).toList();
    }

    public Optional<EnquiryResponse> getEnquiryById(UUID id) {
        return enquiryRepo.findById(id).map(this::mapToResponse);
    }

    @Transactional
    public EnquiryResponse updateEnquiryStatus(UUID id, Enquiry.Status status, String response) {
        Enquiry enquiry = enquiryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found with id: " + id));

        enquiry.setStatus(status);
        if (response != null && !response.isBlank()) {
            enquiry.setResponse(response);
        }
        if (status == Enquiry.Status.resolved) {
            enquiry.setResolvedAt(LocalDateTime.now());
        }

        return mapToResponse(enquiryRepo.save(enquiry));
    }

    @Transactional
    public EnquiryResponse assignEnquiry(UUID id, String assignedTo) {
        Enquiry enquiry = enquiryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found with id: " + id));
        enquiry.setAssignedTo(assignedTo);
        enquiry.setStatus(Enquiry.Status.in_progress);
        return mapToResponse(enquiryRepo.save(enquiry));
    }

    private EnquiryResponse mapToResponse(Enquiry e) {
        DepartmentResponse dept = null;
        if (e.getDepartment() != null) {
            dept = DepartmentResponse.builder()
                    .id(e.getDepartment().getId())
                    .name(e.getDepartment().getName())
                    .phone(e.getDepartment().getPhone())
                    .email(e.getDepartment().getEmail())
                    .build();
        }
        return EnquiryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .subject(e.getSubject())
                .message(e.getMessage())
                .department(dept)
                .enquiryType(e.getEnquiryType())
                .status(e.getStatus())
                .priority(e.getPriority())
                .assignedTo(e.getAssignedTo())
                .response(e.getResponse())
                .resolvedAt(e.getResolvedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
