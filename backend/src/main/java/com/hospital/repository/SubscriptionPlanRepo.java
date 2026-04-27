package com.hospital.repository;

import com.hospital.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepo extends JpaRepository<SubscriptionPlan, UUID> {
    Optional<SubscriptionPlan> findBySlug(String slug);
    Optional<SubscriptionPlan> findByName(String name);
    List<SubscriptionPlan> findByIsActiveTrueOrderByMonthlyPriceAsc();
}

