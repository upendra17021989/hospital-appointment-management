package com.hospital.repository;

import com.hospital.model.ConsultationPayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ConsultationReceiptAggregates {

    interface DailyCollectionAgg {
        LocalDate getDay();
        BigDecimal getTotalCollected();
    }

    interface PaymentModeWiseAgg {
        ConsultationPayment.PaymentMode getPaymentMode();
        BigDecimal getTotalCollected();
    }

    interface DoctorWiseAgg {
        String getDoctorName();
        BigDecimal getTotalCollected();
    }
}


