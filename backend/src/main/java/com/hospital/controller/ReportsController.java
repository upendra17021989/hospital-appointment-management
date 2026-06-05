package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Reports", description = "Patient visit reports APIs")
public class ReportsController {

    private final AppointmentRepo appointmentRepo;
    private final DepartmentRepo departmentRepo;
    private final DoctorRepo doctorRepo;
    private final TenantContext tenantContext;

    @GetMapping("/patients")
    @Operation(summary = "Get patient visit reports between dates")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        // 1. All Patients (unique patients who visited)
        Set<UUID> uniquePatientIds = appointments.stream()
                .map(a -> a.getPatient().getId())
                .collect(Collectors.toSet());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());

        // All Patients
        Map<String, Object> allPatients = new LinkedHashMap<>();
        allPatients.put("totalUniquePatients", uniquePatientIds.size());
        allPatients.put("totalVisits", appointments.size());
        result.put("allPatients", allPatients);

        // Department-wise
        Map<String, Long> deptWise = appointments.stream()
                .filter(a -> a.getDepartment() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getDepartment().getName(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> deptReport = deptWise.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("department", e.getKey());
                    m.put("patientCount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("departmentWise", deptReport);

        // Doctor-wise
        Map<String, Long> docWise = appointments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDoctor().getFullName(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> docReport = docWise.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("doctor", e.getKey());
                    m.put("patientCount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("doctorWise", docReport);

        // OPD Patients (in_person + follow_up + virtual)
        long opdCount = appointments.stream()
                .filter(a -> a.getAppointmentType() != Appointment.AppointmentType.emergency)
                .count();

        Map<String, Object> opdReport = new LinkedHashMap<>();
        opdReport.put("type", "OPD");
        opdReport.put("totalPatients", opdCount);
        opdReport.put("description", "Out-patient department visits (in-person, virtual, follow-up)");
        result.put("totalOPD", opdReport);

        // IPD Patients (emergency - treated as in-patient / admission indicator)
        long ipdCount = appointments.stream()
                .filter(a -> a.getAppointmentType() == Appointment.AppointmentType.emergency)
                .count();

        Map<String, Object> ipdReport = new LinkedHashMap<>();
        ipdReport.put("type", "IPD");
        ipdReport.put("totalPatients", ipdCount);
        ipdReport.put("description", "In-patient department visits (emergency admissions)");
        result.put("totalIPD", ipdReport);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private List<Appointment> getAppointmentsInRange(UUID hospitalId, LocalDate start, LocalDate end) {
        if (hospitalId != null) {
            // Filter appointments within date range for this hospital
            return appointmentRepo.findByHospitalOrDoctorHospitalId(hospitalId).stream()
                    .filter(a -> !a.getAppointmentDate().isBefore(start) && !a.getAppointmentDate().isAfter(end))
                    .collect(Collectors.toList());
        }
        return appointmentRepo.findByDateRange(start, end);
    }
}
