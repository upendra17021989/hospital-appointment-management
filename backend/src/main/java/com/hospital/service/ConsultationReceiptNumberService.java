package com.hospital.service;

import com.hospital.model.ConsultationReceipt;
import com.hospital.repository.ConsultationReceiptRepo;
import com.hospital.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationReceiptNumberService {

    private final ConsultationReceiptRepo receiptRepo;
    private final TenantContext tenantContext;

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generates a unique receipt number scoped by hospital.
     *
     * Strategy:
     * - Try to generate RCPT-{yyyyMMdd}-{random8}
     * - Rely on DB unique constraint (hospital_id + receipt_number)
     * - Retry on constraint violations up to a limit.
     */
    @Transactional
    public String generateUniqueReceiptNumber(int maxAttempts) {
        UUID hospitalId = tenantContext.requireHospitalId();

        for (int i = 1; i <= maxAttempts; i++) {
            String candidate = buildCandidate();

            boolean exists = receiptRepo.findByHospitalIdAndReceiptNumber(hospitalId, candidate).isPresent();
            if (!exists) {
                return candidate;
            }

            log.debug("Receipt number collision for hospital {}, candidate={}, attempt={}", hospitalId, candidate, i);
        }

        throw new IllegalStateException("Failed to generate unique receipt number after " + maxAttempts + " attempts");
    }

    private String buildCandidate() {
        // Example: RCPT-20260611-1A2B3C4D
        String date = LocalDate.now().format(D);
        String random8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RCPT-" + date + "-" + random8;
    }

    /**
     * Utility for building a receipt placeholder string.
     */
    public String defaultStampPlaceholder() {
        return "Hospital Stamp/Signature";
    }

    /**
     * Helper if you need it: validate receipt number format (optional).
     */
    public void validateReceiptNumber(String receiptNumber) {
        if (receiptNumber == null || receiptNumber.isBlank()) {
            throw new IllegalArgumentException("receiptNumber is required");
        }
        // Minimal validation only.
    }
}

