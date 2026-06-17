package com.hospital.service;

import com.hospital.model.ConsultationReceipt;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationReceiptPdfService {

    public byte[] generatePdf(ConsultationReceipt receipt) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            doc.add(new Paragraph("Hospital Consultation Receipt")
                    .setFont(bold)
                    .setFontSize(16));

            doc.add(new Paragraph(
                            safe(receipt.getHospitalName()) + "\n" +
                                    safe(receipt.getHospitalAddress()) + "\n" +
                                    safe(receipt.getHospitalPhone()))
                    .setFont(regular)
                    .setFontSize(10));

            doc.add(new Paragraph("\n"));

            Table keyVals = new Table(new float[]{1, 2});

            addKV(keyVals, "Receipt No", safe(receipt.getReceiptNumber()));
            addKV(keyVals, "Date & Time", receipt.getReceiptDateTime() != null ? receipt.getReceiptDateTime().toString() : "");
            addKV(keyVals, "Patient", safe(receipt.getPatientName()) + " (" + safe(receipt.getPatientIdentifier()) + ")");
            addKV(keyVals, "Doctor", safe(receipt.getDoctorName()));
            addKV(keyVals, "Department", safe(receipt.getDepartmentName()));
            addKV(keyVals, "Consultation Fee", money(receipt.getConsultationFee()));

            addKV(keyVals, "Payment Mode", receipt.getPaymentMode() != null ? receipt.getPaymentMode().name() : "");
            addKV(keyVals, "Payment Reference", safe(receipt.getPaymentReference()));
            addKV(keyVals, "Amount Paid", money(receipt.getAmountPaid()));
            addKV(keyVals, "Received By", safe(receipt.getReceivedByName()));

            doc.add(keyVals);

            // Narrative (staff custom scan-like text)
            // For now we render a look-alike fixed narrative.
            // You can later store this in ConsultationReceipt and replace this text.
            doc.add(new Paragraph(
                            "An amount of ₹" + money(receipt.getAmountPaid()) + " received with thanks from "
                            + safe(receipt.getPatientName()) + " towards consultation."));



            doc.add(new Paragraph("\n"));

            // ===== Sr. No. | Particulars | Amount table (staff-entered) =====
            List<ReceiptLineItem> items = new ArrayList<>();
            if (receipt.getLineItems() != null && !receipt.getLineItems().isEmpty()) {
                for (var li : receipt.getLineItems()) {
                    items.add(ReceiptLineItem.builder()
                            .srNo(li.getSrNo())
                            .particulars(li.getParticulars())
                            .amount(li.getAmount())
                            .build());
                }
            } else {
                // Backward compatible fallback
                items.add(ReceiptLineItem.builder()
                        .srNo(1)
                        .particulars("Consultation Fee")
                        .amount(receipt.getConsultationFee())
                        .build());
            }

            Table linesTable = new Table(new float[]{0.6f, 3.0f, 1.4f});

            linesTable.addHeaderCell(new com.itextpdf.layout.element.Cell()

                    .add(new Paragraph("Sr. No").setFontSize(10).setBold()));
            linesTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("Particulars").setFontSize(10).setBold()));
            linesTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("Amount").setFontSize(10).setBold()));

            for (ReceiptLineItem li : items) {
                linesTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(String.valueOf(li.getSrNo())).setFontSize(10)));
                linesTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(safe(li.getParticulars())).setFontSize(10)));
                linesTable.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(money(li.getAmount())).setFontSize(10)));
            }

            // Total row
            linesTable.addCell(new com.itextpdf.layout.element.Cell(1, 2)
                    .add(new Paragraph("Grand Total").setFontSize(10).setBold()));
            linesTable.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph(money(receipt.getAmountPaid())).setFontSize(10).setBold()));


            doc.add(linesTable);

            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("------------------------------------------------------------"));
            doc.add(new Paragraph("\n"));

            // Stamp / signature area (custom scan/placeholder)
            // If stampPlaceholder is configured with a custom scan label, show it here.
            // For image-based stamp, we'd need a new field for stamp image bytes/URL.
            String stamp = (receipt.getStampPlaceholder() != null && !receipt.getStampPlaceholder().isBlank())
                    ? receipt.getStampPlaceholder()
                    : "Hospital Stamp/Signature";

            doc.add(new Paragraph(stamp).setFont(regular));
            doc.add(new Paragraph("__________________________"));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate consultation receipt PDF: " + e.getMessage(), e);
        }
    }

    private void addKV(Table table, String key, String value) {
        table.addCell(new Paragraph(key).setFontSize(10));
        table.addCell(new Paragraph(value != null ? value : "").setFontSize(10));
    }

    private String money(java.math.BigDecimal amt) {
        if (amt == null) return "";
        return amt.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

