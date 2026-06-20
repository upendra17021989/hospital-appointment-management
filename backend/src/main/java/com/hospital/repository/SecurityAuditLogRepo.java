package com.hospital.repository;

import com.hospital.model.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SecurityAuditLogRepo extends JpaRepository<SecurityAuditLog, UUID> {
}
