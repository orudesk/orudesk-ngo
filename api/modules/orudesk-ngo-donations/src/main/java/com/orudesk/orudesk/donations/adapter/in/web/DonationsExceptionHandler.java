package com.orudesk.orudesk.donations.adapter.in.web;

import com.orudesk.orudesk.donations.domain.DonationNotFoundException;
import com.orudesk.orudesk.donations.domain.UnknownDonorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class DonationsExceptionHandler {

    @ExceptionHandler(DonationNotFoundException.class)
    ProblemDetail donationNotFound(DonationNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Donation not found");
        return problem;
    }

    /** 404 for a URL that names the donor, and for a body that names one: either way, there's no such donor here. */
    @ExceptionHandler(UnknownDonorException.class)
    ProblemDetail unknownDonor(UnknownDonorException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Donor not found");
        return problem;
    }
}
