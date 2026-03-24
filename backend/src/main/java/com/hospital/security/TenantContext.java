package com.hospital.security;

import com.hospital.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility to extract the current authenticated user and their hospital ID
 * from the Spring Security context. Use this in services/controllers to
 * enforce multi-tenant data isolation.
 */
@Component
public class TenantContext {

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<UUID> getCurrentHospitalId() {
        return getCurrentUser()
                .map(user -> user.getHospital() != null ? user.getHospital().getId() : null);
    }

    public UUID requireHospitalId() {
        return getCurrentHospitalId()
                .orElseThrow(() -> new RuntimeException("No hospital context for current user"));
    }

    public boolean isSuperAdmin() {
        return getCurrentUser()
                .map(u -> u.getRole() == com.hospital.model.User.Role.SUPER_ADMIN)
                .orElse(false);
    }
}
