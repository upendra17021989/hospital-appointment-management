package com.hospital.service;

import com.hospital.model.MedicalCertificate;
import com.hospital.model.Patient;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MedicalCertificatePdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final DeviceRgb BRAND_RED = new DeviceRgb(219, 30, 37);
    private static final DeviceRgb BRAND_BLUE = new DeviceRgb(26, 82, 142);
    private static final String MEDICOLEGAL_DISCLAIMER =
            "This Prescription / Certificate is not for medicolegal purpose.";

    private final MedicalCertificateService certificateService;

    public byte[] generatePdf(MedicalCertificate certificate) {
        try {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont italic = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(36, 48, 36, 48);

            addHeader(doc, certificate, bold);
            addTitle(doc, certificate, bold);
            addMeta(doc, certificate, regular, bold);
            addPatientDoctorBlock(doc, certificate, regular, bold);
            addBody(doc, certificate, regular, bold);
            addDynamicFields(doc, certificateService.readFields(certificate.getDynamicFields()), regular, bold);
            addRemarks(doc, certificate, regular, bold);
            addSignature(doc, certificate, regular, bold, italic);
            addFooter(doc, italic);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate medical certificate PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document doc, MedicalCertificate certificate, PdfFont bold) throws Exception {
        Table header = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth();
        Cell logoCell = noBorderCell().setVerticalAlignment(VerticalAlignment.MIDDLE);
        Image logo = loadLogo(certificate);
        if (logo != null) {
            logoCell.add(logo.setWidth(130).setAutoScaleHeight(true));
        } else {
            logoCell.add(new Paragraph(firstPresent(certificate.getHospitalName(), "Hospital"))
                    .setFont(bold)
                    .setFontSize(20)
                    .setFontColor(BRAND_RED)
                    .setMargin(0));
        }
        header.addCell(logoCell);

        Cell hospitalCell = noBorderCell().setTextAlignment(TextAlignment.RIGHT);
        hospitalCell.add(new Paragraph(firstPresent(certificate.getHospitalName(), "Hospital"))
                .setFont(bold)
                .setFontSize(18)
                .setFontColor(BRAND_RED)
                .setMarginBottom(3));
        if (!blank(certificate.getHospitalAddress())) {
            hospitalCell.add(new Paragraph(certificate.getHospitalAddress()).setFontSize(10).setFontColor(BRAND_BLUE).setMargin(0));
        }
        if (!blank(certificate.getHospitalPhone())) {
            hospitalCell.add(new Paragraph("Phone: " + certificate.getHospitalPhone()).setFontSize(10).setFontColor(BRAND_BLUE).setMargin(0));
        }
        header.addCell(hospitalCell);
        doc.add(header);
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1)).setMarginTop(8).setMarginBottom(18));
    }

    private void addTitle(Document doc, MedicalCertificate certificate, PdfFont bold) {
        doc.add(new Paragraph(displayType(certificate.getCertificateType()))
                .setFont(bold)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setUnderline(0.5f, -2)
                .setMarginBottom(18));
    }

    private void addMeta(Document doc, MedicalCertificate certificate, PdfFont regular, PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginBottom(14);
        table.addCell(metaCell("Certificate No.", certificate.getCertificateNumber(), regular, bold));
        table.addCell(metaCell("Issue Date", format(certificate.getIssueDate()), regular, bold));
        table.addCell(metaCell("Valid From", format(certificate.getValidFrom()), regular, bold));
        table.addCell(metaCell("Valid Until", format(certificate.getValidUntil()), regular, bold));
        doc.add(table);
    }

    private void addPatientDoctorBlock(Document doc, MedicalCertificate certificate, PdfFont regular, PdfFont bold) {
        Patient patient = certificate.getPatient();
        String ageGender = joinNonBlank(" / ",
                patient != null && patient.getAge() != null ? patient.getAge() + " yrs" : null,
                patient != null ? patient.getGender() : null);

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginBottom(18);
        table.addCell(blockCell("Patient", joinNonBlank("\n",
                certificate.getPatientName(),
                ageGender,
                patient != null ? patient.getPhone() : null), regular, bold));
        table.addCell(blockCell("Doctor", joinNonBlank("\n",
                certificate.getDoctorName(),
                certificate.getDoctor() != null ? certificate.getDoctor().getQualification() : null,
                firstPresent(certificate.getDepartmentName(), certificate.getDoctor() != null ? certificate.getDoctor().getSpecialization() : null)), regular, bold));
        doc.add(table);
    }

    private void addBody(Document doc, MedicalCertificate certificate, PdfFont regular, PdfFont bold) {
        Paragraph body = new Paragraph()
                .setFont(regular)
                .setFontSize(12)
                .setMultipliedLeading(1.35f)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(12);
        body.add(new Text("This is to certify that ").setFont(regular));
        body.add(new Text(firstPresent(certificate.getPatientName(), "the patient")).setFont(bold));
        body.add(new Text(typeBody(certificate)).setFont(regular));
        doc.add(body);
    }

    private void addDynamicFields(Document doc, Map<String, Object> fields, PdfFont regular, PdfFont bold) {
        if (fields == null || fields.isEmpty()) return;
        Table table = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth().setMarginTop(4).setMarginBottom(14);
        table.addHeaderCell(headerCell("Detail", bold));
        table.addHeaderCell(headerCell("Value", bold));
        fields.forEach((key, value) -> {
            if (value != null && !String.valueOf(value).isBlank()) {
                table.addCell(dataCell(labelize(key), regular));
                table.addCell(dataCell(String.valueOf(value), regular));
            }
        });
        doc.add(table);
    }

    private void addRemarks(Document doc, MedicalCertificate certificate, PdfFont regular, PdfFont bold) {
        if (!blank(certificate.getDiagnosisOrReason())) {
            doc.add(labelParagraph("Diagnosis / Reason", certificate.getDiagnosisOrReason(), regular, bold));
        }
        if (!blank(certificate.getRemarks())) {
            doc.add(labelParagraph("Remarks", certificate.getRemarks(), regular, bold));
        }
    }

    private void addSignature(Document doc, MedicalCertificate certificate, PdfFont regular, PdfFont bold, PdfFont italic) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{55, 45})).useAllAvailableWidth().setMarginTop(40);
        table.addCell(noBorderCell().add(new Paragraph("Issued by: " + firstPresent(certificate.getIssuedByName(), certificate.getDoctorName()))
                .setFont(regular)
                .setFontSize(11)));
        Cell signature = noBorderCell().setTextAlignment(TextAlignment.CENTER);
        signature.add(new Paragraph("____________________________").setFont(regular).setMarginBottom(3));
        signature.add(new Paragraph(certificate.getDoctorName()).setFont(bold).setFontSize(11).setMargin(0));
        signature.add(new Paragraph("Authorized Medical Practitioner").setFont(italic).setFontSize(10).setMargin(0));
        table.addCell(signature);
        doc.add(table);
    }

    private void addFooter(Document doc, PdfFont italic) {
        doc.add(new Paragraph(MEDICOLEGAL_DISCLAIMER)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(18)
                .setMarginBottom(4));
        doc.add(new Paragraph("This certificate is issued based on clinical information available at the time of examination.")
                .setFont(italic)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(26));
    }

    private String typeBody(MedicalCertificate certificate) {
        return switch (certificate.getCertificateType()) {
            case SICK_LEAVE -> " was examined and is advised sick leave / absence from duties for the period mentioned below.";
            case FITNESS -> " has been examined and is found medically fit for the stated purpose, subject to the notes recorded below.";
            case FIT_TO_FLY -> " has been assessed and is considered fit to fly, subject to airline and destination requirements.";
            case FORM_1A_DRIVING_LICENSE -> " has been examined for Form 1A driving license medical fitness requirements.";
            case VACCINATION -> " has received the vaccination recorded below.";
            case RECOVERY -> " has recovered from the stated medical condition as per examination and available records.";
            case CARETAKER_MEDICAL_LEAVE -> " requires care and support from the caretaker for the period mentioned below.";
            case CARA_ADOPTION_FITNESS -> " has been examined for medical fitness related to CARA adoption requirements.";
        };
    }

    private String displayType(MedicalCertificate.CertificateType type) {
        return switch (type) {
            case SICK_LEAVE -> "Sick Leave / Absence Certificate";
            case FITNESS -> "Fitness Certificate";
            case FIT_TO_FLY -> "Fit-to-Fly Certificate";
            case FORM_1A_DRIVING_LICENSE -> "Form 1A Driving License Medical Certificate";
            case VACCINATION -> "Vaccination Certificate";
            case RECOVERY -> "Recovery Certificate";
            case CARETAKER_MEDICAL_LEAVE -> "Caretaker / Medical Leave Certificate";
            case CARA_ADOPTION_FITNESS -> "CARA Adoption Medical Fitness Certificate";
        };
    }

    private Cell metaCell(String label, String value, PdfFont regular, PdfFont bold) {
        return noBorderCell().add(new Paragraph()
                .add(new Text(label + ": ").setFont(bold))
                .add(new Text(firstPresent(value, "-")).setFont(regular))
                .setFontSize(11)
                .setMargin(0));
    }

    private Cell blockCell(String title, String value, PdfFont regular, PdfFont bold) {
        Cell cell = new Cell().setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.8f)).setPadding(10);
        cell.add(new Paragraph(title).setFont(bold).setFontSize(11).setFontColor(BRAND_RED).setMarginBottom(4));
        cell.add(new Paragraph(firstPresent(value, "-")).setFont(regular).setFontSize(11).setMargin(0));
        return cell;
    }

    private Cell headerCell(String value, PdfFont bold) {
        return new Cell().setBackgroundColor(new DeviceRgb(245, 247, 250)).setPadding(6)
                .add(new Paragraph(value).setFont(bold).setFontSize(10).setMargin(0));
    }

    private Cell dataCell(String value, PdfFont regular) {
        return new Cell().setPadding(6).add(new Paragraph(value).setFont(regular).setFontSize(10).setMargin(0));
    }

    private Paragraph labelParagraph(String label, String value, PdfFont regular, PdfFont bold) {
        return new Paragraph()
                .add(new Text(label + ": ").setFont(bold))
                .add(new Text(value).setFont(regular))
                .setFontSize(11)
                .setMultipliedLeading(1.25f);
    }

    private Image loadLogo(MedicalCertificate certificate) throws Exception {
        if (certificate.getHospital() != null && !blank(certificate.getHospital().getLogoUrl())) {
            try {
                return new Image(ImageDataFactory.create(certificate.getHospital().getLogoUrl().trim()));
            } catch (Exception ignored) {
            }
        }
        URL bundledLogo = getClass().getClassLoader().getResource("images/swastik-logo.png");
        return bundledLogo != null ? new Image(ImageDataFactory.create(bundledLogo)) : null;
    }

    private Cell noBorderCell() {
        return new Cell().setBorder(Border.NO_BORDER);
    }

    private String format(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    private String labelize(String key) {
        if (key == null || key.isBlank()) return "";
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ').replace('-', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private String joinNonBlank(String separator, String... values) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String value : values) {
            if (!blank(value)) parts.add(value.trim());
        }
        return String.join(separator, parts);
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (!blank(value)) return value.trim();
        }
        return "";
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
