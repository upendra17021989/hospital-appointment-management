package com.hospital.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hospital.model.Appointment;
import com.hospital.model.Enquiry;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dtos {

    // ========== DEPARTMENT ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DepartmentResponse {
        private UUID id;
        private String name;
        private String description;
        private Integer floorNumber;
        private String phone;
        private String email;
        private Boolean isActive;
        private Integer doctorCount;
    }

    // ========== DOCTOR ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DoctorResponse {
        private UUID id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String specialization;
        private String qualification;
        private Integer experienceYears;
        private String phone;
        private String email;
        private String bio;
        private String profileImageUrl;
        private BigDecimal consultationFee;
        private Boolean isAvailable;
        private String[] languagesSpoken;
        private DepartmentResponse department;
    }

    // ========== PATIENT REQUEST ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PatientRequest {
        @NotBlank(message = "First name is required")
        @Size(max = 50)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 50)
        private String lastName;

        private LocalDate dateOfBirth;
        private Integer age;

        @Pattern(regexp = "male|female|other", message = "Gender must be male, female, or other")
        private String gender;

        @Pattern(regexp = "^$|^[+]?[0-9]{10,15}$", message = "Invalid phone number (if provided)")
        private String phone;

        @Email(message = "Invalid email address")
        private String email;

        private String address;
        private String bloodGroup;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String medicalHistory;
        private String allergies;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PatientResponse {
        private UUID id;
        private String firstName;
        private String lastName;
        private String fullName;
        private LocalDate dateOfBirth;
        private Integer age;
        private String gender;
        private String phone;
        private String email;
        private String address;
        private String bloodGroup;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String medicalHistory;
        private String allergies;
        private LocalDateTime createdAt;
    }

    // ========== APPOINTMENT REQUEST ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppointmentRequest {
        @NotNull(message = "Patient ID is required")
        private UUID patientId;

        @NotNull(message = "Doctor ID is required")
        private UUID doctorId;

        @NotNull(message = "Appointment date is required")
        private LocalDate appointmentDate;

        @NotNull(message = "Appointment time is required")
        private LocalTime appointmentTime;

        @NotBlank(message = "Reason for visit is required")
        private String reasonForVisit;

        private String symptoms;
        private String notes;
        private Appointment.AppointmentType appointmentType;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppointmentResponse {
        private UUID id;
        private PatientResponse patient;
        private DoctorResponse doctor;
        private DepartmentResponse department;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private Integer durationMinutes;
        private Appointment.Status status;
        private Appointment.AppointmentType appointmentType;
        private String reasonForVisit;
        private String symptoms;
        private String notes;
        private String tokenNumber;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppointmentStatusUpdateRequest {
        @NotNull(message = "Status is required")
        private Appointment.Status status;
        private String notes;
        private String cancellationReason;
    }

    // ========== ENQUIRY REQUEST ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EnquiryRequest {
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        private String name;

        @Email(message = "Invalid email address")
        private String email;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
        private String phone;

        @NotBlank(message = "Subject is required")
        @Size(max = 200)
        private String subject;

        @NotBlank(message = "Message is required")
        private String message;

        private UUID departmentId;
        private Enquiry.EnquiryType enquiryType;
        private Enquiry.Priority priority;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EnquiryResponse {
        private UUID id;
        private String name;
        private String email;
        private String phone;
        private String subject;
        private String message;
        private DepartmentResponse department;
        private Enquiry.EnquiryType enquiryType;
        private Enquiry.Status status;
        private Enquiry.Priority priority;
        private String assignedTo;
        private String response;
        private LocalDateTime resolvedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ========== DASHBOARD STATS ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardStats {
        private Long totalAppointments;
        private Long todayAppointments;
        private Long pendingAppointments;
        private Long confirmedAppointments;
        private Long completedAppointments;
        private Long cancelledAppointments;
        private Long openEnquiries;
        private Long totalPatients;
        private Long totalDoctors;
        private Long totalDepartments;
    }

    // ========== TIME SLOT ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TimeSlot {
        private LocalTime time;
        private Boolean available;
        private String displayTime;
    }

    // ========== SUBSCRIPTION PLAN ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionPlanResponse {
        private UUID id;
        private String name;
        private String slug;
        private String description;
        private BigDecimal monthlyPrice;
        private BigDecimal yearlyPrice;
        private Integer maxDoctors;
        private Integer maxUsers;
        private Integer maxAppointmentsPerMonth;
        private Boolean allowPrescriptions;
        private Boolean allowSms;
        private Boolean allowWhatsapp;
        private Boolean allowCustomBranding;
        private Boolean prioritySupport;
        private Boolean isActive;
    }

    // ========== HOSPITAL SUBSCRIPTION ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionResponse {
        private UUID id;
        private SubscriptionPlanResponse plan;
        private String status;
        private String billingCycle;
        private LocalDateTime trialEndsAt;
        private LocalDateTime currentPeriodStart;
        private LocalDateTime currentPeriodEnd;
        private LocalDateTime cancelledAt;
        private Boolean isTrial;
        private Boolean isExpired;
        private Integer daysUntilExpiry;
    }

    // ========== PAYMENT ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentResponse {
        private UUID id;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String description;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
    }

    // ========== CHECKOUT SESSION ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CheckoutSessionResponse {
        private String checkoutUrl;
    }

    // ========== SUBSCRIPTION USAGE ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionUsageResponse {
        private Integer doctorsUsed;
        private Integer doctorsLimit;
        private Integer usersUsed;
        private Integer usersLimit;
        private Long appointmentsThisMonth;
        private Integer appointmentsLimit;
        private Boolean prescriptionsEnabled;
    }

    // ========== API RESPONSE WRAPPER ===========
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApiResponse<T> {
        private Boolean success;
        private String message;
        private T data;
        private Object errors;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
