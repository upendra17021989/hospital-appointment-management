package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.*;
import com.hospital.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Prescriptions", description = "Prescription management APIs")
public class PrescriptionController {

    private final PrescriptionRepo prescriptionRepo;
    private final PatientRepo      patientRepo;
    private final DoctorRepo       doctorRepo;
    private final AppointmentRepo  appointmentRepo;
    private final HospitalRepo     hospitalRepo;
    private final com.hospital.security.TenantContext tenantContext;

    // ── DTOs ──────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MedicineRequest {
        @NotBlank private String medicineName;
        private String dosage;
        private String frequency;
        private String duration;
        private String route;
        private Boolean beforeFood;
        private String instructions;
        private Integer sortOrder;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LabTestRequest {
        @NotBlank private String testName;
        private String instructions;
        private Boolean isUrgent;
        private Integer sortOrder;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PrescriptionRequest {
        @NotNull private UUID patientId;
        @NotNull private UUID doctorId;
        private UUID appointmentId;
        private UUID hospitalId;

        @NotBlank private String diagnosis;
        private String chiefComplaint;
        private String examinationNotes;
        private String vitalSigns;          // JSON string
        private LocalDate followUpDate;
        private String followUpInstructions;
        private String dietInstructions;
        private String activityRestrictions;
        private String additionalNotes;

        private List<MedicineRequest> medicines;
        private List<LabTestRequest>  labTests;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MedicineResponse {
        private UUID id;
        private String medicineName;
        private String dosage;
        private String frequency;
        private String duration;
        private String route;
        private Boolean beforeFood;
        private String instructions;
        private Integer sortOrder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LabTestResponse {
        private UUID id;
        private String testName;
        private String instructions;
        private Boolean isUrgent;
        private Integer sortOrder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PrescriptionResponse {
        private UUID id;
        private UUID appointmentId;
        private PatientInfo patient;
        private DoctorInfo  doctor;
        private LocalDate   prescriptionDate;
        private String diagnosis;
        private String chiefComplaint;
        private String examinationNotes;
        private String vitalSigns;
        private LocalDate followUpDate;
        private String followUpInstructions;
        private String dietInstructions;
        private String activityRestrictions;
        private String additionalNotes;
        private List<MedicineResponse> medicines;
        private List<LabTestResponse>  labTests;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PatientInfo {
        private UUID id; private String fullName;
        private String phone; private String email;
        private String gender; private String bloodGroup;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DoctorInfo {
        private UUID id; private String fullName;
        private String specialization; private String qualification;
        private String departmentName;
    }

    // ── Endpoints ─────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new prescription")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> create(
            @Valid @RequestBody PrescriptionRequest req) {
        try {
            UUID hospitalId = tenantContext.requireHospitalId();
            Hospital hospital = hospitalRepo.findById(hospitalId)
                    .orElseThrow(() -> new RuntimeException("Hospital not found"));

            Patient patient = patientRepo.findById(req.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            Doctor doctor = doctorRepo.findById(req.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            if (patient.getHospital() == null || !hospitalId.equals(patient.getHospital().getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Patient not found for current hospital"));
            }
            if (doctor.getHospital() == null || !hospitalId.equals(doctor.getHospital().getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Doctor not found for current hospital"));
            }

            Prescription prescription = Prescription.builder()
                    .hospital(hospital)
                    .patient(patient)
                    .doctor(doctor)
                    .prescriptionDate(LocalDate.now())
                    .diagnosis(req.getDiagnosis())
                    .chiefComplaint(req.getChiefComplaint())
                    .examinationNotes(req.getExaminationNotes())
                    .vitalSigns(req.getVitalSigns())
                    .followUpDate(req.getFollowUpDate())
                    .followUpInstructions(req.getFollowUpInstructions())
                    .dietInstructions(req.getDietInstructions())
                    .activityRestrictions(req.getActivityRestrictions())
                    .additionalNotes(req.getAdditionalNotes())
                    .isActive(true)
                    .build();

            // Link appointment (and validate hospital scope)
            if (req.getAppointmentId() != null) {
                Appointment appt = appointmentRepo.findById(req.getAppointmentId())
                        .orElseThrow(() -> new RuntimeException("Appointment not found"));
                UUID effectiveHospitalId = appt.getHospital() != null
                        ? appt.getHospital().getId()
                        : (appt.getDoctor() != null && appt.getDoctor().getHospital() != null
                            ? appt.getDoctor().getHospital().getId()
                            : null);
                if (effectiveHospitalId == null || !hospitalId.equals(effectiveHospitalId)) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Appointment not found for current hospital"));
                }
                prescription.setAppointment(appt);
            }

            // Link hospital
            // Medicines
            if (req.getMedicines() != null) {
                List<PrescriptionMedicine> meds = req.getMedicines().stream()
                        .map(m -> PrescriptionMedicine.builder()
                                .prescription(prescription)
                                .medicineName(m.getMedicineName())
                                .dosage(m.getDosage())
                                .frequency(m.getFrequency())
                                .duration(m.getDuration())
                                .route(m.getRoute() != null ? m.getRoute() : "Oral")
                                .beforeFood(m.getBeforeFood() != null ? m.getBeforeFood() : false)
                                .instructions(m.getInstructions())
                                .sortOrder(m.getSortOrder() != null ? m.getSortOrder() : 0)
                                .build())
                        .collect(Collectors.toList());
                prescription.setMedicines(meds);
            }

            // Lab Tests
            if (req.getLabTests() != null) {
                List<LabTest> tests = req.getLabTests().stream()
                        .map(t -> LabTest.builder()
                                .prescription(prescription)
                                .testName(t.getTestName())
                                .instructions(t.getInstructions())
                                .isUrgent(t.getIsUrgent() != null ? t.getIsUrgent() : false)
                                .sortOrder(t.getSortOrder() != null ? t.getSortOrder() : 0)
                                .build())
                        .collect(Collectors.toList());
                prescription.setLabTests(tests);
            }

            Prescription saved = prescriptionRepo.save(prescription);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Prescription created successfully", mapResponse(saved)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/hospital/patient/{patientId}")
    @Operation(summary = "Get hospital prescriptions for a patient")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getHospitalByPatient(
            @PathVariable UUID patientId) {
        UUID hospitalId = tenantContext.requireHospitalId();
        List<PrescriptionResponse> list = prescriptionRepo
                .findByHospitalOrDoctorOrPatientHospitalIdAndPatientIdOrderByCreatedAtDesc(hospitalId, patientId)
                .stream().map(this::mapResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/hospital/appointment/{appointmentId}")
    @Operation(summary = "Get hospital prescription for an appointment")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getHospitalByAppointment(
            @PathVariable UUID appointmentId) {
        UUID hospitalId = tenantContext.requireHospitalId();
        return prescriptionRepo.findByHospitalOrDoctorOrPatientHospitalIdAndAppointmentId(hospitalId, appointmentId)
                .map(p -> ResponseEntity.ok(ApiResponse.success(mapResponse(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription by ID")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getById(@PathVariable UUID id) {
        return prescriptionRepo.findById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.success(mapResponse(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all prescriptions for a patient")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getByPatient(
            @PathVariable UUID patientId) {
        List<PrescriptionResponse> list = prescriptionRepo
                .findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::mapResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "Get prescription for a specific appointment")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getByAppointment(
            @PathVariable UUID appointmentId) {
        return prescriptionRepo.findByAppointmentId(appointmentId)
                .map(p -> ResponseEntity.ok(ApiResponse.success(mapResponse(p))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all prescriptions")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getAll() {
        List<PrescriptionResponse> list = prescriptionRepo.findAll()
                .stream().map(this::mapResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prescription")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID id) {
        if (!prescriptionRepo.existsById(id)) return ResponseEntity.notFound().build();
        prescriptionRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Prescription deleted", "deleted"));
    }

    // ── Mapper ────────────────────────────────────────────────────
    private PrescriptionResponse mapResponse(Prescription p) {
        PatientInfo pi = PatientInfo.builder()
                .id(p.getPatient().getId())
                .fullName(p.getPatient().getFullName())
                .phone(p.getPatient().getPhone())
                .email(p.getPatient().getEmail())
                .gender(p.getPatient().getGender())
                .bloodGroup(p.getPatient().getBloodGroup())
                .build();

        DoctorInfo di = DoctorInfo.builder()
                .id(p.getDoctor().getId())
                .fullName(p.getDoctor().getFullName())
                .specialization(p.getDoctor().getSpecialization())
                .qualification(p.getDoctor().getQualification())
                .departmentName(p.getDoctor().getDepartment() != null
                        ? p.getDoctor().getDepartment().getName() : null)
                .build();

        List<MedicineResponse> meds = p.getMedicines() == null ? List.of() :
                p.getMedicines().stream().sorted((a, b) -> Integer.compare(
                        a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .map(m -> MedicineResponse.builder()
                        .id(m.getId()).medicineName(m.getMedicineName())
                        .dosage(m.getDosage()).frequency(m.getFrequency())
                        .duration(m.getDuration()).route(m.getRoute())
                        .beforeFood(m.getBeforeFood()).instructions(m.getInstructions())
                        .sortOrder(m.getSortOrder()).build())
                .collect(Collectors.toList());

        List<LabTestResponse> tests = p.getLabTests() == null ? List.of() :
                p.getLabTests().stream().sorted((a, b) -> Integer.compare(
                        a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .map(t -> LabTestResponse.builder()
                        .id(t.getId()).testName(t.getTestName())
                        .instructions(t.getInstructions()).isUrgent(t.getIsUrgent())
                        .sortOrder(t.getSortOrder()).build())
                .collect(Collectors.toList());

        return PrescriptionResponse.builder()
                .id(p.getId())
                .appointmentId(p.getAppointment() != null ? p.getAppointment().getId() : null)
                .patient(pi).doctor(di)
                .prescriptionDate(p.getPrescriptionDate())
                .diagnosis(p.getDiagnosis())
                .chiefComplaint(p.getChiefComplaint())
                .examinationNotes(p.getExaminationNotes())
                .vitalSigns(p.getVitalSigns())
                .followUpDate(p.getFollowUpDate())
                .followUpInstructions(p.getFollowUpInstructions())
                .dietInstructions(p.getDietInstructions())
                .activityRestrictions(p.getActivityRestrictions())
                .additionalNotes(p.getAdditionalNotes())
                .medicines(meds).labTests(tests)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
