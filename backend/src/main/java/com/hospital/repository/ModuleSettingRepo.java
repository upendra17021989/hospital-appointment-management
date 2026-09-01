package com.hospital.repository;

import com.hospital.model.ModuleSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ModuleSettingRepo extends JpaRepository<ModuleSetting, UUID> {
    Optional<ModuleSetting> findByScopeKeyAndModuleKey(String scopeKey, String moduleKey);
    List<ModuleSetting> findByScopeKey(String scopeKey);
    void deleteByScopeKeyAndModuleKey(String scopeKey, String moduleKey);
}
