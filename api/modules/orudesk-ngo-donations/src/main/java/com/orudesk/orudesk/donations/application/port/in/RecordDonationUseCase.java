package com.orudesk.orudesk.donations.application.port.in;

import java.util.UUID;

public interface RecordDonationUseCase {

    /** Records the donation and returns its id. Throws UnknownDonorException, or IllegalArgumentException for a rule. */
    UUID record(RecordDonationCommand command);
}
