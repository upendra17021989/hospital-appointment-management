package com.hospital.repository;

import com.hospital.model.LoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoginActivityRepo extends JpaRepository<LoginActivity, UUID> {
}
