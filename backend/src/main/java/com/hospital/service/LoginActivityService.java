package com.hospital.service;

import com.hospital.model.LoginActivity;
import com.hospital.model.User;
import com.hospital.repository.LoginActivityRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginActivityService {

    private final LoginActivityRepo loginActivityRepo;

    public void record(String email, User user, boolean successful, String failureReason, String ipAddress, String userAgent) {
        loginActivityRepo.save(LoginActivity.builder()
                .email(email != null ? email : "unknown")
                .user(user)
                .hospital(user != null ? user.getHospital() : null)
                .successful(successful)
                .failureReason(failureReason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .loggedAt(LocalDateTime.now())
                .build());
    }
}
