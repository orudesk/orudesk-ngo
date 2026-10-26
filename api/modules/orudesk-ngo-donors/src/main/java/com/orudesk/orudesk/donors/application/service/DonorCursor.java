package com.orudesk.orudesk.donors.application.service;

import com.orudesk.orudesk.donors.application.port.in.DonorListItem;
import com.orudesk.orudesk.donors.application.port.out.DonorPosition;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** The same opaque cursor as the People registry's PersonCursor (Day 16), for donors. */
final class DonorCursor {

    private static final String SEPARATOR = "\u0000";

    private DonorCursor() {
    }

    static String encode(DonorListItem last) {
        String raw = last.lastName() + SEPARATOR + last.firstName() + SEPARATOR + last.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static DonorPosition decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(SEPARATOR, -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("invalid cursor");
            }
            return new DonorPosition(parts[0], parts[1], UUID.fromString(parts[2]));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid cursor", e);
        }
    }
}
