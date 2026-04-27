package com.hospital.security;

import com.hospital.model.HospitalSubscription;
import com.hospital.model.User;
import com.hospital.repository.HospitalSubscriptionRepo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionGuardAspect {

    private final HospitalSubscriptionRepo subscriptionRepo;

    @Around("@annotation(requireSubscription)")
    public Object enforceSubscription(ProceedingJoinPoint joinPoint, RequireSubscription requireSubscription) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return joinPoint.proceed();
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof User)) {
            return joinPoint.proceed();
        }

        User user = (User) principal;

        // SUPER_ADMIN bypasses subscription checks
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            return joinPoint.proceed();
        }

        UUID hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        if (hospitalId == null) {
            return reject("No hospital associated with account");
        }

        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId).orElse(null);

        // Auto-create trial if missing
        if (sub == null) {
            log.warn("No subscription found for hospital {}, allowing for now (trial should be auto-created)", hospitalId);
            return joinPoint.proceed();
        }

        // Check if expired
        if (sub.isExpired()) {
            // Allow free plan features even if expired
            if (sub.getPlan() != null && "free".equals(sub.getPlan().getSlug())) {
                return checkFeature(sub, requireSubscription.feature(), joinPoint);
            }
            return reject("Your subscription has expired. Please renew to continue.");
        }

        // Check if cancelled
        if (sub.getStatus() == HospitalSubscription.Status.cancelled) {
            // Allow until period end
            if (sub.getCurrentPeriodEnd() != null && java.time.LocalDateTime.now().isBefore(sub.getCurrentPeriodEnd())) {
                return checkFeature(sub, requireSubscription.feature(), joinPoint);
            }
            return reject("Your subscription has been cancelled. Please renew to continue.");
        }

        // Check active/trial
        if (!sub.isActiveOrTrial()) {
            return reject("Your subscription is not active. Please subscribe to continue.");
        }

        return checkFeature(sub, requireSubscription.feature(), joinPoint);
    }

    private Object checkFeature(HospitalSubscription sub, String feature, ProceedingJoinPoint joinPoint) throws Throwable {
        if (feature == null || feature.isBlank()) {
            return joinPoint.proceed();
        }

        var plan = sub.getPlan();
        if (plan == null) {
            return reject("No subscription plan found");
        }

        boolean allowed = switch (feature.toLowerCase()) {
            case "prescriptions" -> plan.getAllowPrescriptions();
            case "sms" -> plan.getAllowSms();
            case "whatsapp" -> plan.getAllowWhatsapp();
            case "custom_branding" -> plan.getAllowCustomBranding();
            case "priority_support" -> plan.getPrioritySupport();
            default -> true;
        };

        if (!allowed) {
            return reject("This feature requires a higher subscription plan: " + feature);
        }

        return joinPoint.proceed();
    }

    private Object reject(String message) throws IOException {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletResponse response = attrs.getResponse();
            if (response != null) {
                response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"" + message + "\"}"
                );
                return null;
            }
        }
        throw new RuntimeException(message);
    }
}
