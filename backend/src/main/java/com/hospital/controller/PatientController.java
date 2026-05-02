package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.PagedResponse;
import com.hospital.dto.Dtos.PatientRequest;
import com.hospital.dto.Dtos.PatientResponse;
import com.hospital.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.hospital.security.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Patients", description = "Patient management APIs")
public class PatientController {

    private final PatientService patientService;
    private final TenantContext tenantContext;

    @PostMapping
    @Operation(summary = "Register a new patient")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody PatientRequest request) {
        PatientResponse patient = patientService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient registered successfully", patient));
    }

    @GetMapping
    @Operation(summary = "Get all patients")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients() {
        return ResponseEntity.ok(ApiResponse.success(patientService.getAllPatients()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatient(
            @PathVariable UUID id) {
        return patientService.getPatientById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search patient by phone or email")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> search(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {
        List<PatientResponse> results;
        if (phone != null && !phone.isBlank()) {
            results = patientService.searchByPhone(phone);
        } else if (email != null && !email.isBlank()) {
            results = patientService.searchByEmail(email);
        } else {
            results = patientService.getAllPatients();
        }
        return ResponseEntity.ok(ApiResponse.success(results));
    }

@GetMapping("/hospital")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get hospital-scoped patients")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getHospitalPatients() {
        UUID hospitalId = tenantContext.requireHospitalId();
        return ResponseEntity.ok(ApiResponse.success(patientService.getHospitalPatients(hospitalId)));
    }

@GetMapping("/hospital/paged")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get hospital patients with pagination and sorting")
    public ResponseEntity<ApiResponse<PagedResponse<PatientResponse>>> getHospitalPatientsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        UUID hospitalId = tenantContext.requireHospitalId();
        return ResponseEntity.ok(ApiResponse.success(patientService.getHospitalPatientsPaged(hospitalId, page, size, sortBy, sortDirection)));
    }

@GetMapping("/hospital/search")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Search hospital patients by name, phone, address, or email")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> searchHospital(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String query) {
        UUID hospitalId = tenantContext.requireHospitalId();

        List<PatientResponse> results;
        if (query != null && !query.isBlank()) {
            results = patientService.searchHospitalPatients(hospitalId, query);
        } else if (phone != null && !phone.isBlank()) {
            results = patientService.searchByHospitalPhone(hospitalId, phone);
        } else if (email != null && !email.isBlank()) {
            results = patientService.searchByHospitalEmail(hospitalId, email);
        } else {
            results = patientService.getHospitalPatients(hospitalId);
        }
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/hospital/{id}")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Get hospital patient by ID")
    public ResponseEntity<ApiResponse<PatientResponse>> getHospitalPatient(@PathVariable UUID id) {
        UUID hospitalId = tenantContext.requireHospitalId();
        return patientService.getHospitalPatientById(hospitalId, id)
                .map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/hospital/{id}")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update hospital patient details")
    public ResponseEntity<ApiResponse<PatientResponse>> updateHospitalPatient(
            @PathVariable UUID id,
            @Valid @RequestBody PatientRequest request) {
        try {
            PatientResponse updated = patientService.updatePatientForHospital(id, request);
            return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient details")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody PatientRequest request) {
        try {
            PatientResponse updated = patientService.updatePatient(id, request);
            return ResponseEntity.ok(
                    ApiResponse.success("Patient updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
