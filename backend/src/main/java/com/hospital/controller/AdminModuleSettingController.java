package com.hospital.controller;

import com.hospital.dto.Dtos.ApiResponse;
import com.hospital.model.*;
import com.hospital.repository.HospitalRepo;
import com.hospital.service.ModuleSettingService;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class AdminModuleSettingController {
    private final ModuleSettingService service;
    private final HospitalRepo hospitalRepo;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ToggleRequest { private Boolean enabled; }

    @Data @Builder
    public static class HospitalModules {
        private UUID id;
        private String name;
        private String city;
        private Map<String, Boolean> overrides;
        private Map<String, Boolean> effective;
    }

    @GetMapping("/module-settings/me")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> mine(@AuthenticationPrincipal User user) {
        UUID hospitalId = user != null && user.getHospital() != null ? user.getHospital().getId() : null;
        return ResponseEntity.ok(ApiResponse.success(service.effectiveSettings(hospitalId)));
    }

    @GetMapping("/admin/module-settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list() {
        List<HospitalModules> hospitals = hospitalRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toHospital).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("modules", ModuleSettingService.MODULES);
        data.put("global", service.globalSettings());
        data.put("hospitals", hospitals);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/admin/module-settings/global/{moduleKey}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> setGlobal(@PathVariable String moduleKey,
                                                          @RequestBody ToggleRequest request) {
        requireEnabled(request);
        service.setGlobal(moduleKey, request.getEnabled());
        return ResponseEntity.ok(ApiResponse.success("Global module setting updated", moduleKey));
    }

    @PutMapping("/admin/module-settings/hospitals/{hospitalId}/{moduleKey}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> setHospital(@PathVariable UUID hospitalId,
                                                            @PathVariable String moduleKey,
                                                            @RequestBody ToggleRequest request) {
        if (!hospitalRepo.existsById(hospitalId)) throw new IllegalArgumentException("Hospital not found");
        requireEnabled(request);
        service.setHospital(hospitalId, moduleKey, request.getEnabled());
        return ResponseEntity.ok(ApiResponse.success("Hospital module override updated", moduleKey));
    }

    @DeleteMapping("/admin/module-settings/hospitals/{hospitalId}/{moduleKey}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> clearHospital(@PathVariable UUID hospitalId,
                                                              @PathVariable String moduleKey) {
        service.clearHospital(hospitalId, moduleKey);
        return ResponseEntity.ok(ApiResponse.success("Hospital now inherits the global setting", moduleKey));
    }

    private HospitalModules toHospital(Hospital h) {
        return HospitalModules.builder().id(h.getId()).name(h.getName()).city(h.getCity())
                .overrides(service.overrides(h.getId())).effective(service.effectiveSettings(h.getId())).build();
    }

    private void requireEnabled(ToggleRequest request) {
        if (request.getEnabled() == null) throw new IllegalArgumentException("enabled is required");
    }
}
