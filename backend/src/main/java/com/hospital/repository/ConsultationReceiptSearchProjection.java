package com.hospital.repository;

import com.hospital.model.ConsultationPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;



/**
 * Lightweight projection for receipt list/table.
 */
public interface ConsultationReceiptSearchProjection {

    UUID getId();
    String getReceiptNumber();
    LocalDateTime getReceiptDateTime();

    String getPatientName();
    String getDoctorName();
    String getDepartmentName();

    BigDecimal getAmountPaid();
    ConsultationPayment.PaymentMode getPaymentMode();

    String getReceiptStatus();
    LocalDate getReceiptDate();

}

