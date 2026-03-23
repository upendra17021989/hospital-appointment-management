package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.DashboardStats;
import com.hospital.model.Appointment;
import com.hospital.repository.AppointmentRepo;
import com.hospital.repository.DepartmentRepo;
import com.hospital.repository.DoctorRepo;
import com.hospital.repository.EnquiryRepo;
import com.hospital.repository.PatientRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        LocalDate today = LocalDate.now();

        DashboardStats stats = DashboardStats.builder()
                .totalAppointments((long) appointmentRepo.findAll().size())
                .todayAppointments((long) appointmentRepo.findByAppointmentDate(today).size())
                .pendingAppointments((long) appointmentRepo.findByStatus(Appointment.Status.pending).size())
                .confirmedAppointments((long) appointmentRepo.findByStatus(Appointment.Status.confirmed).size())
                .completedAppointments((long) appointmentRepo.findByStatus(Appointment.Status.completed).size())
                .cancelledAppointments((long) appointmentRepo.findByStatus(Appointment.Status.cancelled).size())
                .openEnquiries((long) enquiryRepo.findActiveEnquiries().size())
                .totalPatients(patientRepo.count())
                .totalDoctors((long) doctorRepo.findByIsAvailableTrue().size())
                .totalDepartments((long) departmentRepo.findByIsActiveTrue().size())
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
