package com.orudesk.orudesk.donors.application.port.in;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** @param nextCursor pass it as "after" for the next page; null on the last page */
public record DonorPage(List<DonorListItem> items, @Nullable String nextCursor) {
}
