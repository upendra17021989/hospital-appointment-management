package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.security.TenantContext;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;

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
        List<Appointment> opdAppointments = appointments.stream()
                .filter(a -> a.getAppointmentType() != Appointment.AppointmentType.emergency)
                .collect(Collectors.toList());

        long opdCount = opdAppointments.size();

        Map<String, Object> opdReport = new LinkedHashMap<>();
        opdReport.put("type", "OPD");
        opdReport.put("totalPatients", opdCount);
        opdReport.put("description", "Out-patient department visits (in-person, virtual, follow-up)");
        opdReport.put("patientsList", buildPatientsList(opdAppointments));
        result.put("totalOPD", opdReport);

        // IPD Patients (emergency - treated as in-patient / admission indicator)
        List<Appointment> ipdAppointments = appointments.stream()
                .filter(a -> a.getAppointmentType() == Appointment.AppointmentType.emergency)
                .collect(Collectors.toList());

        long ipdCount = ipdAppointments.size();

        Map<String, Object> ipdReport = new LinkedHashMap<>();
        ipdReport.put("type", "IPD");
        ipdReport.put("totalPatients", ipdCount);
        ipdReport.put("description", "In-patient department visits (emergency admissions)");
        ipdReport.put("patientsList", buildPatientsList(ipdAppointments));
        result.put("totalIPD", ipdReport);


        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/patients/by-department")
    @Operation(summary = "Get patient visit records between dates for a specific department")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDepartmentPatients(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String department) {

        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        List<Appointment> deptAppointments = appointments.stream()
                .filter(a -> a.getDepartment() != null && department.equals(a.getDepartment().getName()))
                .collect(Collectors.toList());

        // Pre-compute visit counts per patient
        Map<UUID, Long> visitCountByPatientId = deptAppointments.stream()
                .collect(Collectors.groupingBy(a -> a.getPatient().getId(), Collectors.counting()));

        // last visit per unique patient
        Map<UUID, Appointment> lastVisitByPatient = new LinkedHashMap<>();
        for (Appointment a : deptAppointments) {
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
                    LocalDate d1 = (LocalDate) m1.get("lastVisitDate");
                    LocalDate d2 = (LocalDate) m2.get("lastVisitDate");
                    if (d1 != null && d2 != null) {
                        return d2.compareTo(d1);
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("department", department);
        result.put("patientsList", patientsList);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/patients/by-doctor")
    @Operation(summary = "Get patient visit records between dates for a specific doctor")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorPatients(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String doctor) {


        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        List<Appointment> docAppointments = appointments.stream()
                .filter(a -> a.getDoctor() != null && doctor.equals(a.getDoctor().getFullName()))
                .collect(Collectors.toList());

        // Pre-compute visit counts per patient
        Map<UUID, Long> visitCountByPatientId = docAppointments.stream()
                .collect(Collectors.groupingBy(a -> a.getPatient().getId(), Collectors.counting()));

        // last visit per unique patient
        Map<UUID, Appointment> lastVisitByPatient = new LinkedHashMap<>();
        for (Appointment a : docAppointments) {
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
                    LocalDate d1 = (LocalDate) m1.get("lastVisitDate");
                    LocalDate d2 = (LocalDate) m2.get("lastVisitDate");
                    if (d1 != null && d2 != null) {
                        return d2.compareTo(d1);
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("doctor", doctor);
        result.put("patientsList", patientsList);

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
        Table table = new Table(new float[]{1.2f, 3f, 1.2f, 1.6f, 2f, 2.2f, 1.2f});




        table.addHeaderCell(new Cell().add(new Paragraph("#")));
        table.addHeaderCell(new Cell().add(new Paragraph("Patient")));
        table.addHeaderCell(new Cell().add(new Paragraph("Age")));
        table.addHeaderCell(new Cell().add(new Paragraph("Gender")));
        table.addHeaderCell(new Cell().add(new Paragraph("Phone")));
        table.addHeaderCell(new Cell().add(new Paragraph("Last Visit")));
        table.addHeaderCell(new Cell().add(new Paragraph("Visits")));

        for (int i = 0; i < patientsList.size(); i++) {
            Map<String, Object> row = patientsList.get(i);

            UUID pid = (UUID) row.get("patientId");
            String patientName = String.valueOf(row.get("patientName"));
            Integer age = (Integer) row.get("age");
            String gender = String.valueOf(row.get("gender"));
            String phone = String.valueOf(row.get("phone"));
            LocalDate lastVisitDate = (LocalDate) row.get("lastVisitDate");
            Object lastVisitTime = row.get("lastVisitTime");
            Long visitCount = (Long) row.get("visitCount");

            String lastVisit = "";
            if (lastVisitDate != null) {
                lastVisit = lastVisitDate.toString();
            }
            if (lastVisitTime != null) {
                lastVisit = lastVisit + (lastVisit.isEmpty() ? "" : " · ") + String.valueOf(lastVisitTime);
            }

            table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))));
            table.addCell(new Cell().add(new Paragraph(patientName + (pid != null ? " (ID: " + pid + ")" : ""))));
            table.addCell(new Cell().add(new Paragraph(age == null ? "—" : String.valueOf(age))));
            table.addCell(new Cell().add(new Paragraph(gender == null ? "—" : gender)));
            table.addCell(new Cell().add(new Paragraph(phone == null ? "—" : phone)));
            table.addCell(new Cell().add(new Paragraph(lastVisit.isEmpty() ? "—" : lastVisit)));
            table.addCell(new Cell().add(new Paragraph(visitCount == null ? "0" : String.valueOf(visitCount))));
        }

        document.add(table);
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

    private static String formatLastVisit(LocalDate d, Object time) {
        if (d == null && time == null) return "—";
        String datePart = d == null ? "" : d.toString();
        String timePart = time == null ? "" : String.valueOf(time);
        if (datePart.isEmpty()) return timePart;
        if (timePart.isEmpty()) return datePart;
        return datePart + " · " + timePart;
    }

    private List<Map<String, Object>> buildPatientsList(List<Appointment> appointments) {
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

        return lastVisitByPatient.values().stream()
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
    }

    @GetMapping("/patients/opd/download/pdf")
    @Operation(summary = "Download OPD patient visit report between dates (PDF)")
    public ResponseEntity<byte[]> downloadOpdPatientsReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        List<Appointment> opdAppointments = appointments.stream()
                .filter(a -> a.getAppointmentType() != Appointment.AppointmentType.emergency)
                .collect(Collectors.toList());

        String title = "OPD Patient Visit Report";
        List<Map<String, Object>> patientsList = buildPatientsList(opdAppointments);

        return exportPatientsTablePdf(startDate, endDate, title, patientsList);
    }

    @GetMapping("/patients/ipd/download/pdf")
    @Operation(summary = "Download IPD patient visit report between dates (PDF)")
    public ResponseEntity<byte[]> downloadIpdPatientsReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        List<Appointment> ipdAppointments = appointments.stream()
                .filter(a -> a.getAppointmentType() == Appointment.AppointmentType.emergency)
                .collect(Collectors.toList());

        String title = "IPD Patient Visit Report";
        List<Map<String, Object>> patientsList = buildPatientsList(ipdAppointments);

        return exportPatientsTablePdf(startDate, endDate, title, patientsList);
    }

     private void fillPatientsTable(Table table, List<Map<String, Object>> patientsList) {
        for (int i = 0; i < patientsList.size(); i++) {
            Map<String, Object> row = patientsList.get(i);

            UUID pid = (UUID) row.get("patientId");
            String patientName = String.valueOf(row.get("patientName"));
            Integer age = (Integer) row.get("age");
            String gender = String.valueOf(row.get("gender"));
            String phone = String.valueOf(row.get("phone"));
            LocalDate lastVisitDate = (LocalDate) row.get("lastVisitDate");
            Object lastVisitTime = row.get("lastVisitTime");
            Long visitCount = (Long) row.get("visitCount");

            table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))));
            table.addCell(new Cell().add(new Paragraph(patientName + (pid != null ? " (ID: " + pid + ")" : ""))));
            table.addCell(new Cell().add(new Paragraph(age == null ? "—" : String.valueOf(age))));
            table.addCell(new Cell().add(new Paragraph(gender == null ? "—" : gender)));
            table.addCell(new Cell().add(new Paragraph(phone == null ? "—" : phone)));
            table.addCell(new Cell().add(new Paragraph(formatLastVisit(lastVisitDate, lastVisitTime))));

            table.addCell(new Cell().add(new Paragraph(visitCount == null ? "0" : String.valueOf(visitCount))));
        }
    }

    // ===== Department Wise PDF Download =====
    @GetMapping("/patients/by-department/download/pdf")
    @Operation(summary = "Download patient visit report between dates for a specific department (PDF)")
    public ResponseEntity<byte[]> downloadDepartmentPatientsReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String department) {

        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        List<Appointment> deptAppointments = appointments.stream()
                .filter(a -> a.getDepartment() != null && department.equals(a.getDepartment().getName()))
                .collect(Collectors.toList());

        Map<UUID, Appointment> lastVisitByPatient = new LinkedHashMap<>();
        for (Appointment a : deptAppointments) {
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

        Map<UUID, Long> visitCountByPatientId = deptAppointments.stream()
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

        String title = "Patient Visit Report - " + department;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(title));
        document.add(new Paragraph("Start Date: " + startDate));
        document.add(new Paragraph("End Date: " + endDate));
        document.add(new Paragraph(" "));

        Table table = new Table(new float[]{1.2f, 3f, 1.2f, 1.6f, 2f, 2.2f, 1.2f});
        table.addHeaderCell(new Cell().add(new Paragraph("#")));
        table.addHeaderCell(new Cell().add(new Paragraph("Patient")));
        table.addHeaderCell(new Cell().add(new Paragraph("Age")));
        table.addHeaderCell(new Cell().add(new Paragraph("Gender")));
        table.addHeaderCell(new Cell().add(new Paragraph("Phone")));
        table.addHeaderCell(new Cell().add(new Paragraph("Last Visit")));
        table.addHeaderCell(new Cell().add(new Paragraph("Visits")));

        for (int i = 0; i < patientsList.size(); i++) {
            Map<String, Object> row = patientsList.get(i);

            UUID pid = (UUID) row.get("patientId");
            String patientName = String.valueOf(row.get("patientName"));
            Integer age = (Integer) row.get("age");
            String gender = String.valueOf(row.get("gender"));
            String phone = String.valueOf(row.get("phone"));
            LocalDate lastVisitDate = (LocalDate) row.get("lastVisitDate");
            Object lastVisitTime = row.get("lastVisitTime");
            Long visitCount = (Long) row.get("visitCount");

            String lastVisit = formatLastVisit(lastVisitDate, lastVisitTime);

            table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))));
            table.addCell(new Cell().add(new Paragraph(patientName + (pid != null ? " (ID: " + pid + ")" : ""))));
            table.addCell(new Cell().add(new Paragraph(age == null ? "—" : String.valueOf(age))));
            table.addCell(new Cell().add(new Paragraph(gender == null ? "—" : gender)));
            table.addCell(new Cell().add(new Paragraph(phone == null ? "—" : phone)));
            table.addCell(new Cell().add(new Paragraph(lastVisit)));
            table.addCell(new Cell().add(new Paragraph(visitCount == null ? "0" : String.valueOf(visitCount))));
        }

        document.add(table);
        document.close();

        byte[] bytes = out.toByteArray();
        String filename = "department-patient-report-" + startDate + "-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    // ===== Doctor Wise PDF Download =====
    @GetMapping("/patients/by-doctor/download/pdf")
    @Operation(summary = "Download patient visit report between dates for a specific doctor (PDF)")
    public ResponseEntity<byte[]> downloadDoctorPatientsReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String doctor) {

        UUID hospitalId = tenantContext.getCurrentHospitalId().orElse(null);
        List<Appointment> appointments = getAppointmentsInRange(hospitalId, startDate, endDate);

        List<Appointment> docAppointments = appointments.stream()
                .filter(a -> a.getDoctor() != null && doctor.equals(a.getDoctor().getFullName()))
                .collect(Collectors.toList());

        Map<UUID, Appointment> lastVisitByPatient = new LinkedHashMap<>();
        for (Appointment a : docAppointments) {
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

        Map<UUID, Long> visitCountByPatientId = docAppointments.stream()
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

        String title = "Patient Visit Report - " + doctor;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(title));
        document.add(new Paragraph("Start Date: " + startDate));
        document.add(new Paragraph("End Date: " + endDate));
        document.add(new Paragraph(" "));

        Table table = new Table(new float[]{1.2f, 3f, 1.2f, 1.6f, 2f, 2.2f, 1.2f});
        table.addHeaderCell(new Cell().add(new Paragraph("#")));
        table.addHeaderCell(new Cell().add(new Paragraph("Patient")));
        table.addHeaderCell(new Cell().add(new Paragraph("Age")));
        table.addHeaderCell(new Cell().add(new Paragraph("Gender")));
        table.addHeaderCell(new Cell().add(new Paragraph("Phone")));
        table.addHeaderCell(new Cell().add(new Paragraph("Last Visit")));
        table.addHeaderCell(new Cell().add(new Paragraph("Visits")));

        for (int i = 0; i < patientsList.size(); i++) {
            Map<String, Object> row = patientsList.get(i);

            UUID pid = (UUID) row.get("patientId");
            String patientName = String.valueOf(row.get("patientName"));
            Integer age = (Integer) row.get("age");
            String gender = String.valueOf(row.get("gender"));
            String phone = String.valueOf(row.get("phone"));
            LocalDate lastVisitDate = (LocalDate) row.get("lastVisitDate");
            Object lastVisitTime = row.get("lastVisitTime");
            Long visitCount = (Long) row.get("visitCount");

            String lastVisit = formatLastVisit(lastVisitDate, lastVisitTime);

            table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))));
            table.addCell(new Cell().add(new Paragraph(patientName + (pid != null ? " (ID: " + pid + ")" : ""))));
            table.addCell(new Cell().add(new Paragraph(age == null ? "—" : String.valueOf(age))));
            table.addCell(new Cell().add(new Paragraph(gender == null ? "—" : gender)));
            table.addCell(new Cell().add(new Paragraph(phone == null ? "—" : phone)));
            table.addCell(new Cell().add(new Paragraph(lastVisit)));
            table.addCell(new Cell().add(new Paragraph(visitCount == null ? "0" : String.valueOf(visitCount))));
        }

        document.add(table);
        document.close();

        byte[] bytes = out.toByteArray();
        String filename = "doctor-patient-report-" + startDate + "-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    private ResponseEntity<byte[]> exportPatientsTablePdf(

            LocalDate startDate,
            LocalDate endDate,
            String title,
            List<Map<String, Object>> patientsList) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(title));
        document.add(new Paragraph("Start Date: " + startDate));
        document.add(new Paragraph("End Date: " + endDate));
        document.add(new Paragraph(" "));
        Table table = new Table(new float[]{1.2f, 3f, 1.2f, 1.6f, 2f, 2.2f, 1.2f});


        table.addHeaderCell(new Cell().add(new Paragraph("#")));
        table.addHeaderCell(new Cell().add(new Paragraph("Patient")));
        table.addHeaderCell(new Cell().add(new Paragraph("Age")));
        table.addHeaderCell(new Cell().add(new Paragraph("Gender")));
        table.addHeaderCell(new Cell().add(new Paragraph("Phone")));
        table.addHeaderCell(new Cell().add(new Paragraph("Last Visit")));
        table.addHeaderCell(new Cell().add(new Paragraph("Visits")));

        fillPatientsTable(table, patientsList);

        document.add(table);
        document.close();

        byte[] bytes = out.toByteArray();
        String filename = title.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + startDate + "-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

}
