package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "common_tests")
public class CommonTest {
    
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @GeneratedValue
    private UUID id;
    
    @Column(name = "hospital_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID hospitalId;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

