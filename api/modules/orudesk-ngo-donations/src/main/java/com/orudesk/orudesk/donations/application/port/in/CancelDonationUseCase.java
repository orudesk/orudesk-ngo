package com.orudesk.orudesk.donations.application.port.in;

/**
 * Cancels a donation that didn't happen as recorded (Day 58): a cheque that bounced, a duplicate entry. The donation
 * and its receipt number stay, marked cancelled; totals and reports leave it out.
 */
public interface CancelDonationUseCase {

    /**
     * Throws DonationNotFoundException, DonationAlreadyCancelledException, or EditConflictException if the
     * donation changed since it was read.
     */
    DonationDetails cancel(CancelDonationCommand command);
}
