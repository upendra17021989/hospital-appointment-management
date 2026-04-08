package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.DepartmentResponse;
import com.hospital.model.Department;
import com.hospital.model.Hospital;
import com.hospital.model.User;
import com.hospital.repository.DepartmentRepo;
import com.hospital.security.TenantContext;
import com.hospital.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Departments", description = "Department management APIs")
public class DepartmentController {

    private final DepartmentRepo departmentRepo;
    private final DepartmentService departmentService;
    private final TenantContext tenantContext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentCreateRequest {
        @NotBlank(message = "Department name is required")
        private String name;
        private String description;
        private Integer floorNumber;
        private String phone;
        private String email;
    }

    @GetMapping
    @Operation(summary = "Get all active departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        List<DepartmentResponse> departments = departmentRepo.findByIsActiveTrue().stream()
                .map(d -> DepartmentResponse.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .description(d.getDescription())
                        .floorNumber(d.getFloorNumber())
                        .phone(d.getPhone())
                        .email(d.getEmail())
                        .isActive(d.getIsActive())
                        .doctorCount(d.getDoctors() != null ? d.getDoctors().size() : 0)
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(departments));
    }

    @GetMapping("/hospital")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get active departments for current hospital")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getCurrentHospitalDepartments() {
        UUID hospitalId = tenantContext.requireHospitalId();
        List<DepartmentResponse> departments = departmentService.getActiveDepartmentsByHospitalId(hospitalId);
        return ResponseEntity.ok(ApiResponse.success(departments));
    }

    @PostMapping("/hospital")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a department for current hospital")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartmentForCurrentHospital(
            @Valid @RequestBody DepartmentCreateRequest request) {
        Hospital hospital = requireCurrentHospital();
        if (hospital == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No hospital is associated with current user."));
        }

        Department department = Department.builder()
                .hospital(hospital)
                .name(request.getName().trim())
                .description(request.getDescription())
                .floorNumber(request.getFloorNumber())
                .phone(request.getPhone())
                .email(request.getEmail())
                .isActive(true)
                .build();

        Department saved = departmentRepo.save(department);
        DepartmentResponse response = departmentService.mapToResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Department created successfully", response));
    }

    @PutMapping("/hospital/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update a department for current hospital")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartmentForCurrentHospital(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentCreateRequest request) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Department department = departmentRepo.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new RuntimeException("Department not found for current hospital"));

        department.setName(request.getName().trim());
        department.setDescription(request.getDescription());
        department.setFloorNumber(request.getFloorNumber());
        department.setPhone(request.getPhone());
        department.setEmail(request.getEmail());
        Department updated = departmentRepo.save(department);

        return ResponseEntity.ok(ApiResponse.success(
                "Department updated successfully",
                departmentService.mapToResponse(updated)
        ));
    }

    @DeleteMapping("/hospital/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a department for current hospital")
    public ResponseEntity<ApiResponse<String>> deleteDepartmentForCurrentHospital(@PathVariable UUID id) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Department department = departmentRepo.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new RuntimeException("Department not found for current hospital"));
        departmentRepo.delete(department);
        return ResponseEntity.ok(ApiResponse.success("Department deleted successfully", "deleted"));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Get department by ID")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable UUID id) {
        return departmentRepo.findById(id)
                .map(d -> ResponseEntity.ok(ApiResponse.success(
                        DepartmentResponse.builder()
                                .id(d.getId())
                                .name(d.getName())
                                .description(d.getDescription())
                                .floorNumber(d.getFloorNumber())
                                .phone(d.getPhone())
                                .email(d.getEmail())
                                .isActive(d.getIsActive())
                                .build())))
                .orElse(ResponseEntity.notFound().build());
    }

    private Hospital requireCurrentHospital() {
        User currentUser = tenantContext.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Unauthenticated request"));
        return currentUser.getHospital();
    }
}
