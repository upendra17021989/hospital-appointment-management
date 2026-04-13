package com.hospital.service;

import com.hospital.model.Prescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sms.account.sid}")
    private String accountSid;

    @Value("${sms.auth.token}")
    private String authToken;

    @Value("${sms.from.number}")
    private String fromNumber;

    public void sendPrescriptionSms(String toPhone, Prescription prescription) {
        if (toPhone == null || toPhone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String formattedPhone = formatPhone(toPhone);
        String message = buildPrescriptionMessage(prescription);

        String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid);

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(accountSid, authToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("To", formattedPhone);
        formData.add("From", fromNumber);
        formData.add("Body", message);
        //formData.add("Body", "hi testing");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("SMS sent to {} status: {}, {}", formattedPhone, response.getStatusCodeValue(), accountSid );
            log.info("SMS response: {}", response.getBody());
        } catch (Exception e) {
            log.error("SMS send failed to {}: {}", formattedPhone, e.getMessage());
            throw e;
        }
    }

    private String formatPhone(String phone) {
        phone = phone.replaceAll("[^0-9+]", "");
        if (phone.startsWith("91") || phone.startsWith("+91")) {
            // good
        } else if (phone.length() == 10) {
            phone = "91" + phone;
        } else {
            throw new IllegalArgumentException("Invalid Indian phone number: " + phone);
        }
        return phone;  // SMS uses plain phone number, no 'whatsapp:' prefix
    }

    private String buildPrescriptionMessage(Prescription p) {
        StringBuilder msg = new StringBuilder();
        msg.append("📋 Your Prescription from MediCare+\n\n");
        msg.append("👤 Patient: ").append(p.getPatient().getFullName()).append("\n");
        msg.append("👨‍⚕️ Doctor: Dr. ").append(p.getDoctor().getFullName()).append("\n\n");
        msg.append("🔬 Diagnosis: ").append(p.getDiagnosis()).append("\n\n");
        msg.append("💊 Medicines:\n");
        if (p.getMedicines() != null && !p.getMedicines().isEmpty()) {
            p.getMedicines().stream()
                    .sorted((a, b) -> Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0, b.getSortOrder() != null ? b.getSortOrder() : 0))
                    .limit(10)
                    .forEach(m -> msg.append("• ")
                            .append(m.getMedicineName()).append(" - ")
                            .append(m.getDosage() != null ? m.getDosage() + " " : "")
                            .append(m.getFrequency()).append(" for ")
                            .append(m.getDuration()).append("\n"));
        } else {
            msg.append("(No medicines)\n");
        }
        msg.append("\n📄 Print full prescription from app for details.\n");
        msg.append("🏥 MediCare+ Hospital");
        return msg.toString();
    }
}

