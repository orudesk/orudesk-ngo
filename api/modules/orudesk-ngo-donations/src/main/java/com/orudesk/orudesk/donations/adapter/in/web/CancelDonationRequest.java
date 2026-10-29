package com.orudesk.orudesk.donations.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The JSON body of POST /api/v1/donations/{id}/cancel. */
record CancelDonationRequest(@NotNull Long version, @NotBlank @Size(max = 200) String reason) {
}
