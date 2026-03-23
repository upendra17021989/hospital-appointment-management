package com.hospital.service;

import com.hospital.dto.Dtos.*;
import com.hospital.model.*;
import com.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepo appointmentRepo;
    private final DoctorRepo doctorRepo;
    private final PatientRepo patientRepo;
    private final DoctorScheduleRepo scheduleRepo;

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        Doctor doctor = doctorRepo.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Patient patient = patientRepo.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Validate availability
        validateSlotAvailability(request.getDoctorId(), request.getAppointmentDate(), request.getAppointmentTime());

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .department(doctor.getDepartment())
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .reasonForVisit(request.getReasonForVisit())
                .symptoms(request.getSymptoms())
                .notes(request.getNotes())
                .appointmentType(request.getAppointmentType() != null
                        ? request.getAppointmentType()
                        : Appointment.AppointmentType.in_person)
                .status(Appointment.Status.pending)
                .build();

        Appointment saved = appointmentRepo.save(appointment);
        return mapToResponse(saved);
    }

    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepo.findAll().stream().map(this::mapToResponse).toList();
    }

    public Optional<AppointmentResponse> getAppointmentById(UUID id) {
        return appointmentRepo.findById(id).map(this::mapToResponse);
    }

    public List<AppointmentResponse> getAppointmentsByPatient(UUID patientId) {
        return appointmentRepo.findByPatientId(patientId).stream().map(this::mapToResponse).toList();
    }

    public List<AppointmentResponse> getAppointmentsByDate(LocalDate date) {
        return appointmentRepo.findByAppointmentDate(date).stream().map(this::mapToResponse).toList();
    }

    public List<AppointmentResponse> getAppointmentsByDoctor(UUID doctorId) {
        return appointmentRepo.findByDoctorId(doctorId).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID id, AppointmentStatusUpdateRequest req) {
        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus(req.getStatus());
        if (req.getNotes() != null) appointment.setNotes(req.getNotes());
        if (req.getCancellationReason() != null) appointment.setCancellationReason(req.getCancellationReason());

        return mapToResponse(appointmentRepo.save(appointment));
    }

    public List<TimeSlot> getAvailableSlots(UUID doctorId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // Java: Monday=1, we need Sunday=0
        List<DoctorSchedule> schedules = scheduleRepo.findByDoctorIdAndDayOfWeekAndIsActiveTrue(doctorId, dayOfWeek);

        if (schedules.isEmpty()) return Collections.emptyList();

        List<Appointment> existingAppointments = appointmentRepo.findByDoctorIdAndAppointmentDate(doctorId, date);
        Set<LocalTime> bookedTimes = new HashSet<>();
        for (Appointment a : existingAppointments) {
            if (a.getStatus() != Appointment.Status.cancelled) {
                bookedTimes.add(a.getAppointmentTime());
            }
        }

        List<TimeSlot> slots = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (DoctorSchedule schedule : schedules) {
            LocalTime current = schedule.getStartTime();
            while (current.plusMinutes(schedule.getSlotDurationMinutes()).compareTo(schedule.getEndTime()) <= 0) {
                boolean available = !bookedTimes.contains(current) && current.isAfter(LocalTime.now().minusMinutes(1));
                slots.add(TimeSlot.builder()
                        .time(current)
                        .available(available)
                        .displayTime(current.format(formatter))
                        .build());
                current = current.plusMinutes(schedule.getSlotDurationMinutes());
            }
        }
        return slots;
    }

    private void validateSlotAvailability(UUID doctorId, LocalDate date, LocalTime time) {
        List<Appointment> existing = appointmentRepo.findByDoctorIdAndAppointmentDate(doctorId, date);
        boolean slotTaken = existing.stream()
                .anyMatch(a -> a.getAppointmentTime().equals(time)
                        && a.getStatus() != Appointment.Status.cancelled);
        if (slotTaken) throw new RuntimeException("This time slot is already booked");
    }

    private AppointmentResponse mapToResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patient(mapPatient(a.getPatient()))
                .doctor(mapDoctor(a.getDoctor()))
                .department(a.getDepartment() != null ? mapDepartment(a.getDepartment()) : null)
                .appointmentDate(a.getAppointmentDate())
                .appointmentTime(a.getAppointmentTime())
                .durationMinutes(a.getDurationMinutes())
                .status(a.getStatus())
                .appointmentType(a.getAppointmentType())
                .reasonForVisit(a.getReasonForVisit())
                .symptoms(a.getSymptoms())
                .notes(a.getNotes())
                .tokenNumber(a.getTokenNumber())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private PatientResponse mapPatient(Patient p) {
        if (p == null) return null;
        return PatientResponse.builder()
                .id(p.getId()).firstName(p.getFirstName()).lastName(p.getLastName())
                .fullName(p.getFullName()).dateOfBirth(p.getDateOfBirth()).gender(p.getGender())
                .phone(p.getPhone()).email(p.getEmail()).build();
    }

    private DoctorResponse mapDoctor(Doctor d) {
        if (d == null) return null;
        return DoctorResponse.builder()
                .id(d.getId()).firstName(d.getFirstName()).lastName(d.getLastName())
                .fullName(d.getFullName()).specialization(d.getSpecialization())
                .qualification(d.getQualification()).experienceYears(d.getExperienceYears())
                .consultationFee(d.getConsultationFee()).isAvailable(d.getIsAvailable())
                .department(d.getDepartment() != null ? mapDepartment(d.getDepartment()) : null)
                .build();
    }

    private DepartmentResponse mapDepartment(Department dept) {
        if (dept == null) return null;
        return DepartmentResponse.builder()
                .id(dept.getId()).name(dept.getName())
                .description(dept.getDescription()).floorNumber(dept.getFloorNumber())
                .phone(dept.getPhone()).email(dept.getEmail()).build();
    }
}
