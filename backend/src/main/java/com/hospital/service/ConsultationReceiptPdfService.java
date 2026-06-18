package com.hospital.service;

import com.hospital.model.ConsultationReceipt;
import com.hospital.model.ConsultationReceiptLineItem;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationReceiptPdfService {

    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    
    // Brand colors from the logo
    private static final DeviceRgb BRAND_RED = new DeviceRgb(220, 53, 69);
    private static final DeviceRgb BRAND_BLUE = new DeviceRgb(0, 123, 255);

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
            float contentWidth = pageWidth - 40;

            // ============================================================
            // HEADER SECTION - Logo + Clinic Name | Contact Info
            // ============================================================
            addClinicHeader(doc, contentWidth, bold, regular);

            // --- Header: Invoice | Receipt ---
            doc.add(new Paragraph("Invoice | Receipt")
                    .setFont(italic)
                    .setFontSize(26)
                    .setUnderline()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(15));

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
                    .add(new Text("An amount of ").setFont(italic))
                    .add(new Text(money(receipt.getAmountPaid())).setFont(bold).setUnderline())
                    .add(new Text(" received with thanks from Mr. / Ms. / Mrs. ").setFont(italic))
                    .add(new Text(patient).setFont(bold).setUnderline())
                    .add(new Text(" towards the treatment / examination / health check-up of Mr. / Ms. / Mrs. ").setFont(italic))
                    .add(new Text(patient).setFont(bold).setUnderline())
                    .add(new Text(" at ").setFont(italic))
                    .add(new Text(hospitalName).setFont(bold).setUnderline())
                    .add(new Text(".").setFont(italic))
                    .setFontSize(14)
                    .setMultipliedLeading(1.2f)
                    .setMarginTop(16)
                    .setWidth(contentWidth)
                    .setTextAlignment(TextAlignment.CENTER);

            doc.add(receiptParagraph);

            doc.add(new Paragraph("Particulars of the payment are as below.")
                    .setFont(italic)
                    .setFontSize(14)
                    .setMultipliedLeading(1.2f)
                    .setMarginTop(10));

            // --- Table: Sr. No / Particulars / Amount ---
            Table lines = new Table(new float[]{0.25f, 0.55f, 0.2f});
            lines.setWidth(contentWidth);
            lines.setHorizontalAlignment(HorizontalAlignment.CENTER);
            lines.setMarginTop(14);

            lines.addHeaderCell(borderedHeaderCell("Sr. No."));
            lines.addHeaderCell(borderedHeaderCell("Particulars"));
            lines.addHeaderCell(borderedHeaderCell("Amount"));

            List<ConsultationReceiptLineItem> items = receipt.getLineItems() != null 
                    ? receipt.getLineItems() 
                    : new ArrayList<>();
            
            if (items.isEmpty()) {
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

            // --- Grand total ---
            doc.add(new Paragraph("Grand Total : " + money(receipt.getAmountPaid()) + " INR")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(10));

            // --- Footer: QR + Signature ---
            Table footer = new Table(new float[]{1, 1});
            footer.setWidth(contentWidth);
            footer.setHorizontalAlignment(HorizontalAlignment.CENTER);
            footer.setMarginTop(30);

            Cell qrCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.CENTER);
            
            URL qrUrl = getClass().getClassLoader().getResource("images/qr-code.png");

            if (qrUrl != null) {
                Image qrImage = new Image(ImageDataFactory.create(qrUrl))
                        .setWidth(100)
                        .setHeight(100)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);
                qrCell.add(qrImage);
                qrCell.add(new Paragraph("Scan the QR Code to pay online")
                        .setFont(italic)
                        .setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(5));
            } else {
                qrCell.add(new Paragraph("QR Code").setFont(bold).setFontSize(13));
                qrCell.add(new Paragraph("(QR placeholder)").setFont(regular));
            }

            footer.addCell(qrCell);

            Cell sigCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.CENTER);

            Div signBox = new Div()
                    .setWidth(150)
                    .setHeight(100)
                    .setBorder(new SolidBorder(2))
                    .setBorderRadius(new BorderRadius(15));
            signBox.setHorizontalAlignment(HorizontalAlignment.CENTER);

            sigCell.add(signBox);
            sigCell.add(new Paragraph("Authorized Signatory")
                    .setFont(italic)
                    .setFontSize(12)
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5));

            footer.addCell(sigCell);
            doc.add(footer);

            // Horizontal line
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
                    .setTextAlignment(TextAlignment.LEFT)
                    .setBold());

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate consultation receipt PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Adds the clinic header with logo and contact information.
     */
    private void addClinicHeader(Document doc, float contentWidth, PdfFont bold, PdfFont regular) {
        try {
            // Main header table: Logo+Name | Contact Info
            Table headerTable = new Table(new float[]{0.45f, 0.55f});
            headerTable.setWidth(contentWidth);
            headerTable.setHorizontalAlignment(HorizontalAlignment.CENTER);

            // --- LEFT CELL: Logo + Clinic Name ---
            Cell leftCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);

            // Try to load the clinic logo
            URL logoUrl = getClass().getClassLoader().getResource("images/swastik-logo.png");
            
            if (logoUrl != null) {
                Image logo = new Image(ImageDataFactory.create(logoUrl))
                        .setWidth(180)
                        .setHeight(100);
                
                // Create a table for logo and text side by side
                Table logoTextTable = new Table(new float[]{180, 150});
                logoTextTable.setBorder(Border.NO_BORDER);
                
                Cell logoCell = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .add(logo)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
                logoTextTable.addCell(logoCell);
                
                // // Clinic name and tagline
                // Cell nameCell = new Cell()
                //         .setBorder(Border.NO_BORDER)
                //         .setVerticalAlignment(VerticalAlignment.MIDDLE)
                //         .setPaddingLeft(5);
                
                // nameCell.add(new Paragraph("SWASTIK CLINIC")
                //         .setFont(bold)
                //         .setFontSize(18)
                //         .setFontColor(BRAND_BLUE)
                //         .setMarginBottom(0));
                
                // nameCell.add(new Paragraph("Curing Humanity...")
                //         .setFont(regular)
                //         .setFontSize(10)
                //         .setFontColor(BRAND_RED)
                //         .setItalic()
                //         .setMarginTop(0));
                
                // logoTextTable.addCell(nameCell);
                leftCell.add(logoTextTable);
            } else {
                // Fallback: text only
                leftCell.add(new Paragraph("SWASTIK CLINIC")
                        .setFont(bold)
                        .setFontSize(18)
                        .setFontColor(BRAND_BLUE));
                leftCell.add(new Paragraph("Curing Humanity...")
                        .setFont(regular)
                        .setFontSize(10)
                        .setFontColor(BRAND_RED)
                        .setItalic());
            }

            headerTable.addCell(leftCell);

            // --- RIGHT CELL: Contact Information ---
            Cell rightCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPaddingLeft(20);

            // "CONTACT US ON" header
            rightCell.add(new Paragraph("CONTACT US ON")
                    .setFont(bold)
                    .setFontSize(14)
                    .setFontColor(BRAND_RED)
                    .setMarginBottom(5));


            // Phone
                URL phoneUrl = getClass().getClassLoader().getResource("images/phone.png");
                if (phoneUrl != null) {
                Image phoneImg = new Image(ImageDataFactory.create(phoneUrl)).scaleToFit(12, 12);
                rightCell.add(new Paragraph()
                        .add(phoneImg)
                        .add(new Text(" 9727777569").setFont(regular).setFontColor(BRAND_BLUE))
                        .setBold().setItalic());
                }

                // Email
                URL emailUrl = getClass().getClassLoader().getResource("images/email.png");
                if (emailUrl != null) {
                Image emailImg = new Image(ImageDataFactory.create(emailUrl)).scaleToFit(12, 12);
                rightCell.add(new Paragraph()
                        .add(emailImg)
                        .add(new Text(" swastikclinic.rajula@gmail.com").setFont(regular).setFontColor(BRAND_BLUE))
                        .setBold().setItalic());
                }

                // Address
                URL addressUrl = getClass().getClassLoader().getResource("images/address.png");
                if (addressUrl != null) {
                Image addressImg = new Image(ImageDataFactory.create(addressUrl)).scaleToFit(12, 12);
                rightCell.add(new Paragraph()
                        .add(addressImg)
                        .add(new Text(" Behind Gayatrimata Temple, Near Old Court Building, Krishnanagar Society, Rajula. 365560")
                        .setFont(regular).setFontColor(BRAND_BLUE))
                        .setBold().setItalic());
                }

            headerTable.addCell(rightCell);

            doc.add(headerTable);

            // Add a separator line below header
            LineSeparator headerLine = new LineSeparator(new SolidLine(1));
            headerLine.setMarginTop(10);
            headerLine.setMarginBottom(5);
            doc.add(headerLine);

        } catch (Exception e) {
            // If header fails, continue without it
            System.err.println("Warning: Could not add clinic header: " + e.getMessage());
        }
    }

    /**
     * Creates a contact information line with icon.
     */
    private Paragraph createContactLine(String icon, String text, PdfFont font) {
        return new Paragraph()
                .add(new Text(icon + " ").setFontColor(BRAND_BLUE))
                .add(new Text(text).setFont(font).setFontSize(10))
                .setMarginTop(2)
                .setMarginBottom(2);
    }

    private String formatReceiptDate(ConsultationReceipt receipt) {
        if (receipt == null || receipt.getReceiptDateTime() == null) return "";
        return receipt.getReceiptDateTime().toLocalDate().format(RECEIPT_DATE);
    }

    private Cell infoCell(String label, String value, PdfFont regular, PdfFont bold) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(label)
                .setFont(bold)
                .setFontSize(14)
                .setMarginBottom(0));

        cell.add(new Paragraph(value != null ? value : "")
                .setFont(regular)
                .setFontSize(14)
                .setMarginTop(0));

        return cell;
    }

    private Cell borderedHeaderCell(String text) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text).setFontSize(12).setBold());
        cell.setBorder(new SolidBorder(1));
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        return cell;
    }

    private Cell borderedBodyCell(String text) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text != null ? text : "").setFontSize(12));
        cell.setBorder(new SolidBorder(1));
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
