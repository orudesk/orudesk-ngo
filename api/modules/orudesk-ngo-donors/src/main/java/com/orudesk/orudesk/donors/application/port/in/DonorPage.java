package com.orudesk.orudesk.donors.application.port.in;

import java.util.List;

/** @param nextCursor pass it as "after" for the next page; null on the last page */
public record DonorPage(List<DonorListItem> items, String nextCursor) {
}
