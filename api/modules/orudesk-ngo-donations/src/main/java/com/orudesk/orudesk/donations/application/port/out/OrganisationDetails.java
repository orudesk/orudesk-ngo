package com.orudesk.orudesk.donations.application.port.out;

import org.jspecify.annotations.Nullable;

/**
 * Who issues the receipts (Day 57): the NGO's own name, address and tax registrations, printed on every receipt.
 *
 * @param registration80G the 80G approval number, such as AAATN1234FF20214; null if the NGO has none
 */
public record OrganisationDetails(String name, @Nullable String address, @Nullable String pan,
                                  @Nullable String registration80G) {
}
