package com.hospital.controller;

import com.hospital.dto.ConsultationPaymentDtos;
import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.security.TenantContext;
import com.hospital.service.ConsultationPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultation-payments")
@RequiredArgsConstructor
@Tag(name = "Consultation Payments", description = "Create consultation payments and enable receipt printing")
public class ConsultationPaymentController {

    private final ConsultationPaymentService paymentService;
    private final TenantContext tenantContext;

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','RECEPTIONIST','DOCTOR','HOSPITAL_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create consultation payment")
    public ResponseEntity<ApiResponse<ConsultationPaymentDtos.CreateConsultationPaymentResponse>> createPayment(
            @Valid @RequestBody ConsultationPaymentDtos.CreateConsultationPaymentRequest req) {

        // Touch tenant context to ensure auth has hospital scope
        tenantContext.requireHospitalId();

        var resp = paymentService.createPayment(req);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}

