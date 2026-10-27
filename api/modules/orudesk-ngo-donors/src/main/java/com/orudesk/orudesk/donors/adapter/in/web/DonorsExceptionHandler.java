package com.orudesk.orudesk.donors.adapter.in.web;

import com.orudesk.orudesk.donors.domain.DonorNotFoundException;
import com.orudesk.orudesk.donors.domain.PanAlreadyRegisteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class DonorsExceptionHandler {

    @ExceptionHandler(DonorNotFoundException.class)
    ProblemDetail donorNotFound(DonorNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Donor not found");
        return problem;
    }

    @ExceptionHandler(PanAlreadyRegisteredException.class)
    ProblemDetail panTaken(PanAlreadyRegisteredException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("PAN already registered");
        return problem;
    }
}
