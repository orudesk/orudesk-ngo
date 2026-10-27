package com.orudesk.orudesk.donors.application.port.in;

import com.orudesk.orudesk.donors.domain.DonorType;

/**
 * What to look for. The same rules as the People registry's search (Day 16).
 *
 * @param text  part of a name, phone or email; null lists every donor
 * @param type  only donors of this type; null for all
 * @param after the nextCursor of the previous page, or null for the first page
 * @param size  1 to {@link SearchDonorsQuery#MAX_PAGE_SIZE}
 */
public record DonorSearch(String text, DonorType type, String after, int size) {

    public DonorSearch {
        if (size < 1 || size > SearchDonorsQuery.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + SearchDonorsQuery.MAX_PAGE_SIZE);
        }
        text = text == null || text.isBlank() ? null : text.trim();
        if (text != null && text.length() < 3) {
            throw new IllegalArgumentException("search text must be at least 3 characters");
        }
    }
}
