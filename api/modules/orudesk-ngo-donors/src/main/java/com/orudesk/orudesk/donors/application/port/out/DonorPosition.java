package com.orudesk.orudesk.donors.application.port.out;

import java.util.UUID;

/** Where a page ended: the sort key of its last donor. */
public record DonorPosition(String lastName, String firstName, UUID id) {
}
