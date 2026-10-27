package com.orudesk.orudesk.donors.application.port.in;

public interface SearchDonorsQuery {

    int MAX_PAGE_SIZE = 100;

    /** One page of the tenant's donors, sorted by last name, first name. */
    DonorPage search(DonorSearch search);
}
