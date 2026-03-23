package com.hospital.service;

import com.hospital.dto.Dtos.DepartmentResponse;
import com.hospital.dto.Dtos.DoctorResponse;
import com.hospital.model.Doctor;
import com.hospital.repository.DoctorRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepo doctorRepo;

    public List<DoctorResponse> getAllAvailableDoctors() {
        return doctorRepo.findByIsAvailableTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DoctorResponse> getDoctorsByDepartment(UUID departmentId) {
        return doctorRepo.findByDepartmentIdAndIsAvailableTrue(departmentId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DoctorResponse> searchDoctors(String query) {
        return doctorRepo.searchDoctors(query).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Optional<DoctorResponse> getDoctorById(UUID id) {
        return doctorRepo.findById(id).map(this::mapToResponse);
    }

    public DoctorResponse mapToResponse(Doctor d) {
        DepartmentResponse dept = null;
        if (d.getDepartment() != null) {
            dept = DepartmentResponse.builder()
                    .id(d.getDepartment().getId())
                    .name(d.getDepartment().getName())
                    .floorNumber(d.getDepartment().getFloorNumber())
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
                .profileImageUrl(d.getProfileImageUrl())
                .consultationFee(d.getConsultationFee())
                .isAvailable(d.getIsAvailable())
                .languagesSpoken(d.getLanguagesSpoken())
                .department(dept)
                .build();
    }
}
