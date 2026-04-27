package com.hospital.repository;

import com.hospital.model.HospitalSubscription;
import com.hospital.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalSubscriptionRepo extends JpaRepository<HospitalSubscription, UUID> {
    Optional<HospitalSubscription> findByHospitalId(UUID hospitalId);
    Optional<HospitalSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<HospitalSubscription> findByStripeCustomerId(String stripeCustomerId);
}

