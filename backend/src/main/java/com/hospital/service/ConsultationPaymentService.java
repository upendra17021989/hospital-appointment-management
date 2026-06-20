package com.hospital.service;

import com.hospital.dto.ConsultationPaymentDtos;
import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.security.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationPaymentService {

    private final ConsultationPaymentRepo paymentRepo;
    private final AppointmentRepo appointmentRepo;
    private final ConsultationReceiptRepo consultationReceiptRepo;
    private final ConsultationPaymentLineItemRepo consultationPaymentLineItemRepo;
    private final TenantContext tenantContext;


    @Transactional
    public ConsultationPaymentDtos.CreateConsultationPaymentResponse createPayment(ConsultationPaymentDtos.CreateConsultationPaymentRequest req) {
        UUID hospitalId = tenantContext.requireHospitalId();

        Appointment appt = appointmentRepo.findById(req.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Ensure tenant isolation
        UUID effectiveHospitalId = null;
        if (appt.getHospital() != null && appt.getHospital().getId() != null) {
            effectiveHospitalId = appt.getHospital().getId();
        } else if (appt.getDoctor() != null && appt.getDoctor().getHospital() != null) {
            effectiveHospitalId = appt.getDoctor().getHospital().getId();
        }

        if (effectiveHospitalId == null || !hospitalId.equals(effectiveHospitalId)) {
            throw new IllegalArgumentException("Appointment not found for current hospital");
        }

        consultationReceiptRepo.findActiveByHospitalIdAndAppointmentId(hospitalId, appt.getId())
                .stream()
                .findFirst()
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Receipt already exists for this appointment: " + existing.getReceiptNumber());
                });

        if (req.getAmountPaid() == null || req.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountPaid must be > 0");
        }

        if (req.getConsultationFee() == null || req.getConsultationFee().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("consultationFee must be > 0");
        }

        if (req.getLineItems() == null || req.getLineItems().isEmpty()) {
            throw new IllegalArgumentException("lineItems must be provided (at least 1 row)");
        }

        java.math.BigDecimal sum = req.getLineItems().stream()
                .map(ConsultationPaymentDtos.ReceiptLineItemDto::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        if (sum.compareTo(req.getAmountPaid()) != 0) {
            throw new IllegalArgumentException("Sum of lineItems.amount must equal amountPaid");
        }


        // Received by
        String receivedByName = req.getReceivedByName();
        if (receivedByName == null || receivedByName.isBlank()) {
            receivedByName = tenantContext.getCurrentUser()
                    .map(User::getFullName)
                    .orElse("Staff");
        }

        User receivedByUser = tenantContext.getCurrentUser().orElse(null);

        ConsultationPayment payment = ConsultationPayment.builder()
                .hospital(appt.getHospital() != null ? appt.getHospital() : appt.getDoctor().getHospital())
                .appointment(appt)
                .patient(appt.getPatient())
                .consultationFee(req.getConsultationFee())
                .paymentMode(req.getPaymentMode())
                .paymentReference(req.getPaymentReference())
                .amountPaid(req.getAmountPaid())
                .paidAt(LocalDateTime.now())
                .receivedByUser(receivedByUser)
                .receivedByName(receivedByName)
                .build();

        // Persist payment + its receipt line items snapshot
        ConsultationPayment saved = paymentRepo.save(payment);

        if (req.getLineItems() != null) {
            for (int i = 0; i < req.getLineItems().size(); i++) {
                var li = req.getLineItems().get(i);
                ConsultationPaymentLineItem pli = ConsultationPaymentLineItem.builder()
                        .payment(saved)
                        .srNo(i + 1)
                        .particulars(li.getParticulars())
                        .amount(li.getAmount())
                        .build();
                // repo is included via wildcard import
                // (Spring will autowire it because we reference it here)
                consultationPaymentLineItemRepo.save(pli);
            }
        }


        log.info("Created consultation payment id={} for appointmentId={} hospitalId={}",
                saved.getId(), appt.getId(), hospitalId);

        ConsultationPaymentDtos.CreateConsultationPaymentResponse resp = new ConsultationPaymentDtos.CreateConsultationPaymentResponse();
        resp.setConsultationPaymentId(saved.getId());
        return resp;
    }
}

