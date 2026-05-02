package com.hospital.service;

import com.hospital.dto.Dtos.*;
import com.hospital.model.Hospital;
import com.hospital.model.Patient;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.PatientRepo;
import com.hospital.security.TenantContext;
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
    private final HospitalRepo hospitalRepo;
    private final TenantContext tenantContext;

    @Transactional
    public PatientResponse registerPatient(PatientRequest request) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));

        Patient patient = Patient.builder()
                .hospital(hospital)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .age(request.getAge())
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
        patient.setAge(request.getAge());
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

public List<PatientResponse> getHospitalPatients(UUID hospitalId) {
        return patientRepo.findByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
                .map(this::mapToResponse).toList();
    }

    public List<PatientResponse> searchByHospitalPhone(UUID hospitalId, String phone) {
        return patientRepo.findByHospitalIdAndPhone(hospitalId, phone).stream()
                .map(this::mapToResponse).toList();
    }

    public List<PatientResponse> searchByHospitalEmail(UUID hospitalId, String email) {
        return patientRepo.findByHospitalIdAndEmail(hospitalId, email).stream()
                .map(this::mapToResponse).toList();
    }

    public List<PatientResponse> searchHospitalPatients(UUID hospitalId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return patientRepo.searchByHospitalId(hospitalId, query.trim())
                .stream()
                .map(this::mapToResponse)
                .limit(20)  // Limit results for performance
                .toList();
    }

    @Transactional
    public PatientResponse updatePatientForHospital(UUID id, PatientRequest request) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Patient patient = patientRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));

        if (patient.getHospital() == null || !hospitalId.equals(patient.getHospital().getId())) {
            throw new RuntimeException("Patient not found for current hospital");
        }

        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setAge(request.getAge());
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

    public Optional<PatientResponse> getHospitalPatientById(UUID hospitalId, UUID id) {
        return patientRepo.findById(id)
                .filter(p -> p.getHospital() != null && hospitalId.equals(p.getHospital().getId()))
                .map(this::mapToResponse);
    }

    private PatientResponse mapToResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .fullName(p.getFullName())
                .dateOfBirth(p.getDateOfBirth())
                .age(p.getAge())
                .gender(p.getGender())
                .phone(p.getPhone())
                .email(p.getEmail())
                .address(p.getAddress())
                .bloodGroup(p.getBloodGroup())
                .emergencyContactName(p.getEmergencyContactName())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .medicalHistory(p.getMedicalHistory())
                .allergies(p.getAllergies())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
