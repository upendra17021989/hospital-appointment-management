# TODO: Subscription & Monetization Implementation

## Phase 1: Database & Backend Core

### Database
- [ ] 1. Update `database/schema.sql` - Add subscription_plans, hospital_subscriptions, payments tables
- [ ] 2. Update `database/seed_dummy_data.sql` - Seed subscription plans

### Backend Entities
- [ ] 3. Create `SubscriptionPlan.java` entity
- [ ] 4. Create `HospitalSubscription.java` entity
- [ ] 5. Create `Payment.java` entity
- [ ] 6. Update `Hospital.java` - Add subscription fields

### Backend Repositories
- [ ] 7. Create `SubscriptionPlanRepo.java`
- [ ] 8. Create `HospitalSubscriptionRepo.java`
- [ ] 9. Create `PaymentRepo.java`

### Backend Services
- [ ] 10. Create `SubscriptionService.java`
- [ ] 11. Create `PaymentService.java` (Stripe integration)

### Backend Controllers
- [ ] 12. Create `SubscriptionController.java`
- [ ] 13. Create `PaymentController.java`
- [ ] 14. Update `AuthController.java` - Include subscription in auth response

### Backend DTOs & Security
- [ ] 15. Update `Dtos.java` - Add subscription/payment DTOs
- [ ] 16. Create `SubscriptionGuard.java` annotation + aspect
- [ ] 17. Update `SecurityConfig.java` - Expose public plans endpoint
- [ ] 18. Update `JwtService.java` - Include subscription status in token claims

### Build & Config
- [ ] 19. Update `backend/pom.xml` - Add Stripe SDK dependency
- [ ] 20. Update `application.properties.example` - Add Stripe keys

## Phase 2: Feature Gating

- [ ] 21. Add subscription checks to `DoctorController`
- [ ] 22. Add subscription checks to `UserController`
- [ ] 23. Add subscription checks to `AppointmentController`
- [ ] 24. Add subscription checks to `PrescriptionController`

## Phase 3: Frontend

- [ ] 25. Update `AuthContext.jsx` - Store subscription state
- [ ] 26. Create `SubscriptionGuard.jsx` component
- [ ] 27. Create `SubscriptionBanner.jsx` component
- [ ] 28. Create `SubscriptionPlans.jsx` page
- [ ] 29. Create `BillingHistory.jsx` page
- [ ] 30. Create `PaymentSuccess.jsx` page
- [ ] 31. Create `PaymentCancel.jsx` page
- [ ] 32. Update `App.jsx` - Add new routes
- [ ] 33. Update `Sidebar.jsx` - Add billing menu
- [ ] 34. Update `api.js` - Add subscription endpoints

## Phase 4: Testing & Polish
- [ ] 35. Test signup → trial → upgrade flow
- [ ] 36. Verify subscription enforcement across endpoints
- [ ] 37. Verify frontend guards and banners

