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
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.itextpdf.layout.properties.HorizontalAlignment;

import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.element.LineSeparator;

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
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont italic = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
            PdfFont boldItalic = PdfFontFactory.createFont(StandardFonts.TIMES_BOLDITALIC);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            doc.setMargins(20, 20, 20, 20);
            float pageWidth = pdf.getDefaultPageSize().getWidth();
            float contentWidth = pageWidth - 40; // approx to match margins in template

            // --- Header: Invoice | Receipt ---
            doc.add(new Paragraph("Invoice | Receipt")
                    .setFont(italic)
                    .setFontSize(26)
                    .setUnderline()
                    .setTextAlignment(TextAlignment.CENTER));

            // --- Receipt info: Receipt No / Receipt Date ---
            Table receiptInfo = new Table(new float[]{1, 1});
            receiptInfo.setWidth(contentWidth);
            receiptInfo.setHorizontalAlignment(HorizontalAlignment.CENTER);
            receiptInfo.setMarginTop(12);

            receiptInfo.addCell(infoCell("Receipt No.", receipt.getReceiptNumber(), italic, bold));
            receiptInfo.addCell(infoCell("Receipt Date", formatReceiptDate(receipt), italic, bold));
            receiptInfo.setTextAlignment(TextAlignment.CENTER);

            doc.add(receiptInfo);

            // --- Description narrative (template text) ---
            String patient = safe(receipt.getPatientName());
            String hospitalName = safe(receipt.getHospitalName());
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
                .setMarginTop(16)
                .setWidth(contentWidth)
                .setTextAlignment(TextAlignment.CENTER)
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
            lines.setHorizontalAlignment(HorizontalAlignment.CENTER);
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
            footer.setHorizontalAlignment(HorizontalAlignment.CENTER);
            footer.setMarginTop(30);

            Cell qrCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
            URL qrUrl = getClass()
                .getClassLoader()
                .getResource("images/qr-code.png");

            if (qrUrl != null) {

                Image qrImage = new Image(ImageDataFactory.create(qrUrl))
                        .setWidth(100)
                        .setHeight(100)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);

                qrCell.add(qrImage);

                qrCell.add(
                    new Paragraph("Scan the QR Code to pay online")
                            .setFont(italic)
                            .setFontSize(12)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(5)
                );

            } else {

                qrCell.add(
                    new Paragraph("QR Code")
                            .setFont(bold)
                            .setFontSize(13)
                );

                qrCell.add(
                    new Paragraph("(QR placeholder)")
                            .setFont(regular)
                );
            }

                footer.addCell(qrCell);
                
                Cell sigCell = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER);

                // Rounded rectangle box
                Div signBox = new Div()
                        .setWidth(150)
                        .setHeight(100)
                        .setBorder(new SolidBorder(2))
                        .setBorderRadius(new BorderRadius(15));

                signBox.setHorizontalAlignment(HorizontalAlignment.CENTER);

                sigCell.add(signBox);

                sigCell.add(
                new Paragraph("Authorized Signatory")
                        .setFont(italic)
                        .setFontSize(12)
                        .setItalic()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(5)
                );

                footer.addCell(sigCell);

                doc.add(footer);

                // Horizontal line below footer
                LineSeparator line = new LineSeparator(new SolidLine());
                line.setHorizontalAlignment(HorizontalAlignment.CENTER);
                line.setMarginTop(10);
                line.setMarginBottom(10);

                doc.add(line);

                // --- Note ---
                doc.add(new Paragraph("N.B.: This receipt is not for the medico legal purpose.")
                        .setFontSize(12)
                        .setMarginTop(20)
                        .setFont(italic)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBold());
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

//     private Cell infoCell(String label, String value, PdfFont regular, PdfFont bold) {
//         Cell cell = new Cell();
//         cell.setBorder(Border.NO_BORDER);
//         cell.add(new Paragraph(label + " : ").setFont(bold).setFontSize(14));
//         cell.add(new Paragraph(value != null ? value : "").setFont(regular).setFontSize(14));
//         return cell;
//     }
        private Cell infoCell(String label, String value,
                        PdfFont regular, PdfFont bold) {

        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(
                new Paragraph(label)
                        .setFont(bold)
                        .setFontSize(14)
                        .setMarginBottom(0)
        );

        cell.add(
                new Paragraph(value != null ? value : "")
                        .setFont(regular)
                        .setFontSize(14)
                        .setMarginTop(0)
        );

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

