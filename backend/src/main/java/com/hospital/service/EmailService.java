package com.hospital.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPrescriptionEmail(String toEmail, byte[] pdf) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Your Prescription");
        helper.setText("Please find your prescription attached.");

        helper.addAttachment(
                "prescription.pdf",
                new ByteArrayResource(pdf)
        );

        mailSender.send(message);
    }
}