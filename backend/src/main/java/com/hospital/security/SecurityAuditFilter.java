package com.hospital.security;

import com.hospital.model.User;
import com.hospital.service.SecurityAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final SecurityAuditService securityAuditService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (shouldAudit(request)) {
                securityAuditService.record(
                        currentUser(),
                        request.getMethod(),
                        request.getRequestURI(),
                        actionFor(request.getMethod()),
                        response.getStatus(),
                        clientIp(request),
                        request.getHeader("User-Agent")
                );
            }
        }
    }

    private boolean shouldAudit(HttpServletRequest request) {
        String path = request.getRequestURI();
        return MUTATING_METHODS.contains(request.getMethod())
                && !path.endsWith("/auth/login")
                && !path.contains("/swagger-ui")
                && !path.contains("/v3/api-docs")
                && !path.contains("/actuator/");
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    private String actionFor(String method) {
        return switch (method) {
            case "POST" -> "CREATE_OR_ACTION";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "REQUEST";
        };
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
