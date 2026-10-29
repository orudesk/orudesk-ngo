package com.orudesk.orudesk.donations.domain;

import java.time.LocalDate;
import java.util.Objects;

/** Why and when a donation was cancelled (Day 58): a bounced cheque, a payment refunded, an entry made twice. */
public record Cancellation(LocalDate on, String reason) {

    public Cancellation {
        Objects.requireNonNull(on);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("say why the donation is cancelled");
        }
        reason = reason.strip();
        if (reason.length() > 200) {
            throw new IllegalArgumentException("the reason has at most 200 characters");
        }
    }
}
