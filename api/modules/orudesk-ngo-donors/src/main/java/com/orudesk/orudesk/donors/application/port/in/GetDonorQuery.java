package com.orudesk.orudesk.donors.application.port.in;

import java.util.UUID;

public interface GetDonorQuery {

    DonorDetails getDonor(UUID donorId);
}
