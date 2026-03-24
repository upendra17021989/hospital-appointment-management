package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.DashboardStats;
import com.hospital.model.Appointment;
import com.hospital.repository.*;
import com.hospital.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard", description = "Dashboard statistics APIs")
public class DashboardController {

    private final AppointmentRepo appointmentRepo;
    private final EnquiryRepo enquiryRepo;
    private final PatientRepo patientRepo;
    private final DoctorRepo doctorRepo;
    private final DepartmentRepo departmentRepo;
    private final TenantContext tenantContext;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics for the current hospital")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        LocalDate today = LocalDate.now();

        // Try to get hospital-scoped stats; fall back to global if no tenant
        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);

        long totalAppts, todayAppts, pendingAppts, confirmedAppts, completedAppts, cancelledAppts,
             openEnqs, totalPatients, totalDoctors, totalDepts;

        if (hospitalId != null) {
            totalAppts     = appointmentRepo.findByHospitalId(hospitalId).size();
            todayAppts     = appointmentRepo.findByHospitalIdAndAppointmentDate(hospitalId, today).size();
            pendingAppts   = appointmentRepo.findByHospitalIdAndStatus(hospitalId, Appointment.Status.pending).size();
            confirmedAppts = appointmentRepo.findByHospitalIdAndStatus(hospitalId, Appointment.Status.confirmed).size();
            completedAppts = appointmentRepo.findByHospitalIdAndStatus(hospitalId, Appointment.Status.completed).size();
            cancelledAppts = appointmentRepo.findByHospitalIdAndStatus(hospitalId, Appointment.Status.cancelled).size();
            openEnqs       = enquiryRepo.findActiveEnquiriesByHospital(hospitalId).size();
            totalPatients  = patientRepo.findByHospitalId(hospitalId).size();
            totalDoctors   = doctorRepo.findByHospitalIdAndIsAvailableTrue(hospitalId).size();
            totalDepts     = departmentRepo.findByHospitalIdAndIsActiveTrue(hospitalId).size();
        } else {
            // Super admin or no tenant — return global counts
            totalAppts     = appointmentRepo.count();
            todayAppts     = appointmentRepo.findByAppointmentDate(today).size();
            pendingAppts   = appointmentRepo.findByStatus(Appointment.Status.pending).size();
            confirmedAppts = appointmentRepo.findByStatus(Appointment.Status.confirmed).size();
            completedAppts = appointmentRepo.findByStatus(Appointment.Status.completed).size();
            cancelledAppts = appointmentRepo.findByStatus(Appointment.Status.cancelled).size();
            openEnqs       = enquiryRepo.findActiveEnquiries().size();
            totalPatients  = patientRepo.count();
            totalDoctors   = doctorRepo.findByIsAvailableTrue().size();
            totalDepts     = departmentRepo.findByIsActiveTrue().size();
        }

        DashboardStats stats = DashboardStats.builder()
                .totalAppointments(totalAppts)
                .todayAppointments(todayAppts)
                .pendingAppointments(pendingAppts)
                .confirmedAppointments(confirmedAppts)
                .completedAppointments(completedAppts)
                .cancelledAppointments(cancelledAppts)
                .openEnquiries(openEnqs)
                .totalPatients(totalPatients)
                .totalDoctors(totalDoctors)
                .totalDepartments(totalDepts)
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
