package com.hospital.controller;

import com.hospital.dto.Dtos.*;
import com.hospital.model.Department;
import com.hospital.model.Doctor;
import com.hospital.model.DoctorSchedule;
import com.hospital.repository.DepartmentRepo;
import com.hospital.repository.DoctorRepo;
import com.hospital.repository.DoctorScheduleRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Doctor Management", description = "Doctor CRUD and schedule management APIs")
public class DoctorManagementController {

    private final DoctorRepo doctorRepo;
    private final DepartmentRepo departmentRepo;
    private final DoctorScheduleRepo scheduleRepo;

    // ── Request DTOs ─────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorSaveRequest {
        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        @NotBlank(message = "Specialization is required")
        private String specialization;

        private String qualification;
        private Integer experienceYears;
        private String phone;
        private String email;
        private String bio;
        private String profileImageUrl;
        private BigDecimal consultationFee;
        private Boolean isAvailable;

        @NotNull(message = "Department ID is required")
        private UUID departmentId;

        private String[] languagesSpoken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleSaveRequest {
        @NotNull(message = "Doctor ID is required")
        private UUID doctorId;

        @NotNull(message = "Day of week is required")
        @Min(0) @Max(6)
        private Integer dayOfWeek;

        @NotNull(message = "Start time is required")
        private LocalTime startTime;

        @NotNull(message = "End time is required")
        private LocalTime endTime;

        private Integer slotDurationMinutes = 30;
        private Integer maxAppointments = 20;
        private Boolean isActive = true;
    }

    // ── Doctor CRUD ───────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @RequireHospitalContext
    @Operation(summary = "Add a new doctor")
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(
            @Valid @RequestBody DoctorSaveRequest request) {
        try {
            Department dept = departmentRepo.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            Doctor doctor = Doctor.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .specialization(request.getSpecialization())
                    .qualification(request.getQualification())
                    .experienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0)
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .bio(request.getBio())
                    .profileImageUrl(request.getProfileImageUrl())
                    .consultationFee(request.getConsultationFee() != null
                            ? request.getConsultationFee() : BigDecimal.ZERO)
                    .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                    .department(dept)
                    .languagesSpoken(request.getLanguagesSpoken())
                    .build();

            Doctor saved = doctorRepo.save(doctor);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Doctor added successfully", mapDoctor(saved)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @RequireHospitalContext
    @Operation(summary = "Update doctor details")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody DoctorSaveRequest request) {
        try {
            Doctor doctor = doctorRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            Department dept = departmentRepo.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            doctor.setFirstName(request.getFirstName());
            doctor.setLastName(request.getLastName());
            doctor.setSpecialization(request.getSpecialization());
            doctor.setQualification(request.getQualification());
            doctor.setExperienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0);
            doctor.setPhone(request.getPhone());
            doctor.setEmail(request.getEmail());
            doctor.setBio(request.getBio());
            doctor.setProfileImageUrl(request.getProfileImageUrl());
            doctor.setConsultationFee(request.getConsultationFee() != null
                    ? request.getConsultationFee() : BigDecimal.ZERO);
            doctor.setIsAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true);
            doctor.setDepartment(dept);
            doctor.setLanguagesSpoken(request.getLanguagesSpoken());

            Doctor updated = doctorRepo.save(doctor);
            return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully", mapDoctor(updated)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN') or hasRole('SUPER_ADMIN')")
    @RequireHospitalContext
    @Operation(summary = "Delete a doctor")
    public ResponseEntity<ApiResponse<String>> deleteDoctor(@PathVariable UUID id) {
        try {
            if (!doctorRepo.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            doctorRepo.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Doctor deleted successfully", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/availability")
    @Operation(summary = "Toggle doctor availability")
    public ResponseEntity<ApiResponse<DoctorResponse>> toggleAvailability(@PathVariable UUID id) {
        try {
            Doctor doctor = doctorRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            doctor.setIsAvailable(!doctor.getIsAvailable());
            Doctor updated = doctorRepo.save(doctor);
            return ResponseEntity.ok(ApiResponse.success("Availability updated", mapDoctor(updated)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Schedule Endpoints ────────────────────────────────────────

    @GetMapping("/{id}/schedules")
    @Operation(summary = "Get all schedules for a doctor")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getDoctorSchedules(
            @PathVariable UUID id) {
        List<ScheduleResponse> schedules = scheduleRepo.findByDoctorIdAndIsActiveTrue(id)
                .stream().map(this::mapSchedule).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }

    @PostMapping("/schedules")
    @Operation(summary = "Add a schedule for a doctor")
    public ResponseEntity<ApiResponse<ScheduleResponse>> addSchedule(
            @Valid @RequestBody ScheduleSaveRequest request) {
        try {
            Doctor doctor = doctorRepo.findById(request.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            DoctorSchedule schedule = DoctorSchedule.builder()
                    .doctor(doctor)
                    .dayOfWeek(request.getDayOfWeek())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .slotDurationMinutes(request.getSlotDurationMinutes() != null
                            ? request.getSlotDurationMinutes() : 30)
                    .maxAppointments(request.getMaxAppointments() != null
                            ? request.getMaxAppointments() : 20)
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();

            DoctorSchedule saved = scheduleRepo.save(schedule);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Schedule added", mapSchedule(saved)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/schedules/{scheduleId}/toggle")
    @Operation(summary = "Toggle schedule active/inactive")
    public ResponseEntity<ApiResponse<ScheduleResponse>> toggleSchedule(
            @PathVariable UUID scheduleId) {
        try {
            DoctorSchedule schedule = scheduleRepo.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("Schedule not found"));
            schedule.setIsActive(!schedule.getIsActive());
            DoctorSchedule updated = scheduleRepo.save(schedule);
            return ResponseEntity.ok(ApiResponse.success("Schedule toggled", mapSchedule(updated)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/schedules/{scheduleId}")
    @Operation(summary = "Delete a schedule")
    public ResponseEntity<ApiResponse<String>> deleteSchedule(
            @PathVariable UUID scheduleId) {
        try {
            if (!scheduleRepo.existsById(scheduleId)) {
                return ResponseEntity.notFound().build();
            }
            scheduleRepo.deleteById(scheduleId);
            return ResponseEntity.ok(ApiResponse.success("Schedule deleted", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Mappers ───────────────────────────────────────────────────

    private DoctorResponse mapDoctor(Doctor d) {
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

    // ── Schedule Response DTO ─────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleResponse {
        private UUID id;
        private UUID doctorId;
        private Integer dayOfWeek;
        private String dayName;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer slotDurationMinutes;
        private Integer maxAppointments;
        private Boolean isActive;
    }

    private ScheduleResponse mapSchedule(DoctorSchedule s) {
        String[] days = {"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        return ScheduleResponse.builder()
                .id(s.getId())
                .doctorId(s.getDoctor() != null ? s.getDoctor().getId() : null)
                .dayOfWeek(s.getDayOfWeek())
                .dayName(s.getDayOfWeek() != null && s.getDayOfWeek() >= 0 && s.getDayOfWeek() <= 6
                        ? days[s.getDayOfWeek()] : "Unknown")
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .slotDurationMinutes(s.getSlotDurationMinutes())
                .maxAppointments(s.getMaxAppointments())
                .isActive(s.getIsActive())
                .build();
    }
}
