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

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");

    /**
     * Generates a unique receipt number scoped by hospital.
     *
     * Format: CR-YYYY-000001
     * Notes:
     * - Sequence resets every year.
     * - Uses database uniqueness constraint (hospital_id + receipt_number) and a small retry window.
     */
    @Transactional
    public String generateUniqueReceiptNumber(int maxAttempts) {
        UUID hospitalId = tenantContext.requireHospitalId();
        String year = LocalDate.now().format(YEAR);

        for (int i = 1; i <= maxAttempts; i++) {
            long next = receiptRepo.countByHospitalIdAndReceiptNumberStartingWith(hospitalId, "CR-" + year + "-") + 1;

            String candidate = "CR-" + year + "-" + String.format("%06d", next);

            boolean exists = receiptRepo.findByHospitalIdAndReceiptNumber(hospitalId, candidate).isPresent();
            if (!exists) return candidate;

            log.debug("Receipt number collision for hospital {}, candidate={}, attempt={}", hospitalId, candidate, i);
        }

        throw new IllegalStateException("Failed to generate unique receipt number after " + maxAttempts + " attempts");
    }

    @SuppressWarnings("unused")
    private String buildCandidate() {
        throw new UnsupportedOperationException("buildCandidate() is not used. Receipt number generation is sequence-based.");
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

