package com.orudesk.orudesk.donors.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An Indian Permanent Account Number, such as ABCDE1234F: five letters, four digits, one letter.
 * An NGO needs it to issue 80G tax receipts. It identifies a taxpayer, so it's shown masked.
 */
public record Pan(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    public Pan {
        Objects.requireNonNull(value, "PAN must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("PAN must look like ABCDE1234F");
        }
    }

    /** XXXXXX234F: enough for a person to recognise their own, not enough to misuse. */
    public String masked() {
        return "XXXXXX" + value.substring(6);
    }
}
