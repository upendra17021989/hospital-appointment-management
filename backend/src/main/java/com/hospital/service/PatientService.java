package com.hospital.service;

import com.hospital.dto.Dtos.*;
import com.hospital.model.Patient;
import com.hospital.repository.PatientRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepo patientRepo;

    @Transactional
    public PatientResponse registerPatient(PatientRequest request) {
        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .bloodGroup(request.getBloodGroup())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .medicalHistory(request.getMedicalHistory())
                .allergies(request.getAllergies())
                .build();
        return mapToResponse(patientRepo.save(patient));
    }

    public Optional<PatientResponse> getPatientById(UUID id) {
        return patientRepo.findById(id).map(this::mapToResponse);
    }

    public List<PatientResponse> searchByPhone(String phone) {
        return patientRepo.findByPhone(phone).stream().map(this::mapToResponse).toList();
    }

    public List<PatientResponse> searchByEmail(String email) {
        return patientRepo.findByEmail(email).stream().map(this::mapToResponse).toList();
    }

    public List<PatientResponse> getAllPatients() {
        return patientRepo.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public PatientResponse updatePatient(UUID id, PatientRequest request) {
        Patient patient = patientRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setPhone(request.getPhone());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        patient.setMedicalHistory(request.getMedicalHistory());
        patient.setAllergies(request.getAllergies());
        return mapToResponse(patientRepo.save(patient));
    }

    private PatientResponse mapToResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .fullName(p.getFullName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .phone(p.getPhone())
                .email(p.getEmail())
                .address(p.getAddress())
                .bloodGroup(p.getBloodGroup())
                .emergencyContactName(p.getEmergencyContactName())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
