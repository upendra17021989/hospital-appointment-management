package com.hospital.service;

import com.hospital.model.*;
import com.hospital.dto.ConsultationReceiptDtos;
import com.hospital.dto.Dtos;
import com.hospital.repository.ConsultationPaymentLineItemRepo;
import com.hospital.repository.ConsultationPaymentRepo;
import com.hospital.repository.ConsultationReceiptLineItemRepo;
import com.hospital.repository.ConsultationReceiptRepo;



import com.hospital.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationReceiptService {

    private final ConsultationReceiptRepo receiptRepo;
    private final ConsultationPaymentRepo paymentRepo;
    private final ConsultationPaymentLineItemRepo receiptLineItemRepo;
    private final ConsultationReceiptLineItemRepo consultationReceiptLineItemRepo;
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

        var existingActive = receiptRepo.findActiveByHospitalIdAndConsultationPaymentId(hospitalId, consultationPaymentId)
                .stream()
                .findFirst();
        if (existingActive.isPresent()) {
            return existingActive.get();
        }

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


        try {
            return receiptRepo.save(receipt);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Receipt already exists for consultation payment {} in hospital {}; returning existing receipt",
                    consultationPaymentId, hospitalId);
            return receiptRepo.findActiveByHospitalIdAndConsultationPaymentId(hospitalId, consultationPaymentId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> ex);
        }

    }

    @Transactional(readOnly = true)
    public Page<ConsultationReceiptDtos.ConsultationReceiptResponse> search(ConsultationReceiptDtos.SearchReceiptsRequest req,
                                                                            int page,
                                                                            int size,
                                                                            String sortBy,
                                                                            String sortDirection,
                                                                            ConsultationPayment.PaymentMode paymentMode) {
        UUID hospitalId = tenantContext.requireHospitalId();
        Sort sort = Sort.by(normalizeSort(sortBy));
        sort = "ASC".equalsIgnoreCase(sortDirection) ? sort.ascending() : sort.descending();

        LocalDateTime start = req.getStartDate() != null ? req.getStartDate().atStartOfDay() : null;
        LocalDateTime end = req.getEndDate() != null ? req.getEndDate().atTime(LocalTime.MAX) : null;

        Specification<ConsultationReceipt> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("hospital").get("id"), hospitalId));

            addContainsIgnoreCase(predicates, cb, root.<String>get("receiptNumber"), req.getReceiptNumber());
            addContainsIgnoreCase(predicates, cb, root.<String>get("patientName"), req.getPatientName());
            addContainsIgnoreCase(predicates, cb, root.<String>get("doctorName"), req.getDoctorName());
            addContainsIgnoreCase(predicates, cb, root.<String>get("departmentName"), req.getDepartmentName());

            if (paymentMode != null) {
                predicates.add(cb.equal(root.<ConsultationPayment.PaymentMode>get("paymentMode"), paymentMode));
            }
            if (req.getReceiptStatus() != null && !req.getReceiptStatus().isBlank()) {
                predicates.add(cb.equal(root.<String>get("receiptStatus"), req.getReceiptStatus()));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("receiptDateTime"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("receiptDateTime"), end));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return receiptRepo.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort)
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConsultationReceipt getReceipt(UUID id) {
        UUID hospitalId = tenantContext.requireHospitalId();
        ConsultationReceipt receipt = receiptRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        if (receipt.getHospital() == null || !hospitalId.equals(receipt.getHospital().getId())) {
            throw new IllegalArgumentException("Receipt not found for current hospital");
        }
        receipt.getHospital().getEmail();
        receipt.getHospital().getLogoUrl();
        receipt.getHospital().getCity();
        receipt.getHospital().getState();
        receipt.getHospital().getPincode();
        receipt.getLineItems().size();
        return receipt;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ConsultationReceipt> findActiveReceiptForAppointment(UUID appointmentId) {
        UUID hospitalId = tenantContext.requireHospitalId();
        return receiptRepo.findActiveByHospitalIdAndAppointmentId(hospitalId, appointmentId)
                .stream()
                .findFirst();
    }

    @Transactional
    public ConsultationReceipt updateReceipt(UUID id, ConsultationReceiptDtos.UpdateConsultationReceiptRequest req) {
        ConsultationReceipt receipt = getReceipt(id);
        if ("VOIDED".equalsIgnoreCase(receipt.getReceiptStatus())) {
            throw new IllegalArgumentException("Voided receipts cannot be updated");
        }
        if (req.getAmountPaid() == null || req.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountPaid must be > 0");
        }
        validateLineItems(req.getLineItems(), req.getAmountPaid());
        receipt.setAmountPaid(req.getAmountPaid());
        receipt.setPaymentReference(req.getPaymentReference());
        receipt.setReceivedByName(req.getReceivedByName());
        receipt.getLineItems().clear();
        for (int i = 0; i < req.getLineItems().size(); i++) {
            var li = req.getLineItems().get(i);
            receipt.getLineItems().add(ConsultationReceiptLineItem.builder()
                    .receipt(receipt)
                    .srNo(i + 1)
                    .particulars(li.getParticulars())
                    .amount(li.getAmount())
                    .build());
        }
        return receiptRepo.save(receipt);
    }

    @Transactional
    public ConsultationReceiptDtos.VoidReceiptResponse voidReceipt(UUID id) {
        ConsultationReceipt receipt = getReceipt(id);
        if (!"VOIDED".equalsIgnoreCase(receipt.getReceiptStatus())) {
            receipt.setReceiptStatus("VOIDED");
            receipt.setVoidedAt(LocalDateTime.now());
            receipt.setVoidedBy(tenantContext.getCurrentUser().map(User::getId).orElse(null));
            receiptRepo.save(receipt);
        }
        return ConsultationReceiptDtos.VoidReceiptResponse.builder()
                .receiptId(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .receiptStatus(receipt.getReceiptStatus())
                .voidedAt(receipt.getVoidedAt())
                .voidedBy(receipt.getVoidedBy())
                .build();
    }

    @Transactional(readOnly = true)
    public ConsultationReceiptDtos.ReceiptDashboardStats dashboardStats() {
        UUID hospitalId = tenantContext.requireHospitalId();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        return ConsultationReceiptDtos.ReceiptDashboardStats.builder()
                .todayReceipts(receiptRepo.countActiveReceiptsBetween(hospitalId, start, end))
                .todayCollection(receiptRepo.sumActiveCollectionBetween(hospitalId, start, end))
                .pendingReceipts(receiptRepo.countByHospitalIdAndReceiptStatus(hospitalId, "PENDING"))
                .build();
    }

    @Transactional(readOnly = true)
    public ConsultationReceiptDtos.DateRangeReportResponse report(LocalDate startDate, LocalDate endDate) {
        UUID hospitalId = tenantContext.requireHospitalId();
        LocalDate startDay = startDate != null ? startDate : LocalDate.now();
        LocalDate endDay = endDate != null ? endDate : startDay;
        LocalDateTime start = startDay.atStartOfDay();
        LocalDateTime end = endDay.atTime(LocalTime.MAX);
        return ConsultationReceiptDtos.DateRangeReportResponse.builder()
                .dailyCollection(receiptRepo.dailyCollection(hospitalId, start, end).stream()
                        .map(a -> ConsultationReceiptDtos.DailyCollectionRow.builder()
                                .date(a.getDay())
                                .totalCollected(a.getTotalCollected())
                                .build())
                        .collect(Collectors.toList()))
                .paymentModeWiseCollection(receiptRepo.paymentModeWiseCollection(hospitalId, start, end).stream()
                        .map(a -> ConsultationReceiptDtos.PaymentModeWiseRow.builder()
                                .paymentMode(a.getPaymentMode())
                                .totalCollected(a.getTotalCollected())
                                .build())
                        .collect(Collectors.toList()))
                .doctorWiseCollection(receiptRepo.doctorWiseCollection(hospitalId, start, end).stream()
                        .map(a -> ConsultationReceiptDtos.DoctorWiseRow.builder()
                                .doctorName(a.getDoctorName())
                                .totalCollected(a.getTotalCollected())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ConsultationReceiptDtos.ConsultationReceiptResponse> patientHistory(UUID patientId) {
        UUID hospitalId = tenantContext.requireHospitalId();
        return receiptRepo.findByHospitalIdAndPatientIdentifierOrderByReceiptDateTimeDesc(hospitalId, String.valueOf(patientId))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] export(LocalDate startDate, LocalDate endDate, String format) {
        var report = report(startDate, endDate);
        String normalized = format == null ? "csv" : format.toLowerCase(Locale.ROOT);
        if ("xlsx".equals(normalized) || "excel".equals(normalized)) return exportExcel(report);
        if ("pdf".equals(normalized)) return exportReportPdf(report);
        return exportCsv(report);
    }

    public ConsultationReceiptDtos.ConsultationReceiptResponse toResponse(ConsultationReceipt receipt) {
        List<ConsultationReceiptDtos.ReceiptLineItemDto> items = consultationReceiptLineItemRepo
                .findByReceiptIdOrderBySrNoAsc(receipt.getId())
                .stream()
                .map(li -> ConsultationReceiptDtos.ReceiptLineItemDto.builder()
                        .particulars(li.getParticulars())
                        .amount(li.getAmount())
                        .build())
                .collect(Collectors.toList());
        return ConsultationReceiptDtos.ConsultationReceiptResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .receiptDateTime(receipt.getReceiptDateTime())
                .patientName(receipt.getPatientName())
                .doctorName(receipt.getDoctorName())
                .departmentName(receipt.getDepartmentName())
                .amountPaid(receipt.getAmountPaid())
                .paymentMode(receipt.getPaymentMode())
                .paymentReference(receipt.getPaymentReference())
                .receivedByName(receipt.getReceivedByName())
                .receiptStatus(receipt.getReceiptStatus())
                .voidedAt(receipt.getVoidedAt())
                .voidedBy(receipt.getVoidedBy())
                .lineItems(items)
                .build();
    }

    private void validateLineItems(List<ConsultationReceiptDtos.ReceiptLineItemDto> lineItems, BigDecimal amountPaid) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must be provided");
        }
        BigDecimal sum = lineItems.stream()
                .map(ConsultationReceiptDtos.ReceiptLineItemDto::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(amountPaid) != 0) {
            throw new IllegalArgumentException("Sum of lineItems.amount must equal amountPaid");
        }
    }

    private byte[] exportCsv(ConsultationReceiptDtos.DateRangeReportResponse report) {
        StringBuilder csv = new StringBuilder("Report,Key,Amount\n");
        report.getDailyCollection().forEach(r -> csv.append("Daily,").append(r.getDate()).append(',').append(r.getTotalCollected()).append('\n'));
        report.getPaymentModeWiseCollection().forEach(r -> csv.append("Payment Mode,").append(r.getPaymentMode()).append(',').append(r.getTotalCollected()).append('\n'));
        report.getDoctorWiseCollection().forEach(r -> csv.append("Doctor,").append(escape(r.getDoctorName())).append(',').append(r.getTotalCollected()).append('\n'));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportExcel(ConsultationReceiptDtos.DateRangeReportResponse report) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("Collections");
            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Report");
            header.createCell(1).setCellValue("Key");
            header.createCell(2).setCellValue("Amount");
            for (var r : report.getDailyCollection()) rowIdx = addExportRow(sheet, rowIdx, "Daily", String.valueOf(r.getDate()), r.getTotalCollected());
            for (var r : report.getPaymentModeWiseCollection()) rowIdx = addExportRow(sheet, rowIdx, "Payment Mode", String.valueOf(r.getPaymentMode()), r.getTotalCollected());
            for (var r : report.getDoctorWiseCollection()) rowIdx = addExportRow(sheet, rowIdx, "Doctor", r.getDoctorName(), r.getTotalCollected());
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    private int addExportRow(org.apache.poi.ss.usermodel.Sheet sheet, int rowIdx, String report, String key, BigDecimal amount) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(report);
        row.createCell(1).setCellValue(key);
        row.createCell(2).setCellValue(amount == null ? 0 : amount.doubleValue());
        return rowIdx;
    }

    private byte[] exportReportPdf(ConsultationReceiptDtos.DateRangeReportResponse report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf);
            doc.add(new Paragraph("Consultation Receipt Collection Report").setBold().setFontSize(16));
            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 45, 25})).useAllAvailableWidth();
            table.addHeaderCell("Report");
            table.addHeaderCell("Key");
            table.addHeaderCell("Amount");
            report.getDailyCollection().forEach(r -> addPdfRow(table, "Daily", String.valueOf(r.getDate()), r.getTotalCollected()));
            report.getPaymentModeWiseCollection().forEach(r -> addPdfRow(table, "Payment Mode", String.valueOf(r.getPaymentMode()), r.getTotalCollected()));
            report.getDoctorWiseCollection().forEach(r -> addPdfRow(table, "Doctor", r.getDoctorName(), r.getTotalCollected()));
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF", e);
        }
    }

    private void addPdfRow(Table table, String report, String key, BigDecimal amount) {
        table.addCell(report);
        table.addCell(key == null ? "" : key);
        table.addCell(amount == null ? "0.00" : amount.toPlainString());
    }

    private String normalizeSort(String sortBy) {
        if (sortBy == null) return "receiptDateTime";
        return switch (sortBy) {
            case "receiptNumber", "patientName", "doctorName", "amountPaid", "paymentMode", "receiptStatus" -> sortBy;
            default -> "receiptDateTime";
        };
    }

    private void addContainsIgnoreCase(List<jakarta.persistence.criteria.Predicate> predicates,
                                       jakarta.persistence.criteria.CriteriaBuilder cb,
                                       jakarta.persistence.criteria.Path<String> field,
                                       String value) {
        if (value != null && !value.isBlank()) {
            predicates.add(cb.like(cb.lower(field), "%" + value.toLowerCase(Locale.ROOT) + "%"));
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.contains(",") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
