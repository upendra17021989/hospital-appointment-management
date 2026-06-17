package com.hospital.service;

import com.hospital.model.ConsultationReceipt;
import com.hospital.model.ConsultationReceiptLineItem;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.borders.Border;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationReceiptPdfService {

    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] generatePdf(ConsultationReceipt receipt) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            //PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            // PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            // PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont italic = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
            PdfFont boldItalic = PdfFontFactory.createFont(StandardFonts.TIMES_BOLDITALIC);

            float pageWidth = pdf.getDefaultPageSize().getWidth();
            float contentWidth = pageWidth - 40; // approx to match margins in template

            // --- Header: Invoice | Receipt ---
            doc.add(new Paragraph("Invoice | Receipt")
                    .setFont(bold)
                    .setFontSize(28)
                    .setTextAlignment(TextAlignment.CENTER));

            // --- Receipt info: Receipt No / Receipt Date ---
            Table receiptInfo = new Table(new float[]{1, 1});
            receiptInfo.setWidth(contentWidth);
            receiptInfo.setMarginTop(12);

            receiptInfo.addCell(infoCell("Receipt No.", receipt.getReceiptNumber(), regular, bold));
            receiptInfo.addCell(infoCell("Receipt Date", formatReceiptDate(receipt), regular, bold));

            doc.add(receiptInfo);

            // --- Description narrative (template text) ---
            String patient = safe(receipt.getPatientName());
            String hospitalName = safe(receipt.getHospitalName());
            // doc.add(new Paragraph(
            //         "An amount of " + money(receipt.getAmountPaid()) + " received with thanks from " +
            //                 "Mr. / Ms. / Mrs. " + patient + " " +
            //                 "towards the treatment / examination / health check-up of " +
            //                 "Mr. / Ms. / Mrs. " + patient + " " +
            //                 "at " + hospitalName + ".")
            //             .setFont(regular)
            //             .setFontSize(14)
            //             .setMultipliedLeading(1.2f)
            //             .setMarginTop(16));
            //     doc.add(new Paragraph("Particulars of the payment are as below.")
            //     .setFont(italic)
            //     .setFontSize(14)
            //     .setMultipliedLeading(1.2f)
            //     .setMarginTop(16));
            Paragraph receiptParagraph = new Paragraph()
    // Static text - Roman Italic
    .add(new Text("An amount of ")
            .setFont(italic))

    // Dynamic amount - Bold + Underline
    .add(new Text(money(receipt.getAmountPaid()))
            .setFont(bold)
            .setUnderline())

    // Static text - Roman Italic
    .add(new Text(" received with thanks from Mr. / Ms. / Mrs. ")
            .setFont(italic))

    // Dynamic patient name - Bold + Underline
    .add(new Text(patient)
            .setFont(bold)
            .setUnderline())

    // Static text - Roman Italic
    .add(new Text(" towards the treatment / examination / health check-up of Mr. / Ms. / Mrs. ")
            .setFont(italic))

    // Dynamic patient name - Bold + Underline
    .add(new Text(patient)
            .setFont(bold)
            .setUnderline())

    // Static text - Roman Italic
    .add(new Text(" at ")
            .setFont(italic))

    // Dynamic hospital name - Bold + Underline
    .add(new Text(hospitalName)
            .setFont(bold)
            .setUnderline())

    // Static text - Roman Italic
    .add(new Text(".")
            .setFont(italic))

    .setFontSize(14)
    .setMultipliedLeading(1.2f)
    .setMarginTop(16);

doc.add(receiptParagraph);

doc.add(
    new Paragraph("Particulars of the payment are as below.")
        .setFont(italic)
        .setFontSize(14)
        .setMultipliedLeading(1.2f)
        .setMarginTop(10)
);
            // --- Table: Sr. No / Particulars / Amount (with borders) ---
            Table lines = new Table(new float[]{0.25f, 0.55f, 0.2f});
            lines.setWidth(contentWidth);
            lines.setMarginTop(14);

            lines.addHeaderCell(borderedHeaderCell("Sr. No."));
            lines.addHeaderCell(borderedHeaderCell("Particulars"));
            lines.addHeaderCell(borderedHeaderCell("Amount"));

            // Use stored receipt line items snapshot
            List<ConsultationReceiptLineItem> items = receipt.getLineItems() != null ? receipt.getLineItems() : new ArrayList<>();
            if (items.isEmpty()) {
                // fallback
                ConsultationReceiptLineItem li = ConsultationReceiptLineItem.builder()
                        .srNo(1)
                        .particulars("Consultation Fee")
                        .amount(receipt.getConsultationFee())
                        .build();
                items = List.of(li);
            }

            for (ConsultationReceiptLineItem li : items) {
                lines.addCell(borderedBodyCell(String.valueOf(li.getSrNo())));
                lines.addCell(borderedBodyCell(safe(li.getParticulars())));

                Cell amt = borderedBodyCell(money(li.getAmount()));
                amt.setTextAlignment(TextAlignment.RIGHT);
                lines.addCell(amt);
            }

            doc.add(lines);

            // --- Grand total (right) ---
            doc.add(new Paragraph("Grand Total : " + money(receipt.getAmountPaid()) + " INR")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(10));

            // --- Footer: QR section placeholder + Signature ---
            Table footer = new Table(new float[]{1, 1});
            footer.setWidth(contentWidth);
            footer.setMarginTop(30);

            Cell qrCell = new Cell();
            qrCell.setBorder(Border.NO_BORDER);
            qrCell.setTextAlignment(TextAlignment.CENTER);
            qrCell.add(new Paragraph("QR Code").setFont(bold).setFontSize(13));
            qrCell.add(new Paragraph("(QR placeholder)"));
            footer.addCell(qrCell);

            Cell sigCell = new Cell();
            sigCell.setBorder(Border.NO_BORDER);
            sigCell.setTextAlignment(TextAlignment.CENTER);
            sigCell.add(new Paragraph("Authorized Signatory").setMarginBottom(20));
            sigCell.add(new Paragraph("__________________________"));
            footer.addCell(sigCell);

            doc.add(footer);

            // --- Note ---
            doc.add(new Paragraph("N.B.: This receipt is not for the medico legal purpose.")
                    .setFontSize(12)
                    .setMarginTop(20));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate consultation receipt PDF: " + e.getMessage(), e);
        }
    }

    private String formatReceiptDate(ConsultationReceipt receipt) {
        if (receipt == null || receipt.getReceiptDateTime() == null) return "";
        return receipt.getReceiptDateTime().toLocalDate().format(RECEIPT_DATE);
    }

    private Cell infoCell(String label, String value, PdfFont regular, PdfFont bold) {
        Cell cell = new Cell();
        cell.setBorder(Border.NO_BORDER);
        cell.add(new Paragraph(label + " : ").setFont(bold).setFontSize(14));
        cell.add(new Paragraph(value != null ? value : "").setFont(regular).setFontSize(14));
        return cell;
    }

    private Cell borderedHeaderCell(String text) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text).setFontSize(12).setBold());
        cell.setBorder(new com.itextpdf.layout.borders.SolidBorder(1));
        cell.setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
        return cell;
    }

    private Cell borderedBodyCell(String text) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text != null ? text : "").setFontSize(12));
        cell.setBorder(new com.itextpdf.layout.borders.SolidBorder(1));
        return cell;
    }

    private String money(java.math.BigDecimal amt) {
        if (amt == null) return "";
        return amt.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

