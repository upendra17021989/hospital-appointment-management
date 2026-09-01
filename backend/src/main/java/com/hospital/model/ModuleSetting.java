package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "module_settings", uniqueConstraints = @UniqueConstraint(
        name = "uk_module_setting_scope_module", columnNames = {"scope_key", "module_key"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ModuleSetting {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "scope_key", nullable = false, length = 50)
    private String scopeKey;
    @Column(name = "hospital_id")
    private UUID hospitalId;
    @Column(name = "module_key", nullable = false, length = 50)
    private String moduleKey;
    @Column(nullable = false)
    private Boolean enabled;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
