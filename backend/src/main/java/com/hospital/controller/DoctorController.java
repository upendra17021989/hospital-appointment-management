package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.DepartmentResponse;
import com.hospital.dto.Dtos.DoctorResponse;
import com.hospital.dto.Dtos.TimeSlot;
import com.hospital.model.Doctor;
import com.hospital.repository.DoctorRepo;
import com.hospital.security.TenantContext;
import com.hospital.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Doctors", description = "Doctor management APIs")
public class DoctorController {

    private final DoctorRepo doctorRepo;
    private final AppointmentService appointmentService;
    private final TenantContext tenantContext;

    @GetMapping
    @Operation(summary = "Get all available doctors")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String search) {

        List<Doctor> doctors;
        if (search != null && !search.isBlank()) {
            doctors = doctorRepo.searchDoctors(search);
        } else if (departmentId != null) {
            doctors = doctorRepo.findByDepartmentIdAndIsAvailableTrue(departmentId);
        } else {
            doctors = doctorRepo.findByIsAvailableTrue();
        }

        List<DoctorResponse> response = doctors.stream()
                .map(this::mapDoctor)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/hospital/list")
    @PreAuthorize("hasRole('STAFF') or hasRole('RECEPTIONIST') or hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get available doctors for current hospital")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getHospitalDoctors(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String search) {
        UUID hospitalId = tenantContext.requireHospitalId();
        List<Doctor> doctors;
        if (search != null && !search.isBlank()) {
            doctors = doctorRepo.searchDoctorsByDepartmentHospital(hospitalId, search);
        } else if (departmentId != null) {
            doctors = doctorRepo.findByDepartmentHospitalIdAndDepartmentIdAndIsAvailableTrue(hospitalId, departmentId);
        } else {
            doctors = doctorRepo.findByDepartmentHospitalIdAndIsAvailableTrue(hospitalId);
        }

        List<DoctorResponse> response = doctors.stream()
                .map(this::mapDoctor)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable UUID id) {
        return doctorRepo.findById(id)
                .map(d -> ResponseEntity.ok(ApiResponse.success(mapDoctor(d))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "Get available appointment slots for a doctor")
    public ResponseEntity<ApiResponse<List<TimeSlot>>> getAvailableSlots(
            @PathVariable UUID id,
            @RequestParam String date) {
        LocalDate appointmentDate = LocalDate.parse(date);
        List<TimeSlot> slots = appointmentService.getAvailableSlots(id, appointmentDate);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    private DoctorResponse mapDoctor(Doctor d) {
        DepartmentResponse dept = null;
        if (d.getDepartment() != null) {
            dept = DepartmentResponse.builder()
                    .id(d.getDepartment().getId())
                    .name(d.getDepartment().getName())
                    .build();
        }
        return DoctorResponse.builder()
                .id(d.getId())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .fullName(d.getFullName())
                .specialization(d.getSpecialization())
                .qualification(d.getQualification())
                .experienceYears(d.getExperienceYears())
                .phone(d.getPhone())
                .email(d.getEmail())
                .bio(d.getBio())
                .consultationFee(d.getConsultationFee())
                .isAvailable(d.getIsAvailable())
                .languagesSpoken(d.getLanguagesSpoken())
                .department(dept)
                .build();
    }
}
