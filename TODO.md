# Subscription & Monetization Implementation Tracker

## Phase 1: Backend Core Fixes
- [x] 1.1 Create TODO.md tracker
- [x] 1.2 Complete `SubscriptionService.java` — finish `createTrialSubscription()` + implement `getUsage()`
- [x] 1.3 Create `PaymentController.java` — webhook + billing history endpoints
- [x] 1.4 Update `SubscriptionPlanRepo.java` — add `findByIsActiveTrueOrderByMonthlyPriceAsc()`
- [x] 1.5 Update `SecurityConfig.java` — expose `/subscriptions/plans` as public
- [x] 1.6 Update `AppointmentRepo.java` — add `countByHospitalIdAndAppointmentDateBetween()` for usage stats

## Phase 2: Feature Gating
- [x] 2.1 Add `@RequireSubscription` to `DoctorManagementController` (create/update/delete/schedule endpoints)
- [x] 2.2 Add `@RequireSubscription` to `UserController` (create endpoint)
- [x] 2.3 Add `@RequireSubscription` to `AppointmentController` (hospital endpoints)
- [x] 2.4 Add `@RequireSubscription(feature = "prescriptions")` to `PrescriptionController`

## Phase 3: Frontend Core Wiring
- [x] 3.1 Update `AuthContext.jsx` — store subscription state from auth response
- [x] 3.2 Update `api.js` — add subscription/payment API helpers

## Phase 4: Frontend Pages & Components
- [x] 4.1 Create `SubscriptionPlans.jsx` page
- [x] 4.2 Create `BillingHistory.jsx` page
- [x] 4.3 Create `PaymentSuccess.jsx` page
- [x] 4.4 Create `PaymentCancel.jsx` page
- [x] 4.5 Create `SubscriptionBanner.jsx` component
- [x] 4.6 Create `SubscriptionGuard.jsx` component
- [x] 4.7 Update `App.jsx` — add new routes
- [x] 4.8 Update `Sidebar.jsx` — add billing menu

## Phase 5: Styles
- [x] 5.1 Update `_pages.scss` — add subscription page styles

## Phase 6: Verification
- [x] 6.1 Verify backend compiles
- [x] 6.2 Verify frontend builds
