package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.Hospital;
import com.hospital.model.User;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.UserRepo;
import com.hospital.security.RequireHospitalContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.hospital.security.TenantContext;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD operations and role management")
public class UserController {

    private final UserRepo userRepo;
    private final HospitalRepo hospitalRepo;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;

    // ── DTOs ──────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserDto {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String role;
        private Boolean isActive;
        private HospitalInfo hospital;
        private String createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HospitalInfo {
        private UUID id;
        private String name;
        private String city;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CreateUserRequest {
        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        private User.Role role = User.Role.STAFF;
        private UUID hospitalId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateUserRequest {
        private String firstName;
        private String lastName;
        private String email;
        private User.Role role;
        private Boolean isActive;
        private String password; // Optional - only if changing password
    }

    // ── GET /users - List users ───────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HOSPITAL_ADMIN')")
    @RequireHospitalContext
    @Operation(summary = "Get all users (filtered by hospital for HOSPITAL_ADMIN)")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<User> usersPage;
        if (tenantContext.isSuperAdmin()) {
            // SUPER_ADMIN can see all users
            usersPage = userRepo.findAll(pageable);
        } else {
            // HOSPITAL_ADMIN can only see users from their hospital
            UUID hospitalId = tenantContext.requireHospitalId();
            usersPage = userRepo.findByHospitalId(hospitalId, pageable);
        }

        List<UserDto> userDtos = usersPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(userDtos));
    }

    // ── POST /users - Create user ────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HOSPITAL_ADMIN')")
    @RequireHospitalContext
    @Operation(summary = "Create a new user")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        // Security: Prevent SUPER_ADMIN role assignment through API
        if (request.getRole() == User.Role.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SUPER_ADMIN role cannot be assigned through this interface. Use database access for security."));
        }

        // Check if email already exists
        if (userRepo.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("An account with this email already exists."));
        }

        Hospital hospital;
        if (tenantContext.isSuperAdmin() && request.getHospitalId() != null) {
            // SUPER_ADMIN can specify hospital
            hospital = hospitalRepo.findById(request.getHospitalId())
                    .orElseThrow(() -> new RuntimeException("Hospital not found"));
        } else {
            // HOSPITAL_ADMIN: force current hospital; SUPER_ADMIN without id: no hospital
            UUID currentHospitalId = tenantContext.requireHospitalId();
            hospital = hospitalRepo.findById(currentHospitalId)
                    .orElseThrow(() -> new RuntimeException("Current hospital not found"));
        }

        User user = User.builder()
                .hospital(hospital)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();

        User savedUser = userRepo.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToDto(savedUser)));
    }

    // ── PUT /users/{id} - Update user ────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HOSPITAL_ADMIN')")
    @RequireHospitalContext
    @Operation(summary = "Update an existing user")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Security: Prevent SUPER_ADMIN role assignment through API
        if (request.getRole() == User.Role.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SUPER_ADMIN role cannot be assigned through this interface. Use database access for security."));
        }

        // Update fields
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepo.save(user);

        return ResponseEntity.ok(ApiResponse.success(convertToDto(savedUser)));
    }

    // ── DELETE /users/{id} - Delete user ──────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HOSPITAL_ADMIN')")
    @RequireHospitalContext
    @Operation(summary = "Delete a user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {

        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepo.delete(user);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Helper Methods ────────────────────────────────────────────

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .hospital(user.getHospital() != null ? HospitalInfo.builder()
                        .id(user.getHospital().getId())
                        .name(user.getHospital().getName())
                        .city(user.getHospital().getCity())
                        .build() : null)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}