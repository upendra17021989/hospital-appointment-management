package com.hospital.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.model.User;
import com.hospital.service.ModuleSettingService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ModuleAccessFilter extends OncePerRequestFilter {
    private final ModuleSettingService settings;
    private final ObjectMapper objectMapper;
    private static final Map<String, String> PREFIXES = new LinkedHashMap<>();
    static {
        PREFIXES.put("/subscriptions", "BILLING_PLANS");
        PREFIXES.put("/payments", "BILLING_PLANS");
        PREFIXES.put("/reports", "REPORTS");
        PREFIXES.put("/prescriptions", "CLINICAL");
        PREFIXES.put("/medical-certificates", "CLINICAL");
        PREFIXES.put("/consultation-payments", "CONSULTATION_BILLING");
        PREFIXES.put("/consultation-receipts", "CONSULTATION_BILLING");
        PREFIXES.put("/patients", "PATIENTS");
        PREFIXES.put("/appointments", "APPOINTMENTS");
        PREFIXES.put("/users", "USER_MANAGEMENT");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user && user.getRole() != User.Role.SUPER_ADMIN
                && user.getHospital() != null) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String module = PREFIXES.entrySet().stream().filter(e -> path.startsWith(e.getKey()))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            if (module != null && !settings.isEnabled(user.getHospital().getId(), module)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), Map.of("success", false,
                        "message", "This module has been disabled by the site administrator", "module", module));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
