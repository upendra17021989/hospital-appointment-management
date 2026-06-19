package com.hospital.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.dto.MedicalCertificateDtos;
import com.hospital.model.*;
import com.hospital.repository.AppointmentRepo;
import com.hospital.repository.DoctorRepo;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.MedicalCertificateRepo;
import com.hospital.repository.PatientRepo;
import com.hospital.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalCertificateService {

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");

    private final MedicalCertificateRepo certificateRepo;
    private final PatientRepo patientRepo;
    private final DoctorRepo doctorRepo;
    private final AppointmentRepo appointmentRepo;
    private final HospitalRepo hospitalRepo;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    @Transactional
    public MedicalCertificate create(MedicalCertificateDtos.CreateMedicalCertificateRequest req) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
        Patient patient = patientRepo.findById(req.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        Doctor doctor = doctorRepo.findById(req.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        validatePatientHospital(patient, hospitalId);
        validateDoctorHospital(doctor, hospitalId);

        Appointment appointment = null;
        if (req.getAppointmentId() != null) {
            appointment = appointmentRepo.findById(req.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            validateAppointmentHospital(appointment, hospitalId);
        }

        LocalDate issueDate = req.getIssueDate() != null ? req.getIssueDate() : LocalDate.now();
        validateDates(req.getValidFrom(), req.getValidUntil());
        validateRequiredFields(req.getCertificateType(), req.getDynamicFields());

        MedicalCertificate certificate = MedicalCertificate.builder()
                .hospital(hospital)
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .certificateNumber(generateUniqueCertificateNumber(8))
                .certificateType(req.getCertificateType())
                .certificateStatus(MedicalCertificate.CertificateStatus.ACTIVE)
                .issueDate(issueDate)
                .validFrom(req.getValidFrom())
                .validUntil(req.getValidUntil())
                .diagnosisOrReason(req.getDiagnosisOrReason())
                .remarks(req.getRemarks())
                .dynamicFields(writeFields(req.getDynamicFields()))
                .patientName(patient.getFullName())
                .doctorName(doctor.getFullName())
                .departmentName(doctor.getDepartment() != null ? doctor.getDepartment().getName() : null)
                .hospitalName(hospital.getName())
                .hospitalAddress(hospital.getAddress())
                .hospitalPhone(hospital.getPhone())
                .issuedByName(firstPresent(req.getIssuedByName(), tenantContext.getCurrentUser().map(User::getFullName).orElse(null)))
                .build();

        return certificateRepo.save(certificate);
    }

    @Transactional(readOnly = true)
    public Page<MedicalCertificateDtos.MedicalCertificateResponse> search(
            String certificateNumber,
            String patientName,
            String doctorName,
            MedicalCertificate.CertificateType certificateType,
            MedicalCertificate.CertificateStatus certificateStatus,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Sort sort = Sort.by(normalizeSort(sortBy));
        sort = "ASC".equalsIgnoreCase(sortDirection) ? sort.ascending() : sort.descending();
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Specification<MedicalCertificate> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("hospital").get("id"), hospitalId));
            addContainsIgnoreCase(predicates, cb, root.get("certificateNumber"), certificateNumber);
            addContainsIgnoreCase(predicates, cb, root.get("patientName"), patientName);
            addContainsIgnoreCase(predicates, cb, root.get("doctorName"), doctorName);
            if (certificateType != null) predicates.add(cb.equal(root.get("certificateType"), certificateType));
            if (certificateStatus != null) predicates.add(cb.equal(root.get("certificateStatus"), certificateStatus));
            if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return certificateRepo.findAll(spec, PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MedicalCertificate getCertificate(UUID id) {
        UUID hospitalId = tenantContext.requireHospitalId();
        MedicalCertificate certificate = certificateRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        if (certificate.getHospital() == null || !hospitalId.equals(certificate.getHospital().getId())) {
            throw new IllegalArgumentException("Certificate not found for current hospital");
        }
        if (certificate.getHospital() != null) {
            certificate.getHospital().getEmail();
            certificate.getHospital().getLogoUrl();
            certificate.getHospital().getCity();
            certificate.getHospital().getState();
            certificate.getHospital().getPincode();
        }
        return certificate;
    }

    @Transactional
    public MedicalCertificate update(UUID id, MedicalCertificateDtos.UpdateMedicalCertificateRequest req) {
        MedicalCertificate certificate = getCertificate(id);
        if (MedicalCertificate.CertificateStatus.VOIDED.equals(certificate.getCertificateStatus())) {
            throw new IllegalArgumentException("Voided certificates cannot be updated");
        }
        validateDates(req.getValidFrom(), req.getValidUntil());
        validateRequiredFields(certificate.getCertificateType(), req.getDynamicFields());

        if (req.getIssueDate() != null) certificate.setIssueDate(req.getIssueDate());
        certificate.setValidFrom(req.getValidFrom());
        certificate.setValidUntil(req.getValidUntil());
        certificate.setDiagnosisOrReason(req.getDiagnosisOrReason());
        certificate.setRemarks(req.getRemarks());
        certificate.setDynamicFields(writeFields(req.getDynamicFields()));
        certificate.setIssuedByName(firstPresent(req.getIssuedByName(), certificate.getIssuedByName()));
        return certificateRepo.save(certificate);
    }

    @Transactional
    public MedicalCertificateDtos.VoidCertificateResponse voidCertificate(UUID id) {
        MedicalCertificate certificate = getCertificate(id);
        if (!MedicalCertificate.CertificateStatus.VOIDED.equals(certificate.getCertificateStatus())) {
            certificate.setCertificateStatus(MedicalCertificate.CertificateStatus.VOIDED);
            certificate.setVoidedAt(LocalDateTime.now());
            certificate.setVoidedBy(tenantContext.getCurrentUser().map(User::getId).orElse(null));
            certificateRepo.save(certificate);
        }
        return MedicalCertificateDtos.VoidCertificateResponse.builder()
                .certificateId(certificate.getId())
                .certificateNumber(certificate.getCertificateNumber())
                .certificateStatus(certificate.getCertificateStatus())
                .voidedAt(certificate.getVoidedAt())
                .voidedBy(certificate.getVoidedBy())
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.List<MedicalCertificateDtos.MedicalCertificateResponse> patientHistory(UUID patientId) {
        UUID hospitalId = tenantContext.requireHospitalId();
        return certificateRepo.findByHospitalIdAndPatientIdOrderByCreatedAtDesc(hospitalId, patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MedicalCertificateDtos.MedicalCertificateResponse toResponse(MedicalCertificate c) {
        Patient patient = c.getPatient();
        Doctor doctor = c.getDoctor();
        return MedicalCertificateDtos.MedicalCertificateResponse.builder()
                .id(c.getId())
                .certificateNumber(c.getCertificateNumber())
                .certificateType(c.getCertificateType())
                .certificateStatus(c.getCertificateStatus())
                .issueDate(c.getIssueDate())
                .validFrom(c.getValidFrom())
                .validUntil(c.getValidUntil())
                .diagnosisOrReason(c.getDiagnosisOrReason())
                .remarks(c.getRemarks())
                .dynamicFields(readFields(c.getDynamicFields()))
                .patientId(patient != null ? patient.getId() : null)
                .patientName(c.getPatientName())
                .patientAge(patient != null ? patient.getAge() : null)
                .patientGender(patient != null ? patient.getGender() : null)
                .patientPhone(patient != null ? patient.getPhone() : null)
                .doctorId(doctor != null ? doctor.getId() : null)
                .doctorName(c.getDoctorName())
                .doctorQualification(doctor != null ? doctor.getQualification() : null)
                .doctorSpecialization(doctor != null ? doctor.getSpecialization() : null)
                .appointmentId(c.getAppointment() != null ? c.getAppointment().getId() : null)
                .departmentName(c.getDepartmentName())
                .hospitalName(c.getHospitalName())
                .hospitalAddress(c.getHospitalAddress())
                .hospitalPhone(c.getHospitalPhone())
                .issuedByName(c.getIssuedByName())
                .voidedAt(c.getVoidedAt())
                .voidedBy(c.getVoidedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private String generateUniqueCertificateNumber(int maxAttempts) {
        UUID hospitalId = tenantContext.requireHospitalId();
        String year = LocalDate.now().format(YEAR);
        String prefix = "MC-" + year + "-";
        for (int i = 1; i <= maxAttempts; i++) {
            long next = certificateRepo.countByHospitalIdAndCertificateNumberStartingWith(hospitalId, prefix) + 1;
            String candidate = prefix + String.format("%06d", next);
            if (certificateRepo.findByHospitalIdAndCertificateNumber(hospitalId, candidate).isEmpty()) return candidate;
            log.debug("Certificate number collision for hospital {}, candidate={}, attempt={}", hospitalId, candidate, i);
        }
        throw new IllegalStateException("Failed to generate unique certificate number after " + maxAttempts + " attempts");
    }

    private void validatePatientHospital(Patient patient, UUID hospitalId) {
        if (patient.getHospital() == null || !hospitalId.equals(patient.getHospital().getId())) {
            throw new IllegalArgumentException("Patient not found for current hospital");
        }
    }

    private void validateDoctorHospital(Doctor doctor, UUID hospitalId) {
        if (doctor.getHospital() == null || !hospitalId.equals(doctor.getHospital().getId())) {
            throw new IllegalArgumentException("Doctor not found for current hospital");
        }
    }

    private void validateAppointmentHospital(Appointment appointment, UUID hospitalId) {
        UUID effectiveHospitalId = appointment.getHospital() != null
                ? appointment.getHospital().getId()
                : (appointment.getDoctor() != null && appointment.getDoctor().getHospital() != null
                ? appointment.getDoctor().getHospital().getId()
                : null);
        if (effectiveHospitalId == null || !hospitalId.equals(effectiveHospitalId)) {
            throw new IllegalArgumentException("Appointment not found for current hospital");
        }
    }

    private void validateDates(LocalDate validFrom, LocalDate validUntil) {
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException("validUntil cannot be before validFrom");
        }
    }

    private void validateRequiredFields(MedicalCertificate.CertificateType type, Map<String, Object> fields) {
        if (type == null) throw new IllegalArgumentException("certificateType is required");
        Map<String, Object> safeFields = fields != null ? fields : Map.of();
        switch (type) {
            case SICK_LEAVE, CARETAKER_MEDICAL_LEAVE -> requireAnyDateRange(safeFields);
            case VACCINATION -> requireField(safeFields, "vaccineName");
            case FIT_TO_FLY -> requireField(safeFields, "flightDate");
            default -> {
            }
        }
    }

    private void requireAnyDateRange(Map<String, Object> fields) {
        if (isBlank(fields.get("fromDate")) || isBlank(fields.get("toDate"))) {
            throw new IllegalArgumentException("fromDate and toDate are required for this certificate type");
        }
    }

    private void requireField(Map<String, Object> fields, String key) {
        if (isBlank(fields.get(key))) {
            throw new IllegalArgumentException(key + " is required for this certificate type");
        }
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    private String writeFields(Map<String, Object> fields) {
        try {
            return objectMapper.writeValueAsString(fields != null ? fields : Map.of());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid dynamic fields");
        }
    }

    public Map<String, Object> readFields(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String normalizeSort(String sortBy) {
        if (sortBy == null) return "createdAt";
        return switch (sortBy) {
            case "certificateNumber", "patientName", "doctorName", "certificateType", "certificateStatus", "issueDate" -> sortBy;
            default -> "createdAt";
        };
    }

    private void addContainsIgnoreCase(java.util.List<jakarta.persistence.criteria.Predicate> predicates,
                                       jakarta.persistence.criteria.CriteriaBuilder cb,
                                       jakarta.persistence.criteria.Path<String> field,
                                       String value) {
        if (value != null && !value.isBlank()) {
            predicates.add(cb.like(cb.lower(field), "%" + value.toLowerCase(Locale.ROOT) + "%"));
        }
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }
}
