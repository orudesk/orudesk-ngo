package com.orudesk.orudesk.donors.application.port.in;

import java.util.Optional;
import java.util.UUID;

/** Who a donor is, for a donation and its receipt (Day 56). Empty if there's no such donor in this tenant. */
public interface DonorReceiptQuery {

    Optional<DonorForReceipt> findForReceipt(UUID donorId);
}
