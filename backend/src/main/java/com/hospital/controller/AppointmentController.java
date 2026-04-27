package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.AppointmentRequest;
import com.hospital.dto.Dtos.AppointmentResponse;
import com.hospital.dto.Dtos.AppointmentStatusUpdateRequest;
import com.hospital.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.hospital.security.RequireSubscription;
import com.hospital.security.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Appointments", description = "Appointment management APIs")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final TenantContext tenantContext;

    @PostMapping
    @Operation(summary = "Book a new appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            @Valid @RequestBody AppointmentRequest request) {
        try {
            AppointmentResponse appointment = appointmentService.bookAppointment(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Appointment booked successfully", appointment));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all appointments with optional filters")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAllAppointments(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID patientId) {

        List<AppointmentResponse> appointments;
        if (date != null) {
            appointments = appointmentService.getAppointmentsByDate(LocalDate.parse(date));
        } else if (doctorId != null) {
            appointments = appointmentService.getAppointmentsByDoctor(doctorId);
        } else if (patientId != null) {
            appointments = appointmentService.getAppointmentsByPatient(patientId);
        } else {
            appointments = appointmentService.getAllAppointments();
        }
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(@PathVariable UUID id) {
        return appointmentService.getAppointmentById(id)
                .map(a -> ResponseEntity.ok(ApiResponse.success(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request) {
        try {
            AppointmentResponse updated = appointmentService.updateStatus(id, request);
            return ResponseEntity.ok(ApiResponse.success("Status updated successfully", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/hospital")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @RequireSubscription
    @Operation(summary = "Get hospital-scoped appointments with optional filters")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getHospitalAppointments(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID patientId) {
        UUID hospitalId = tenantContext.requireHospitalId();
        List<AppointmentResponse> appointments = appointmentService.getHospitalAppointments(
                hospitalId, date, doctorId, patientId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @PatchMapping("/hospital/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @RequireSubscription
    @Operation(summary = "Update hospital-scoped appointment status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateHospitalStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request) {
        UUID hospitalId = tenantContext.requireHospitalId();
        AppointmentResponse updated = appointmentService.updateStatusForHospital(hospitalId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", updated));
    }
}
