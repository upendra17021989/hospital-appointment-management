package com.hospital.service;

import com.hospital.model.ModuleSetting;
import com.hospital.repository.ModuleSettingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ModuleSettingService {
    public static final String GLOBAL_SCOPE = "GLOBAL";
    public static final List<String> MODULES = List.of("APPOINTMENTS", "PATIENTS", "CLINICAL",
            "CONSULTATION_BILLING", "REPORTS", "USER_MANAGEMENT", "BILLING_PLANS");
    private final ModuleSettingRepo repo;

    public Map<String, Boolean> effectiveSettings(UUID hospitalId) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        MODULES.forEach(key -> result.put(key, true));
        repo.findByScopeKey(GLOBAL_SCOPE).forEach(s -> result.put(s.getModuleKey(), s.getEnabled()));
        if (hospitalId != null) repo.findByScopeKey(hospitalId.toString())
                .forEach(s -> result.put(s.getModuleKey(), s.getEnabled()));
        return result;
    }

    public Map<String, Boolean> globalSettings() { return effectiveSettings(null); }

    public Map<String, Boolean> overrides(UUID hospitalId) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        repo.findByScopeKey(hospitalId.toString()).forEach(s -> result.put(s.getModuleKey(), s.getEnabled()));
        return result;
    }

    public boolean isEnabled(UUID hospitalId, String moduleKey) {
        validate(moduleKey);
        return effectiveSettings(hospitalId).get(moduleKey);
    }

    @Transactional public void setGlobal(String key, boolean enabled) { save(GLOBAL_SCOPE, null, key, enabled); }
    @Transactional public void setHospital(UUID id, String key, boolean enabled) { save(id.toString(), id, key, enabled); }

    @Transactional
    public void clearHospital(UUID id, String key) {
        validate(key);
        repo.deleteByScopeKeyAndModuleKey(id.toString(), key);
    }

    private void save(String scope, UUID hospitalId, String key, boolean enabled) {
        validate(key);
        ModuleSetting setting = repo.findByScopeKeyAndModuleKey(scope, key).orElseGet(ModuleSetting::new);
        setting.setScopeKey(scope);
        setting.setHospitalId(hospitalId);
        setting.setModuleKey(key);
        setting.setEnabled(enabled);
        setting.setUpdatedAt(LocalDateTime.now());
        repo.save(setting);
    }

    private void validate(String key) {
        if (!MODULES.contains(key)) throw new IllegalArgumentException("Unknown module: " + key);
    }
}
