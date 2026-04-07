package com.hospital.security;

import com.hospital.model.User;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class HospitalContextAspect {

    @Before("@annotation(RequireHospitalContext)")
    public void validateHospitalContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            // For now, we'll allow access if user has a hospital
            // In a full implementation, you'd validate against the request parameters
            if (user.getHospital() == null) {
                throw new SecurityException("User must be associated with a hospital");
            }
        }
    }
}