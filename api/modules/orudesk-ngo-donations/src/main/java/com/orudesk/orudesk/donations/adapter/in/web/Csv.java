package com.orudesk.orudesk.donations.adapter.in.web;

/** Writing one CSV field safely (RFC 4180, plus spreadsheet formula injection). */
final class Csv {

    private Csv() {
    }

    /**
     * A donor called "=HYPERLINK(...)" must stay text when an accountant opens the file in Excel: a cell that starts
     * with = + - @ (or a tab or carriage return) is a formula there. A leading apostrophe keeps it text (OWASP's
     * advice). Then quotes: a field with a comma, quote or line break is wrapped in quotes, and its quotes doubled.
     */
    static String field(String value) {
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
