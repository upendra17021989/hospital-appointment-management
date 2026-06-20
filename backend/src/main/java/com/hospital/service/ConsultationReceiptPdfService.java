package com.hospital.service;

import com.hospital.model.ConsultationReceipt;
import com.hospital.model.ConsultationReceiptLineItem;
import com.hospital.model.Hospital;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ConsultationReceiptPdfService {

    private enum ContactIcon {
        PHONE,
        EMAIL,
        LOCATION
    }

    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final PageSize RECEIPT_PAGE = new PageSize(794, 628);
    private static final float PAGE_MARGIN_X = 48;
    private static final float CONTENT_WIDTH = 698;
    private static final float FORM_WIDTH = 606;
    private static final float TABLE_WIDTH = 559;

    private static final DeviceRgb BRAND_RED = new DeviceRgb(219, 30, 37);
    private static final DeviceRgb BRAND_BLUE = new DeviceRgb(26, 82, 142);
    public byte[] generatePdf(ConsultationReceipt receipt) {
        try {
            PdfFont times = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            PdfFont timesBold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont timesItalic = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
            PdfFont timesBoldItalic = PdfFontFactory.createFont(StandardFonts.TIMES_BOLDITALIC);
            PdfFont helveticaBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont helveticaBoldOblique = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLDOBLIQUE);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            pdf.setDefaultPageSize(RECEIPT_PAGE);

            Document doc = new Document(pdf, RECEIPT_PAGE);
            doc.setMargins(16, PAGE_MARGIN_X, 10, PAGE_MARGIN_X);

            if (isHeaderEnabled(receipt)) {
                addHeader(doc, pdf, receipt, helveticaBold, helveticaBoldOblique);
            }
            addTitle(doc, timesItalic);
            addReceiptMeta(doc, receipt, timesItalic, timesBold);
            addNarration(doc, receipt, timesItalic, timesBoldItalic);
            addParticulars(doc, receipt, times, timesItalic);
            addQrAndSignature(doc, receipt, timesItalic);
            addFooterNote(doc, timesBoldItalic);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate consultation receipt PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document doc, PdfDocument pdf, ConsultationReceipt receipt, PdfFont bold, PdfFont boldOblique) throws Exception {
        Table header = new Table(UnitValue.createPointArray(new float[]{330, 320}))
                .setWidth(CONTENT_WIDTH)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        Cell logoCell = noBorderCell()
                .setHeight(113)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingLeft(78)
                .setPaddingTop(0);

        Image logo = loadLogo(receipt);
        if (logo != null) {
            logoCell.add(logo.setWidth(113).setAutoScaleHeight(false));
        } else {
            logoCell.add(new Paragraph(hospitalName(receipt))
                    .setFont(bold)
                    .setFontSize(24)
                    .setFontColor(BRAND_RED)
                    .setMargin(0));
        }
        header.addCell(logoCell);

        Cell contact = noBorderCell()
                .setHeight(113)
                .setPaddingLeft(32)
                .setPaddingTop(0)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        contact.add(new Paragraph("CONTACT US ON")
                .setFont(bold)
                .setFontSize(20)
                .setFontColor(BRAND_RED)
                .setMarginTop(0)
                .setMarginBottom(4));
        contact.add(contactLine(pdf, ContactIcon.PHONE, firstPresent(hospital(receipt).getPhone(), receipt.getHospitalPhone(), "+919876543210"), boldOblique));
        contact.add(contactLine(pdf, ContactIcon.EMAIL, firstPresent(hospital(receipt).getEmail(), "default@gmail.com"), boldOblique));
        addAddressLines(contact, pdf, boldOblique, receipt);
        header.addCell(contact);

        doc.add(header);
        doc.add(rule(CONTENT_WIDTH, 1.5f).setMarginTop(0).setMarginBottom(8));
    }

    private void addAddressLines(Cell contact, PdfDocument pdf, PdfFont font, ConsultationReceipt receipt) {
        List<String> lines = splitAddress(addressText(receipt), 44);
        if (lines.isEmpty()) return;

        contact.add(contactLine(pdf, ContactIcon.LOCATION, lines.get(0), font));
        for (int i = 1; i < Math.min(lines.size(), 3); i++) {
            contact.add(new Paragraph(lines.get(i))
                    .setFont(font)
                    .setFontSize(12.5f)
                    .setFontColor(BRAND_BLUE)
                    .setMarginTop(0)
                    .setMarginBottom(0)
                    .setMarginLeft(29));
        }
    }

    private void addTitle(Document doc, PdfFont italic) {
        Paragraph title = new Paragraph()
                .add(new Text("Invoice").setUnderline(0.5f, -2))
                .add(new Text("  |  "))
                .add(new Text("Receipt").setUnderline(0.5f, -2))
                .setFont(italic)
                .setFontSize(26)
                .setTextAlignment(TextAlignment.CENTER)
                .setWidth(CONTENT_WIDTH)
                .setMarginTop(0)
                .setMarginBottom(13);
        doc.add(title);
        doc.add(rule(FORM_WIDTH, 0.75f).setMarginTop(0).setMarginBottom(14));
    }

    private void addReceiptMeta(Document doc, ConsultationReceipt receipt, PdfFont italic, PdfFont bold) {
        Table meta = new Table(UnitValue.createPointArray(new float[]{303, 303}))
                .setWidth(FORM_WIDTH)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(12);
        meta.addCell(metaCell("Receipt No. : ", safe(receipt.getReceiptNumber()), italic, bold, TextAlignment.CENTER));
        meta.addCell(metaCell("Receipt Date : ", formatReceiptDate(receipt), italic, bold, TextAlignment.CENTER));
        doc.add(meta);
    }

    private void addNarration(Document doc, ConsultationReceipt receipt, PdfFont italic, PdfFont boldItalic) {
        String patient = safe(receipt.getPatientName());
        String hospital = safe(receipt.getHospitalName()).isBlank() ? "Swastik Clinic" : safe(receipt.getHospitalName());

        Paragraph line = new Paragraph()
                .add(new Text("An amount of ").setFont(italic))
                .add(new Text(amountInWords(receipt.getAmountPaid())).setFont(boldItalic).setUnderline(0.5f, -1))
                .add(new Text(" received with thanks from Mr. / Ms. / Mrs. ").setFont(italic))
                .add(new Text(patient).setFont(boldItalic).setUnderline(0.5f, -1))
                .add(new Text(" towards the ").setFont(italic))
                .add(new Text("treatment / examination / health check-up of Mr. / Ms. / Mrs. ").setFont(italic))
                .add(new Text(patient).setFont(boldItalic).setUnderline(0.5f, -1))
                .add(new Text(" at ").setFont(italic))
                .add(new Text(hospital).setFont(boldItalic).setUnderline(0.5f, -1))
                .add(new Text(".").setFont(italic))
                .setFontSize(12)
                .setMarginTop(0)
                .setMarginBottom(0)
                .setTextAlignment(TextAlignment.CENTER)
                .setWidth(FORM_WIDTH)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMultipliedLeading(1.1f);
        doc.add(line);
    }

    private void addParticulars(Document doc,
                                ConsultationReceipt receipt,
                                PdfFont times,
                                PdfFont italic) {
        doc.add(new Paragraph("Particulars of the payment are as below.")
                .setFont(italic)
                .setFontSize(12)
                .setWidth(FORM_WIDTH)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(0)
                .setMarginBottom(16));

        Table table = new Table(UnitValue.createPointArray(new float[]{57, 378, 124}))
                .setWidth(TABLE_WIDTH)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(0)
                .setMarginBottom(12);

        table.addHeaderCell(tableCell("Sr. No.", italic, 11, TextAlignment.CENTER, true));
        table.addHeaderCell(tableCell("Particulars", italic, 11, TextAlignment.CENTER, true));
        table.addHeaderCell(tableCell("Amount", italic, 11, TextAlignment.CENTER, true));

        List<ConsultationReceiptLineItem> items = lineItems(receipt);
        for (int i = 0; i < 5; i++) {
            ConsultationReceiptLineItem item = i < items.size() ? items.get(i) : null;
            table.addCell(tableCell(item == null ? "" : String.valueOf(item.getSrNo()), times, 10, TextAlignment.CENTER, false));
            table.addCell(tableCell(item == null ? "" : safe(item.getParticulars()), times, 10, TextAlignment.LEFT, false));
            table.addCell(tableCell(item == null ? "" : money(item.getAmount()), times, 10, TextAlignment.CENTER, false));
        }

        table.addCell(tableCell("", times, 10, TextAlignment.CENTER, false));
        table.addCell(tableCell("Grand Total", italic, 11, TextAlignment.RIGHT, false));
        table.addCell(tableCell(money(receipt.getAmountPaid()) + " INR", times, 10, TextAlignment.CENTER, false));
        doc.add(table);
    }

    private void addQrAndSignature(Document doc, ConsultationReceipt receipt, PdfFont italic) throws Exception {
        Table bottom = new Table(UnitValue.createPointArray(new float[]{330, 330}))
                .setWidth(660)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(0)
                .setMarginBottom(11);

        Cell qrCell = noBorderCell()
                .setTextAlignment(TextAlignment.CENTER)
                .setPaddingLeft(84)
                .setPaddingTop(0);
        Image qrImage = loadQrCode(receipt);
        if (qrImage != null) {
            qrCell.add(qrImage
                    .setWidth(84)
                    .setHeight(84)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER));
        } else {
            qrCell.add(new Paragraph("QR").setFont(italic).setFontSize(18).setMarginBottom(42));
        }
        qrCell.add(new Paragraph("Scan the QR Code to pay online")
                .setFont(italic)
                .setFontSize(12)
                .setMarginTop(2)
                .setMarginBottom(0));
        bottom.addCell(qrCell);

        Cell signCell = noBorderCell()
                .setTextAlignment(TextAlignment.CENTER)
                .setPaddingLeft(74)
                .setPaddingTop(0);
        Div signBox = new Div()
                .setWidth(122)
                .setHeight(82)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1.7f))
                .setBorderRadius(new BorderRadius(14))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        signCell.add(signBox);
        signCell.add(new Paragraph("Authorized Signatory")
                .setFont(italic)
                .setFontSize(12)
                .setMarginTop(2)
                .setMarginBottom(0));
        bottom.addCell(signCell);

        doc.add(bottom);
        doc.add(rule(FORM_WIDTH, 0.75f).setMarginTop(0).setMarginBottom(17));
    }

    private void addFooterNote(Document doc, PdfFont boldItalic) {
        doc.add(new Paragraph("N.B.: This receipt is not for the medico legal purpose.")
                .setFont(boldItalic)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setWidth(FORM_WIDTH)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMargin(0));
    }

    private Paragraph contactLine(PdfDocument pdf, ContactIcon icon, String text, PdfFont font) {
        Paragraph paragraph = new Paragraph()
                .setFont(font)
                .setFontSize(12.5f)
                .setFontColor(BRAND_BLUE)
                .setMarginTop(0)
                .setMarginBottom(2)
                .setMultipliedLeading(1);
        paragraph.add(createContactIcon(pdf, icon));
        paragraph.add(new Text("  "));
        paragraph.add(new Text(text));
        return paragraph;
    }

    private Image createContactIcon(PdfDocument pdf, ContactIcon icon) {
        PdfFormXObject xObject = new PdfFormXObject(new Rectangle(0, 0, 18, 18));
        PdfCanvas canvas = new PdfCanvas(xObject, pdf);

        canvas.saveState()
                .setFillColor(BRAND_BLUE)
                .circle(9, 9, 8.4f)
                .fill()
                .restoreState();

        canvas.saveState()
                .setStrokeColor(ColorConstants.WHITE)
                .setLineWidth(1.45f)
                .setLineCapStyle(1)
                .setLineJoinStyle(1);

        switch (icon) {
            case PHONE -> drawPhoneIcon(canvas);
            case EMAIL -> drawEmailIcon(canvas);
            case LOCATION -> drawLocationIcon(canvas);
        }

        canvas.restoreState();
        return new Image(xObject).setWidth(18).setHeight(18);
    }

    private Image loadLogo(ConsultationReceipt receipt) throws Exception {
        String logoUrl = hospital(receipt).getLogoUrl();
        if (logoUrl != null && !logoUrl.isBlank()) {
            try {
                return new Image(ImageDataFactory.create(logoUrl.trim()));
            } catch (Exception ignored) {
                // Fall back to bundled logo when a configured URL/path cannot be loaded.
            }
        }

        URL bundledLogo = resource("images/default-logo.png");
        return bundledLogo != null ? new Image(ImageDataFactory.create(bundledLogo)) : null;
    }

    private Image loadQrCode(ConsultationReceipt receipt) throws Exception {
        String qrCodeUrl = hospital(receipt).getConsultationReceiptQrCodeUrl();
        if (qrCodeUrl != null && !qrCodeUrl.isBlank()) {
            try {
                return new Image(ImageDataFactory.create(qrCodeUrl.trim()));
            } catch (Exception ignored) {
                // Fall back to bundled QR when a configured URL/path cannot be loaded.
            }
        }

        URL bundledQr = resource("images/qr-code.png");
        return bundledQr != null ? new Image(ImageDataFactory.create(bundledQr)) : null;
    }

    private boolean isHeaderEnabled(ConsultationReceipt receipt) {
        Boolean enabled = hospital(receipt).getConsultationReceiptHeaderEnabled();
        return enabled == null || enabled;
    }

    private String hospitalName(ConsultationReceipt receipt) {
        return firstPresent(hospital(receipt).getName(), receipt.getHospitalName(), "SWASTIK CLINIC");
    }

    private String addressText(ConsultationReceipt receipt) {
        Hospital hospital = hospital(receipt);
        String address = firstPresent(hospital.getAddress(), receipt.getHospitalAddress());
        String cityStatePin = joinNonBlank(", ", hospital.getCity(), hospital.getState(), hospital.getPincode());
        String full = joinNonBlank(", ", address, cityStatePin);
        return firstPresent(full, "default address line 1", "default address line 2");
    }

    private Hospital hospital(ConsultationReceipt receipt) {
        return receipt != null && receipt.getHospital() != null ? receipt.getHospital() : new Hospital();
    }

    private List<String> splitAddress(String value, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (value == null || value.isBlank()) return lines;

        StringBuilder current = new StringBuilder();
        for (String word : value.trim().split("\\s+")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > maxChars) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private String joinNonBlank(String separator, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) parts.add(value.trim());
        }
        return String.join(separator, parts);
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private void drawPhoneIcon(PdfCanvas canvas) {
        canvas.moveTo(6.1f, 12.9f)
                .curveTo(5.1f, 11.7f, 5.1f, 9.4f, 6.5f, 7.2f)
                .curveTo(7.8f, 5.2f, 9.9f, 4.1f, 12.1f, 5.1f)
                .stroke();
        canvas.moveTo(6.2f, 12.8f)
                .lineTo(7.9f, 11.3f)
                .lineTo(7.0f, 9.6f)
                .stroke();
        canvas.moveTo(12.0f, 5.1f)
                .lineTo(10.9f, 7.1f)
                .lineTo(12.6f, 8.2f)
                .stroke();
    }

    private void drawEmailIcon(PdfCanvas canvas) {
        canvas.rectangle(4.6f, 6.1f, 8.8f, 6.2f).stroke();
        canvas.moveTo(4.9f, 12.0f).lineTo(9.0f, 8.5f).lineTo(13.1f, 12.0f).stroke();
        canvas.moveTo(4.9f, 6.4f).lineTo(7.8f, 9.0f).stroke();
        canvas.moveTo(13.1f, 6.4f).lineTo(10.2f, 9.0f).stroke();
    }

    private void drawLocationIcon(PdfCanvas canvas) {
        canvas.circle(9.0f, 10.4f, 2.8f).stroke();
        canvas.moveTo(9.0f, 4.2f)
                .curveTo(6.0f, 7.6f, 5.6f, 9.8f, 6.9f, 12.0f)
                .curveTo(8.0f, 13.7f, 10.0f, 13.7f, 11.1f, 12.0f)
                .curveTo(12.4f, 9.8f, 12.0f, 7.6f, 9.0f, 4.2f)
                .stroke();
    }

    private Cell metaCell(String label, String value, PdfFont labelFont, PdfFont valueFont, TextAlignment align) {
        Paragraph p = new Paragraph()
                .add(new Text(label).setFont(labelFont))
                .add(new Text(value).setFont(valueFont).setUnderline(0.5f, -1))
                .setFontSize(12)
                .setTextAlignment(align)
                .setMargin(0);
        return noBorderCell().add(p).setPadding(0);
    }

    private Cell tableCell(String text, PdfFont font, float size, TextAlignment align, boolean header) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.8f))
                .setHeight(17)
                .setPaddingTop(1)
                .setPaddingBottom(0)
                .setPaddingLeft(7)
                .setPaddingRight(7)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (header) {
            cell.setPaddingLeft(3).setPaddingRight(3);
        }
        cell.add(new Paragraph(text == null ? "" : text)
                .setFont(font)
                .setFontSize(size)
                .setTextAlignment(align)
                .setMargin(0)
                .setMultipliedLeading(1));
        return cell;
    }

    private LineSeparator rule(float width, float lineWidth) {
        LineSeparator line = new LineSeparator(new SolidLine(lineWidth));
        line.setWidth(width);
        line.setHorizontalAlignment(HorizontalAlignment.CENTER);
        return line;
    }

    private Cell noBorderCell() {
        return new Cell().setBorder(Border.NO_BORDER);
    }

    private List<ConsultationReceiptLineItem> lineItems(ConsultationReceipt receipt) {
        List<ConsultationReceiptLineItem> items = receipt.getLineItems() != null
                ? new ArrayList<>(receipt.getLineItems())
                : new ArrayList<>();
        if (items.isEmpty()) {
            items.add(ConsultationReceiptLineItem.builder()
                    .srNo(1)
                    .particulars("Consultation Fee")
                    .amount(receipt.getConsultationFee())
                    .build());
        }
        return items;
    }

    private String formatReceiptDate(ConsultationReceipt receipt) {
        if (receipt == null || receipt.getReceiptDateTime() == null) return "";
        return receipt.getReceiptDateTime().toLocalDate().format(RECEIPT_DATE);
    }

    private String amountInWords(BigDecimal amount) {
        if (amount == null) return "";
        long rupees = amount.setScale(0, RoundingMode.DOWN).longValue();
        String words = numberToWords(rupees);
        return words.isBlank() ? money(amount) + " Rupees" : words + " Rupees";
    }

    private String numberToWords(long number) {
        if (number == 0) return "Zero";
        if (number < 0) return "Minus " + numberToWords(Math.abs(number));

        String[] units = {
                "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
                "Seventeen", "Eighteen", "Nineteen"
        };
        String[] tens = {
                "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        };
        if (number < 20) return units[(int) number];
        if (number < 100) return tens[(int) number / 10] + (number % 10 == 0 ? "" : " " + units[(int) number % 10]);
        if (number < 1_000) return units[(int) number / 100] + " Hundred" + (number % 100 == 0 ? "" : " " + numberToWords(number % 100));
        if (number < 100_000) return numberToWords(number / 1_000) + " Thousand" + (number % 1_000 == 0 ? "" : " " + numberToWords(number % 1_000));
        if (number < 10_000_000) return numberToWords(number / 100_000) + " Lakh" + (number % 100_000 == 0 ? "" : " " + numberToWords(number % 100_000));
        return numberToWords(number / 10_000_000) + " Crore" + (number % 10_000_000 == 0 ? "" : " " + numberToWords(number % 10_000_000));
    }

    private URL resource(String path) {
        return getClass().getClassLoader().getResource(path);
    }

    private String money(BigDecimal amt) {
        if (amt == null) return "";
        return amt.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
