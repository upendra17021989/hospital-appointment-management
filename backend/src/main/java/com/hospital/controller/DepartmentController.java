package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.DepartmentResponse;
import com.hospital.repository.DepartmentRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}")
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
}
