package com.orudesk.orudesk.donors.application.port.out;

import com.orudesk.orudesk.donors.application.port.in.DonorCount;
import com.orudesk.orudesk.donors.application.port.in.DonorDetails;
import com.orudesk.orudesk.donors.application.port.in.DonorListItem;
import com.orudesk.orudesk.donors.domain.DonorId;
import com.orudesk.orudesk.donors.domain.DonorType;

import java.util.List;
import java.util.Optional;

/**
 * The read side: donors together with their registry details, for showing and reporting. Returns what
 * the API shows (port.in records), not domain objects: nothing is ever changed through this port.
 */
public interface ReadDonorsPort {

    Optional<DonorDetails> findDetails(DonorId id);

    /** @param text lower case, or null; @param after null for the first page */
    List<DonorListItem> search(String text, DonorType type, DonorPosition after, int limit);

    List<DonorCount> countByType();
}
