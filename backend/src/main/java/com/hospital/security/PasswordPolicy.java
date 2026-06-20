package com.hospital.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class PasswordPolicy {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        List<String> issues = new ArrayList<>();
        if (password == null || password.length() < 12) {
            issues.add("at least 12 characters");
        }
        if (password == null || !UPPERCASE.matcher(password).find()) {
            issues.add("one uppercase letter");
        }
        if (password == null || !LOWERCASE.matcher(password).find()) {
            issues.add("one lowercase letter");
        }
        if (password == null || !DIGIT.matcher(password).find()) {
            issues.add("one number");
        }
        if (password == null || !SPECIAL.matcher(password).find()) {
            issues.add("one special character");
        }

        if (!issues.isEmpty()) {
            throw new IllegalArgumentException("Password must contain " + String.join(", ", issues) + ".");
        }
    }
}
