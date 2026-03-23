package com.hospital.service;

import com.hospital.dto.Dtos.DepartmentResponse;
import com.hospital.model.Department;
import com.hospital.repository.DepartmentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepo departmentRepo;

    public List<DepartmentResponse> getAllActiveDepartments() {
        return departmentRepo.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Optional<DepartmentResponse> getDepartmentById(UUID id) {
        return departmentRepo.findById(id).map(this::mapToResponse);
    }

    public DepartmentResponse mapToResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .floorNumber(d.getFloorNumber())
                .phone(d.getPhone())
                .email(d.getEmail())
                .isActive(d.getIsActive())
                .doctorCount(d.getDoctors() != null ? d.getDoctors().size() : 0)
                .build();
    }
}
