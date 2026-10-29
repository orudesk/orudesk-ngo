package com.orudesk.orudesk.donations.domain;

import java.math.BigDecimal;

/**
 * "Rupees One Lakh Fifty Thousand and Fifty Paise Only" (Day 57). Indian receipts and cheques write the amount
 * in words too: figures are easy to alter (₹1,000 becomes ₹7,000 with one stroke), words much less so. Indian
 * numbering: thousand, lakh (1,00,000), crore (1,00,00,000).
 */
public final class RupeesInWords {

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty",
            "Ninety"};

    private RupeesInWords() {
    }

    public static String of(Amount amount) {
        BigDecimal value = amount.value();
        long rupees = value.longValue();
        int paise = value.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        StringBuilder words = new StringBuilder("Rupees ");
        words.append(rupees == 0 ? "Zero" : number(rupees));
        if (paise > 0) {
            words.append(" and ").append(belowHundred(paise)).append(" Paise");
        }
        return words.append(" Only").toString();
    }

    /** Crores can be more than 99: "One Thousand Two Hundred Crore" is how Indian usage says 1,200 crore. */
    private static String number(long n) {
        StringBuilder words = new StringBuilder();
        long crores = n / 1_00_00_000;
        n %= 1_00_00_000;
        if (crores > 0) {
            words.append(number(crores)).append(" Crore ");
        }
        n = part(words, n, 1_00_000, "Lakh");
        n = part(words, n, 1_000, "Thousand");
        n = part(words, n, 100, "Hundred");
        if (n > 0) {
            words.append(belowHundred((int) n));
        }
        return words.toString().strip();
    }

    private static long part(StringBuilder words, long n, int unit, String name) {
        if (n >= unit) {
            words.append(belowHundred((int) (n / unit))).append(' ').append(name).append(' ');
        }
        return n % unit;
    }

    private static String belowHundred(int n) {
        if (n < 20) {
            return ONES[n];
        }
        return n % 10 == 0 ? TENS[n / 10] : TENS[n / 10] + " " + ONES[n % 10];
    }
}
