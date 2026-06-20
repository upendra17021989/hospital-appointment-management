package com.hospital.service;

import com.hospital.model.SecurityAuditLog;
import com.hospital.model.User;
import com.hospital.repository.SecurityAuditLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final SecurityAuditLogRepo securityAuditLogRepo;

    public void record(User user, String method, String path, String action, int status, String ipAddress, String userAgent) {
        securityAuditLogRepo.save(SecurityAuditLog.builder()
                .user(user)
                .hospital(user != null ? user.getHospital() : null)
                .method(method)
                .path(path)
                .action(action)
                .httpStatus(status)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
