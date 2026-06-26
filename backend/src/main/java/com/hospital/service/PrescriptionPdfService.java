package com.hospital.service;

import com.hospital.model.Prescription;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PrescriptionPdfService {

    private static final String MEDICOLEGAL_DISCLAIMER =
            "This Prescription / Certificate is not for medicolegal purpose.";

    public byte[] generatePdf(Prescription prescription) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Hospital Prescription"));
        document.add(new Paragraph("Patient: " + prescription.getPatient().getFullName()));
        document.add(new Paragraph("Doctor: " + prescription.getDoctor().getFullName()));
        document.add(new Paragraph("Diagnosis: " + prescription.getDiagnosis()));

        document.add(new Paragraph("Medicines:"));

        prescription.getMedicines().forEach(m -> {
            document.add(new Paragraph(
                    m.getMedicineName() + " - " +
                    m.getDosage() + " - " +
                    m.getFrequency()));
        });

        document.add(new Paragraph(MEDICOLEGAL_DISCLAIMER));

        document.close();

        return out.toByteArray();
    }
}
