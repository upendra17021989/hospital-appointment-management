package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.ConsultationPaymentLineItemRepo;
import com.hospital.repository.ConsultationPaymentRepo;
import com.hospital.repository.ConsultationReceiptRepo;



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
public class ConsultationReceiptService {

    private final ConsultationReceiptRepo receiptRepo;
    private final ConsultationPaymentRepo paymentRepo;
    private final ConsultationPaymentLineItemRepo receiptLineItemRepo;
    private final ConsultationReceiptNumberService numberService;
    private final TenantContext tenantContext;


    /**
     * Creates a new receipt for a consultation payment.
     *
     * This method is intentionally snapshot-based so later reprints remain consistent.
     */
@Transactional
    public ConsultationReceipt createReceiptForPayment(UUID consultationPaymentId) {


        UUID hospitalId = tenantContext.requireHospitalId();

        ConsultationPayment payment = paymentRepo.findById(consultationPaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation payment not found"));

        if (payment.getHospital() == null || !hospitalId.equals(payment.getHospital().getId())) {
            throw new IllegalArgumentException("Consultation payment not found for current hospital");
        }

        // If you want 1 receipt per payment, enforce here.
        // Currently we allow multiple receipts (reprints) by generating a new receipt number.

        String receiptNumber = numberService.generateUniqueReceiptNumber(8);

        Hospital hospital = payment.getHospital();
        Appointment appt = payment.getAppointment();
        Patient patient = payment.getPatient();
        Doctor doctor = appt != null ? appt.getDoctor() : null;
        Department dept = appt != null ? appt.getDepartment() : null;

        ConsultationReceipt receipt = ConsultationReceipt.builder()
                .hospital(hospital)
                .consultationPayment(payment)
                .receiptNumber(receiptNumber)
                .receiptDateTime(LocalDateTime.now())
                .hospitalName(hospital != null ? hospital.getName() : "")
                .hospitalAddress(hospital != null ? hospital.getAddress() : "")
                .hospitalPhone(hospital != null ? hospital.getPhone() : "")
                .patientName(patient != null ? patient.getFullName() : "")
                .patientIdentifier(patient != null ? String.valueOf(patient.getId()) : "")
                .doctorName(doctor != null ? doctor.getFullName() : "")
                .departmentName(dept != null ? dept.getName() : "")
                .consultationFee(payment.getConsultationFee())
                .paymentMode(payment.getPaymentMode())
                .paymentReference(payment.getPaymentReference())
                .amountPaid(payment.getAmountPaid())
                .receivedByName(payment.getReceivedByName())
                .stampPlaceholder(numberService.defaultStampPlaceholder())
                .receiptStatus("ACTIVE")
                .voidedAt(null)
                .voidedBy(null)
                .lineItems(new java.util.ArrayList<>())
                .build();


        // Snapshot of receptionist-entered line items.
        // We persist these at payment creation time into consultation_payment_line_items,
        // so we can safely snapshot them into the receipt later (PDF/reprint).

        var paymentLineItems = receiptLineItemRepo.findByPaymentIdOrderBySrNoAsc(consultationPaymentId);

        java.util.List<ConsultationReceiptLineItem> snapshot = new java.util.ArrayList<>();
        if (paymentLineItems != null && !paymentLineItems.isEmpty()) {
            for (var pli : paymentLineItems) {
                snapshot.add(ConsultationReceiptLineItem.builder()
                        .receipt(receipt)
                        .srNo(pli.getSrNo())
                        .particulars(pli.getParticulars())
                        .amount(pli.getAmount())
                        .build());
            }
        } else {
            // Backward compatibility for previously created payments/receipts.
            snapshot.add(ConsultationReceiptLineItem.builder()
                    .srNo(1)
                    .particulars("Consultation Fee")
                    .amount(payment.getConsultationFee())
                    .receipt(receipt)
                    .build());
        }

        receipt.setLineItems(snapshot);


        return receiptRepo.save(receipt);

    }
}

