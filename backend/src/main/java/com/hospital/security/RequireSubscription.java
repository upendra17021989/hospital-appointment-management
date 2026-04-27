package com.hospital.security;

import java.lang.annotation.*;

/**
 * Annotation to enforce subscription requirements on controller methods.
 * Usage: @RequireSubscription(feature = "prescriptions") or just @RequireSubscription for general active sub check.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSubscription {

    /**
     * Feature to check. Leave empty for general active subscription check.
     * Options: "prescriptions", "sms", "whatsapp", "custom_branding"
     */
    String feature() default "";
}
