package com.orudesk.orudesk.donors.application.port.in;

/**
 * Changes a donor's details: the person's in the registry and the NGO's own, in one transaction. Throws the
 * core's EditConflictException if either was changed since it was read.
 */
public interface EditDonorUseCase {

    DonorDetails edit(EditDonorCommand command);
}
