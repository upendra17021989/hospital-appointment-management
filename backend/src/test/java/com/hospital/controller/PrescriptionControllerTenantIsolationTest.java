package com.hospital.controller;

import com.hospital.model.Prescription;
import com.hospital.repository.AppointmentRepo;
import com.hospital.repository.CommonMedicineRepo;
import com.hospital.repository.CommonTestRepo;
import com.hospital.repository.DoctorRepo;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.PatientRepo;
import com.hospital.repository.PrescriptionRepo;
import com.hospital.security.TenantContext;
import com.hospital.service.EmailService;
import com.hospital.service.PrescriptionPdfService;
import com.hospital.service.SmsService;
import com.hospital.service.WhatsAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionControllerTenantIsolationTest {

    @Mock private PrescriptionRepo prescriptionRepo;
    @Mock private PatientRepo patientRepo;
    @Mock private DoctorRepo doctorRepo;
    @Mock private AppointmentRepo appointmentRepo;
    @Mock private HospitalRepo hospitalRepo;
    @Mock private CommonMedicineRepo commonMedicineRepo;
    @Mock private CommonTestRepo commonTestRepo;
    @Mock private TenantContext tenantContext;
    @Mock private PrescriptionPdfService pdfService;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;
    @Mock private WhatsAppService whatsappService;

    @InjectMocks private PrescriptionController controller;

    @Test
    void deleteDoesNotRemovePrescriptionOutsideCurrentHospital() {
        UUID hospitalId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        when(tenantContext.requireHospitalId()).thenReturn(hospitalId);
        when(prescriptionRepo.findByHospitalOrDoctorOrPatientHospitalIdAndId(hospitalId, prescriptionId))
                .thenReturn(Optional.empty());

        var response = controller.delete(prescriptionId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(prescriptionRepo, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteRemovesPrescriptionResolvedWithinCurrentHospital() {
        UUID hospitalId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        Prescription prescription = Prescription.builder().id(prescriptionId).build();
        when(tenantContext.requireHospitalId()).thenReturn(hospitalId);
        when(prescriptionRepo.findByHospitalOrDoctorOrPatientHospitalIdAndId(hospitalId, prescriptionId))
                .thenReturn(Optional.of(prescription));

        var response = controller.delete(prescriptionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(prescriptionRepo).delete(prescription);
    }
}
