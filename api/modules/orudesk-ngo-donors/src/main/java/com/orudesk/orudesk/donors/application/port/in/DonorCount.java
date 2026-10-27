package com.orudesk.orudesk.donors.application.port.in;

import com.orudesk.orudesk.donors.domain.DonorType;

/** How many donors of one type, and how many of them want a tax receipt. */
public record DonorCount(DonorType type, long donors, long wantingTaxReceipts) {
}
