package com.example.carrentingtest.utils;

import java.util.regex.Pattern;

public class MoroccanLicenseValidator {

    // Example pattern: one or two letters followed by five or six digits.
    private static final Pattern LICENSE_PATTERN = Pattern.compile("^[A-Z]{1,2}\\d{5,6}$", Pattern.CASE_INSENSITIVE);

    /**
     * Validates the provided driving licence number.
     *
     * @param license the licence number to validate
     * @return true if the licence matches the Moroccan format
     */
    public static boolean isValid(String license) {
        return license != null && LICENSE_PATTERN.matcher(license).matches();
    }
}