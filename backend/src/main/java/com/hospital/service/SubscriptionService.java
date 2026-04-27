package com.hospital.service;

import com.hospital.dto.Dtos.SubscriptionUsageResponse;
import com.hospital.model.Hospital;
import com.hospital.model.HospitalSubscription;
import com.hospital.model.SubscriptionPlan;
import com.hospital.repository.AppointmentRepo;
import com.hospital.repository.DoctorRepo;
import com.hospital.repository.HospitalRepo;
import com.hospital.repository.HospitalSubscriptionRepo;
import com.hospital.repository.SubscriptionPlanRepo;
import com.hospital.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final HospitalSubscriptionRepo subscriptionRepo;
    private final SubscriptionPlanRepo planRepo;
    private final HospitalRepo hospitalRepo;
    private final DoctorRepo doctorRepo;
    private final UserRepo userRepo;
    private final AppointmentRepo appointmentRepo;

    @Value("${app.subscription.trial-days:14}")
    private int trialDays;

    public List<SubscriptionPlan> getAllActivePlans() {
        return planRepo.findByIsActiveTrueOrderByMonthlyPriceAsc();
    }

    public Optional<SubscriptionPlan> getPlanBySlug(String slug) {
        return planRepo.findBySlug(slug);
    }

    @Transactional
    public HospitalSubscription createTrialSubscription(Hospital hospital) {
        SubscriptionPlan freePlan = planRepo.findBySlug("free")
                .orElseThrow(() -> new RuntimeException("Default plan not found"));

        LocalDateTime now = LocalDateTime.now();
        HospitalSubscription subscription = HospitalSubscription.builder()
                .hospital(hospital)
                .plan(freePlan)
                .status(HospitalSubscription.Status.trial)
                .trialEndsAt(now.plusDays(trialDays))
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plusDays(trialDays))
                .build();

        return subscriptionRepo.save(subscription);
    }

    @Transactional
    public HospitalSubscription createOrUpdateSubscription(Hospital hospital, SubscriptionPlan plan,
                                                           HospitalSubscription.Status status,
                                                           HospitalSubscription.BillingCycle billingCycle) {
        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospital.getId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (sub == null) {
            sub = HospitalSubscription.builder()
                    .hospital(hospital)
                    .plan(plan)
                    .status(status)
                    .billingCycle(billingCycle)
                    .currentPeriodStart(now)
                    .currentPeriodEnd(now.plusMonths(billingCycle == HospitalSubscription.BillingCycle.yearly ? 12 : 1))
                    .build();
        } else {
            sub.setPlan(plan);
            sub.setStatus(status);
            sub.setBillingCycle(billingCycle);
            sub.setCurrentPeriodStart(now);
            sub.setCurrentPeriodEnd(now.plusMonths(billingCycle == HospitalSubscription.BillingCycle.yearly ? 12 : 1));
            sub.setTrialEndsAt(null);
            sub.setCancelledAt(null);
        }

        return subscriptionRepo.save(sub);
    }

    public SubscriptionUsageResponse getUsage(UUID hospitalId) {
        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId).orElse(null);
        SubscriptionPlan plan = sub != null && sub.getPlan() != null ? sub.getPlan() : null;

        int doctorsUsed = doctorRepo.findByHospitalIdAndIsAvailableTrue(hospitalId).size();
        int usersUsed = userRepo.findByHospitalId(hospitalId).size();

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        long appointmentsThisMonth = appointmentRepo.countByHospitalIdAndAppointmentDateBetween(hospitalId, startOfMonth, endOfMonth);

        return SubscriptionUsageResponse.builder()
                .doctorsUsed(doctorsUsed)
                .doctorsLimit(plan != null ? plan.getMaxDoctors() : 3)
                .usersUsed(usersUsed)
                .usersLimit(plan != null ? plan.getMaxUsers() : 2)
                .appointmentsThisMonth(appointmentsThisMonth)
                .appointmentsLimit(plan != null ? plan.getMaxAppointmentsPerMonth() : 100)
                .prescriptionsEnabled(plan != null && Boolean.TRUE.equals(plan.getAllowPrescriptions()))
                .build();
    }

    public Optional<HospitalSubscription> getSubscriptionByHospitalId(UUID hospitalId) {
        return subscriptionRepo.findByHospitalId(hospitalId);
    }

    @Transactional
    public void cancelSubscription(UUID hospitalId) {
        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId)
                .orElseThrow(() -> new RuntimeException("No subscription found"));
        sub.setStatus(HospitalSubscription.Status.cancelled);
        sub.setCancelledAt(LocalDateTime.now());
        subscriptionRepo.save(sub);
    }

    @Transactional
    public void downgradeToFreePlan(UUID hospitalId) {
        SubscriptionPlan freePlan = planRepo.findBySlug("free")
                .orElseThrow(() -> new RuntimeException("Free plan not found"));

        HospitalSubscription sub = subscriptionRepo.findByHospitalId(hospitalId).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (sub == null) {
            Hospital hospital = hospitalRepo.findById(hospitalId)
                    .orElseThrow(() -> new RuntimeException("Hospital not found"));
            sub = HospitalSubscription.builder()
                    .hospital(hospital)
                    .plan(freePlan)
                    .status(HospitalSubscription.Status.active)
                    .currentPeriodStart(now)
                    .currentPeriodEnd(now.plusMonths(1))
                    .build();
        } else {
            sub.setPlan(freePlan);
            sub.setStatus(HospitalSubscription.Status.active);
            sub.setBillingCycle(HospitalSubscription.BillingCycle.monthly);
            sub.setTrialEndsAt(null);
            sub.setCancelledAt(null);
            sub.setCurrentPeriodStart(now);
            sub.setCurrentPeriodEnd(now.plusMonths(1));
        }

        subscriptionRepo.save(sub);
    }
}

