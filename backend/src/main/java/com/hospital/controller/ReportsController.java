package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.security.TenantContext;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
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

        // All Patients (aggregate)
        Map<String, Object> allPatients = new LinkedHashMap<>();
        allPatients.put("totalUniquePatients", uniquePatientIds.size());
        allPatients.put("totalVisits", appointments.size());
        result.put("allPatients", allPatients);

        // Patients list (basic details) — used for table rendering on frontend
        // We show one row per unique patient, plus their last visit date/time within the range.
        Map<UUID, Appointment> lastVisitByPatient = new LinkedHashMap<>();
        for (Appointment a : appointments) {
            UUID pid = a.getPatient().getId();
            Appointment current = lastVisitByPatient.get(pid);
            if (current == null) {
                lastVisitByPatient.put(pid, a);
            } else {
                // compare by date then time
                if (a.getAppointmentDate().isAfter(current.getAppointmentDate())
                        || (a.getAppointmentDate().isEqual(current.getAppointmentDate())
                        && a.getAppointmentTime().compareTo(current.getAppointmentTime()) > 0)) {
                    lastVisitByPatient.put(pid, a);
                }
            }
        }

        // Pre-compute visit counts per patient to avoid repeated O(n^2) counting
        Map<UUID, Long> visitCountByPatientId = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getPatient().getId(), Collectors.counting()));

        List<Map<String, Object>> patientsList = lastVisitByPatient.values().stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    Patient p = a.getPatient();

                    long visitCount = visitCountByPatientId.getOrDefault(p.getId(), 0L);

                    m.put("patientId", p.getId());
                    m.put("patientName", p.getFullName());
                    m.put("age", p.getAge());
                    m.put("gender", p.getGender());
                    m.put("phone", p.getPhone());
                    m.put("lastVisitDate", a.getAppointmentDate());
                    m.put("lastVisitTime", a.getAppointmentTime());
                    m.put("visitCount", visitCount);
                    return m;
                })
                .sorted((m1, m2) -> {
                    // sort by lastVisitDate desc
                    LocalDate d1 = (LocalDate) m1.get("lastVisitDate");
                    LocalDate d2 = (LocalDate) m2.get("lastVisitDate");
                    if (d1 != null && d2 != null) {
                        return d2.compareTo(d1);
                    }
                    return 0;
                })
                .collect(Collectors.toList());


        result.put("patientsList", patientsList);


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

    @GetMapping("/patients/download")
    @Operation(summary = "Download patient visit report between dates (CSV)")
    public ResponseEntity<byte[]> downloadPatientsReport(

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // returns CSV


        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        // Unique patients
        Set<UUID> uniquePatientIds = appointments.stream()
                .map(a -> a.getPatient().getId())
                .collect(Collectors.toSet());

        Map<UUID, Appointment> lastVisitByPatient = new LinkedHashMap<>();
        for (Appointment a : appointments) {
            UUID pid = a.getPatient().getId();
            Appointment current = lastVisitByPatient.get(pid);
            if (current == null) {
                lastVisitByPatient.put(pid, a);
            } else {
                if (a.getAppointmentDate().isAfter(current.getAppointmentDate())
                        || (a.getAppointmentDate().isEqual(current.getAppointmentDate())
                        && a.getAppointmentTime().compareTo(current.getAppointmentTime()) > 0)) {
                    lastVisitByPatient.put(pid, a);
                }
            }
        }

        Map<UUID, Long> visitCountByPatientId = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getPatient().getId(), Collectors.counting()));

        List<Map<String, Object>> patientsList = lastVisitByPatient.values().stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    Patient p = a.getPatient();
                    m.put("patientId", p.getId());
                    m.put("patientName", p.getFullName());
                    m.put("age", p.getAge());
                    m.put("gender", p.getGender());
                    m.put("phone", p.getPhone());
                    m.put("lastVisitDate", a.getAppointmentDate());
                    m.put("lastVisitTime", a.getAppointmentTime());
                    m.put("visitCount", visitCountByPatientId.getOrDefault(p.getId(), 0L));
                    return m;
                })
                .sorted((m1, m2) -> {
                    LocalDate d1 = (LocalDate) m1.get("lastVisitDate");
                    LocalDate d2 = (LocalDate) m2.get("lastVisitDate");
                    if (d1 != null && d2 != null) return d2.compareTo(d1);
                    return 0;
                })
                .collect(Collectors.toList());

        // Build CSV
        StringBuilder sb = new StringBuilder();
        sb.append("PatientId,PatientName,Age,Gender,Phone,LastVisitDate,LastVisitTime,VisitCount\n");
        for (Map<String, Object> row : patientsList) {
            UUID pid = (UUID) row.get("patientId");
            String patientName = String.valueOf(row.get("patientName"));
            Integer age = (Integer) row.get("age");
            String gender = String.valueOf(row.get("gender"));
            String phone = String.valueOf(row.get("phone"));
            LocalDate lastVisitDate = (LocalDate) row.get("lastVisitDate");
            Object lastVisitTime = row.get("lastVisitTime");
            Long visitCount = (Long) row.get("visitCount");

            // CSV escape minimal (quotes)
            sb.append(pid).append(",")
                    .append('"').append(patientName.replace("\"", "\"\"")).append("\"").append(",")
                    .append(age == null ? "" : age).append(",")
                    .append('"').append(gender == null ? "" : gender.replace("\"", "\"\"")).append("\"").append(",")
                    .append('"').append(phone == null ? "" : phone.replace("\"", "\"\"")).append("\"").append(",")
                    .append(lastVisitDate == null ? "" : lastVisitDate).append(",")
                    .append(lastVisitTime == null ? "" : lastVisitTime).append(",")
                    .append(visitCount == null ? "" : visitCount)
                    .append("\n");
        }

        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "patient-report-" + startDate + "-" + endDate + ".csv";

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    @GetMapping("/patients/download/pdf")
    @Operation(summary = "Download patient visit report between dates (PDF)")
    public ResponseEntity<byte[]> downloadPatientsReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        Map<UUID, Appointment> lastVisitByPatient = new LinkedHashMap<>();
        for (Appointment a : appointments) {
            UUID pid = a.getPatient().getId();
            Appointment current = lastVisitByPatient.get(pid);
            if (current == null) {
                lastVisitByPatient.put(pid, a);
            } else {
                if (a.getAppointmentDate().isAfter(current.getAppointmentDate())
                        || (a.getAppointmentDate().isEqual(current.getAppointmentDate())
                        && a.getAppointmentTime().compareTo(current.getAppointmentTime()) > 0)) {
                    lastVisitByPatient.put(pid, a);
                }
            }
        }

        Map<UUID, Long> visitCountByPatientId = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getPatient().getId(), Collectors.counting()));

        List<Map<String, Object>> patientsList = lastVisitByPatient.values().stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    Patient p = a.getPatient();
                    m.put("patientId", p.getId());
                    m.put("patientName", p.getFullName());
                    m.put("age", p.getAge());
                    m.put("gender", p.getGender());
                    m.put("phone", p.getPhone());
                    m.put("lastVisitDate", a.getAppointmentDate());
                    m.put("lastVisitTime", a.getAppointmentTime());
                    m.put("visitCount", visitCountByPatientId.getOrDefault(p.getId(), 0L));
                    return m;
                })
                .sorted((m1, m2) -> {
                    LocalDate d1 = (LocalDate) m1.get("lastVisitDate");
                    LocalDate d2 = (LocalDate) m2.get("lastVisitDate");
                    if (d1 != null && d2 != null) return d2.compareTo(d1);
                    return 0;
                })
                .toList();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Patient Visit Report"));
        document.add(new Paragraph("Start Date: " + startDate));
        document.add(new Paragraph("End Date: " + endDate));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Patients:"));
        for (Map<String, Object> row : patientsList) {
            UUID pid = (UUID) row.get("patientId");
            String patientName = String.valueOf(row.get("patientName"));
            Integer age = (Integer) row.get("age");
            String gender = String.valueOf(row.get("gender"));
            String phone = String.valueOf(row.get("phone"));
            LocalDate lastVisitDate = (LocalDate) row.get("lastVisitDate");
            Object lastVisitTime = row.get("lastVisitTime");
            Long visitCount = (Long) row.get("visitCount");

            document.add(new Paragraph(
                    "- " + patientName + " (ID: " + pid + ")" +
                            " | Age: " + (age == null ? "" : age) +
                            " | Gender: " + (gender == null ? "" : gender) +
                            " | Phone: " + (phone == null ? "" : phone) +
                            " | Last Visit: " + (lastVisitDate == null ? "" : lastVisitDate) +
                            (lastVisitTime == null ? "" : (" " + lastVisitTime)) +
                            " | Visits: " + (visitCount == null ? "" : visitCount)
            ));
        }

        document.close();
        byte[] bytes = out.toByteArray();
        String filename = "patient-report-" + startDate + "-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bytes);
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
