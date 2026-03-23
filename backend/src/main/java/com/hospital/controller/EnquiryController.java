package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.dto.Dtos.EnquiryRequest;
import com.hospital.dto.Dtos.EnquiryResponse;
import com.hospital.model.Enquiry;
import com.hospital.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/enquiries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Enquiries", description = "Hospital enquiry management APIs")
public class EnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping
    @Operation(summary = "Submit a new enquiry")
    public ResponseEntity<ApiResponse<EnquiryResponse>> createEnquiry(
            @Valid @RequestBody EnquiryRequest request) {
        EnquiryResponse enquiry = enquiryService.submitEnquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enquiry submitted successfully", enquiry));
    }

    @GetMapping
    @Operation(summary = "Get enquiries with optional status filter")
    public ResponseEntity<ApiResponse<List<EnquiryResponse>>> getEnquiries(
            @RequestParam(required = false) String status) {
        List<EnquiryResponse> enquiries;
        if (status != null) {
            enquiries = enquiryService.getEnquiriesByStatus(Enquiry.Status.valueOf(status));
        } else {
            enquiries = enquiryService.getActiveEnquiries();
        }
        return ResponseEntity.ok(ApiResponse.success(enquiries));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all enquiries")
    public ResponseEntity<ApiResponse<List<EnquiryResponse>>> getAllEnquiries() {
        return ResponseEntity.ok(ApiResponse.success(enquiryService.getAllEnquiries()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get enquiry by ID")
    public ResponseEntity<ApiResponse<EnquiryResponse>> getEnquiry(@PathVariable UUID id) {
        return enquiryService.getEnquiryById(id)
                .map(e -> ResponseEntity.ok(ApiResponse.success(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update enquiry status")
    public ResponseEntity<ApiResponse<EnquiryResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) String response) {
        try {
            EnquiryResponse updated = enquiryService.updateEnquiryStatus(
                    id, Enquiry.Status.valueOf(status), response);
            return ResponseEntity.ok(ApiResponse.success("Enquiry status updated", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign enquiry to staff")
    public ResponseEntity<ApiResponse<EnquiryResponse>> assignEnquiry(
            @PathVariable UUID id,
            @RequestParam String assignedTo) {
        try {
            EnquiryResponse updated = enquiryService.assignEnquiry(id, assignedTo);
            return ResponseEntity.ok(ApiResponse.success("Enquiry assigned", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
