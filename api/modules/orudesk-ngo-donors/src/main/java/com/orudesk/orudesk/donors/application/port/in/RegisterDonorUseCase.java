package com.orudesk.orudesk.donors.application.port.in;

import java.util.UUID;

public interface RegisterDonorUseCase {

    /** Adds the person to the registry and makes them a donor: both, or neither. */
    UUID register(RegisterDonorCommand command);
}
