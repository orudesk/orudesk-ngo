package com.orudesk.orudesk.donations.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount of Indian rupees, exact to the paisa (Day 56). Never a double: 0.1 + 0.2 is 0.30000000000000004 in
 * binary floating point, and an NGO's books must add up to the paisa. BigDecimal holds decimal digits exactly.
 *
 * Always positive, at most two decimals (no fractions of a paisa) and below ten thousand crore (12 digits before
 * the point, the column's NUMERIC(14, 2)).
 */
public record Amount(BigDecimal value) implements Comparable<Amount> {

    private static final BigDecimal MAX = new BigDecimal("1000000000000");   // 10^12 rupees: one lakh crore

    public Amount {
        Objects.requireNonNull(value, "amount must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("an amount must be more than zero");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("an amount has at most two decimals (paise)");
        }
        if (value.compareTo(MAX) >= 0) {
            throw new IllegalArgumentException("an amount must be less than 1,00,000 crore");
        }
        value = value.setScale(2, RoundingMode.UNNECESSARY);   // 1500 and 1500.0 are the same amount: 1500.00
    }

    /** From text such as "1500" or "1500.50", the way the API receives it. */
    public static Amount of(String text) {
        try {
            return new Amount(new BigDecimal(text.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not an amount: " + text);
        }
    }

    public static Amount rupees(long rupees) {
        return new Amount(BigDecimal.valueOf(rupees));
    }

    public boolean isAtLeast(Amount other) {
        return value.compareTo(other.value) >= 0;
    }

    @Override
    public int compareTo(Amount other) {
        return value.compareTo(other.value);
    }

    /** "1500.00": what the API sends, as a string, so no JSON parser turns it into a floating-point number. */
    public String plain() {
        return value.toPlainString();
    }

    /** "₹1,50,000.00": Indian digit grouping, three digits and then twos (lakh, crore). For people, not machines. */
    public String formatted() {
        String plain = plain();
        int point = plain.indexOf('.');
        String rupees = plain.substring(0, point);
        StringBuilder grouped = new StringBuilder();
        int length = rupees.length();
        for (int i = 0; i < length; i++) {
            int fromEnd = length - i;
            grouped.append(rupees.charAt(i));
            if (fromEnd > 1 && (fromEnd == 4 || (fromEnd > 4 && fromEnd % 2 == 0))) {
                grouped.append(',');
            }
        }
        return "₹" + grouped + plain.substring(point);
    }

    @Override
    public String toString() {
        return formatted();
    }
}
