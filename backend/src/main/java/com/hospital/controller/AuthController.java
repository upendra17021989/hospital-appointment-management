package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.Hospital;
import com.hospital.model.User;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.UserRepo;
import com.hospital.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login and profile endpoints")
public class AuthController {

    private final HospitalRepo hospitalRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ── DTOs ──────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        // Hospital info
        @NotBlank(message = "Hospital name is required")
        private String hospitalName;

        private String hospitalAddress;
        private String hospitalCity;
        private String hospitalState;
        private String hospitalPhone;
        private String hospitalEmail;
        private String hospitalWebsite;
        private String licenseNumber;

        // Admin user info
        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String email;
        private String fullName;
        private String role;
        private HospitalInfo hospital;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HospitalInfo {
        private UUID id;
        private String name;
        private String slug;
        private String city;
        private String phone;
        private String email;
        private String logoUrl;
    }

    // ── Register Hospital + Admin ─────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new hospital with admin account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        // Check if email already exists
        if (userRepo.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("An account with this email already exists."));
        }

        // Generate unique slug from hospital name
        String slug = generateSlug(request.getHospitalName());

        // Create Hospital
        Hospital hospital = Hospital.builder()
                .name(request.getHospitalName())
                .slug(slug)
                .address(request.getHospitalAddress())
                .city(request.getHospitalCity())
                .state(request.getHospitalState())
                .phone(request.getHospitalPhone())
                .email(request.getHospitalEmail())
                .website(request.getHospitalWebsite())
                .licenseNumber(request.getLicenseNumber())
                .isActive(true)
                .build();

        Hospital savedHospital = hospitalRepo.save(hospital);

        // Create Admin User
        User admin = User.builder()
                .hospital(savedHospital)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.HOSPITAL_ADMIN)
                .isActive(true)
                .build();

        User savedAdmin = userRepo.save(admin);

        String token = jwtService.generateToken(
                savedAdmin,
                savedHospital.getId(),
                savedHospital.getName(),
                savedAdmin.getRole().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Hospital registered successfully!", buildAuthResponse(token, savedAdmin, savedHospital))
        );
    }

    // ── Login ─────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        }

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your account has been deactivated. Please contact support."));
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepo.save(user);

        Hospital hospital = user.getHospital();
        String token = jwtService.generateToken(
                user,
                hospital != null ? hospital.getId() : null,
                hospital != null ? hospital.getName() : null,
                user.getRole().name()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", buildAuthResponse(token, user, hospital))
        );
    }

    // ── Get current user profile ──────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<AuthResponse>> me(@AuthenticationPrincipal User user) {
        Hospital hospital = user.getHospital();
        // Generate fresh short-lived token
        String token = jwtService.generateToken(
                user,
                hospital != null ? hospital.getId() : null,
                hospital != null ? hospital.getName() : null,
                user.getRole().name()
        );
        return ResponseEntity.ok(ApiResponse.success(buildAuthResponse(token, user, hospital)));
    }

    // ── Add Staff User (HOSPITAL_ADMIN only) ──────────────────────

    @PostMapping("/add-staff")
    @Operation(summary = "Add a staff member to your hospital (admin only)")
    public ResponseEntity<ApiResponse<String>> addStaff(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RegisterRequest request) {

        if (currentUser.getRole() != User.Role.HOSPITAL_ADMIN
                && currentUser.getRole() != User.Role.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Only hospital admins can add staff."));
        }

        if (userRepo.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Email already in use."));
        }

        User staff = User.builder()
                .hospital(currentUser.getHospital())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.STAFF)
                .isActive(true)
                .build();

        userRepo.save(staff);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff member added successfully.", "created"));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(String token, User user, Hospital hospital) {
        HospitalInfo hospitalInfo = null;
        if (hospital != null) {
            hospitalInfo = HospitalInfo.builder()
                    .id(hospital.getId())
                    .name(hospital.getName())
                    .slug(hospital.getSlug())
                    .city(hospital.getCity())
                    .phone(hospital.getPhone())
                    .email(hospital.getEmail())
                    .logoUrl(hospital.getLogoUrl())
                    .build();
        }
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .hospital(hospitalInfo)
                .build();
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String slug = base;
        int suffix = 1;
        while (hospitalRepo.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }
}
