package com.orudesk.orudesk.donors.application.port.in;

import com.orudesk.orudesk.donors.domain.DonorType;

import java.util.UUID;

/** One line of the donor list: what a screen needs to show, nothing more. */
public record DonorListItem(UUID id, String firstName, String lastName, String phone, DonorType type,
                            boolean wantsTaxReceipt) {
}
