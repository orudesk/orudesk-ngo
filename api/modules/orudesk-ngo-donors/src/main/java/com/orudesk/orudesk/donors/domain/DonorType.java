package com.orudesk.orudesk.donors.domain;

/**
 * Who gives. For CORPORATE and FOUNDATION, the person is the contact at that organisation (organisations
 * as parties of their own come after release 1).
 */
public enum DonorType {
    INDIVIDUAL,
    CORPORATE,
    FOUNDATION
}
